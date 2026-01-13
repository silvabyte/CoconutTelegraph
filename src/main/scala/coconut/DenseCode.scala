package coconut

import scala.quoted.*

/**
 * Dense Code Compiler
 *
 * Compiles ultra-compact string codes into full robot programs.
 *
 * Encoding:
 *   First char = instruction type
 *   Remaining = parameters (hex/base64-ish encoding)
 *
 * Instruction Types:
 *   > < ^ v   Move forward/back/up/down
 *   @ N       Turn N degrees (hex-encoded)
 *   ? I       Read sensor I
 *   ! I V     Set actuator I to V
 *   [ ... ]   Loop
 *   { ... }   Conditional
 *   |         Parallel separator
 *   .         Wait 100ms
 *   ;         Sequence separator
 *   #         Halt
 *   " text "  Log message
 */

object DenseCode:

  /** Runtime interpreter for dense code */
  def parse(code: String): Program =
    val instructions = parseInstructions(code.trim)
    Program("dense", instructions)

  private def parseInstructions(code: String): Seq[Instr] =
    if code.isEmpty then return Seq.empty

    val result = scala.collection.mutable.ArrayBuffer[Instr]()
    var i = 0

    while i < code.length do
      val c = code(i)
      c match
        case '>' =>
          val dist = parseNum(code, i + 1)
          result += Instr.Move(Dir.Forward, dist._1.max(1))
          i = dist._2

        case '<' =>
          val dist = parseNum(code, i + 1)
          result += Instr.Move(Dir.Backward, dist._1.max(1))
          i = dist._2

        case '^' =>
          val dist = parseNum(code, i + 1)
          result += Instr.Move(Dir.Forward, dist._1.max(1))
          i = dist._2

        case 'v' =>
          val dist = parseNum(code, i + 1)
          result += Instr.Move(Dir.Backward, dist._1.max(1))
          i = dist._2

        case '/' =>
          val dist = parseNum(code, i + 1)
          result += Instr.Move(Dir.Left, dist._1.max(1))
          i = dist._2

        case '\\' =>
          val dist = parseNum(code, i + 1)
          result += Instr.Move(Dir.Right, dist._1.max(1))
          i = dist._2

        case '@' =>
          val deg = parseNum(code, i + 1)
          result += Instr.Turn(deg._1)
          i = deg._2

        case '?' =>
          val sensorId = parseNum(code, i + 1)
          val varName = s"s${sensorId._1}"
          result += Instr.Sense(Proximity(sensorId._1.toByte), varName)
          i = sensorId._2

        case '!' =>
          val (actuatorId, pos1) = parseNum(code, i + 1)
          val (value, pos2) = parseNum(code, pos1)
          result += Instr.Actuate(Motor(actuatorId.toByte), value.toDouble)
          i = pos2

        case '[' =>
          val (inner, endPos) = extractBracketed(code, i, '[', ']')
          val times = parseNum(inner, 0)
          val bodyCode = inner.drop(times._2)
          result += Instr.Loop(times._1.max(1), parseInstructions(bodyCode))
          i = endPos + 1

        case '{' =>
          val (inner, endPos) = extractBracketed(code, i, '{', '}')
          // Simple conditional: {threshold:then:else}
          val parts = inner.split(':')
          if parts.length >= 2 then
            val threshold = parts(0).toIntOption.getOrElse(50)
            val thenCode = if parts.length > 1 then parts(1) else ""
            val elseCode = if parts.length > 2 then parts(2) else ""
            result += Instr.Cond(
              robot => robot.memory.get("s0").exists(_ > threshold),
              parseInstructions(thenCode),
              parseInstructions(elseCode)
            )
          i = endPos + 1

        case '|' =>
          // Parallel marker - handled by parent
          i += 1

        case '.' =>
          result += Instr.Wait(100)
          i += 1

        case ',' =>
          result += Instr.Wait(50)
          i += 1

        case ';' =>
          // Sequence separator, just advance
          i += 1

        case '#' =>
          result += Instr.Halt
          i += 1

        case '"' =>
          val endQuote = code.indexOf('"', i + 1)
          if endQuote > i then
            result += Instr.Log(code.substring(i + 1, endQuote))
            i = endQuote + 1
          else
            i += 1

        case c if c.isWhitespace =>
          i += 1

        case _ =>
          i += 1

    result.toSeq

  private def parseNum(code: String, start: Int): (Int, Int) =
    if start >= code.length then return (0, start)

    var i = start
    val sb = StringBuilder()

    // Handle negative
    if i < code.length && code(i) == '-' then
      sb += '-'
      i += 1

    // Collect digits
    while i < code.length && (code(i).isDigit || code(i) == 'x' || ('a' to 'f').contains(code(i).toLower)) do
      sb += code(i)
      i += 1

    val numStr = sb.toString
    val value =
      if numStr.startsWith("0x") || numStr.startsWith("-0x") then
        Integer.parseInt(numStr.replace("0x", ""), 16)
      else
        numStr.toIntOption.getOrElse(0)

    (value, i)

  private def extractBracketed(code: String, start: Int, open: Char, close: Char): (String, Int) =
    var depth = 0
    var i = start
    val sb = StringBuilder()

    while i < code.length do
      val c = code(i)
      if c == open then
        if depth > 0 then sb += c
        depth += 1
      else if c == close then
        depth -= 1
        if depth == 0 then
          return (sb.toString, i)
        else
          sb += c
      else if depth > 0 then
        sb += c
      i += 1

    (sb.toString, i)


  // ============================================================================
  // COMPILE-TIME MACRO - Validates dense code at compile time
  // ============================================================================

  /** Compile-time validated dense code */
  inline def dense(inline code: String): Program =
    ${ denseImpl('code) }

  private def denseImpl(code: Expr[String])(using Quotes): Expr[Program] =
    import quotes.reflect.*

    code.value match
      case Some(codeStr) =>
        // Validate at compile time
        try
          val _ = parse(codeStr)
          '{ DenseCode.parse($code) }
        catch
          case e: Exception =>
            report.errorAndAbort(s"Invalid dense code: ${e.getMessage}")

      case None =>
        '{ DenseCode.parse($code) }


/**
 * Extended dense encoding with more symbols
 */
object UltraDenseCode:

  /**
   * Ultra-dense encoding using base-64-like compression
   *
   * Format: &[type][id]:[params];
   *
   * Types:
   *   0-9   = Motor power (0=stop, 9=full)
   *   A-Z   = Sensor read (A=prox0, B=prox1, etc)
   *   a-z   = Actuator (a=motor0, b=motor1, etc)
   *   $     = Turn (followed by hex degrees)
   *   @     = Conditional
   *   *     = Loop
   *   .     = Wait
   *   #     = Halt
   */

  def ultra(code: String): Program =
    val expanded = expandUltra(code)
    DenseCode.parse(expanded)

  private def expandUltra(code: String): String =
    val sb = StringBuilder()
    var i = 0

    while i < code.length do
      code(i) match
        case '&' =>
          // Motor control: &[motor_id][power]
          // &05 = motor 0 at 50%, &19 = motor 1 at 90%
          if i + 2 < code.length then
            val motorId = code(i + 1).asDigit
            val power = code(i + 2).asDigit * 10
            sb ++= s"!$motorId$power"
            i += 3
          else i += 1

        case '$' =>
          // Turn: $[hex_degrees]
          if i + 2 < code.length then
            val deg = Integer.parseInt(code.substring(i + 1, i + 3), 16)
            sb ++= s"@$deg"
            i += 3
          else i += 1

        case '~' =>
          // Sensor read: ~[sensor_id]
          if i + 1 < code.length then
            val sensorId = code(i + 1).asDigit
            sb ++= s"?$sensorId"
            i += 2
          else i += 1

        case ':' =>
          // Sequence
          sb += ';'
          i += 1

        case '.' =>
          sb += '.'
          i += 1

        case ',' =>
          sb += ','
          i += 1

        case ';' =>
          sb += '#'
          i += 1

        case '*' =>
          // Loop: *[times][body until *]
          if i + 1 < code.length then
            val times = code(i + 1).asDigit.max(1)
            val endLoop = code.indexOf('*', i + 2)
            if endLoop > i then
              val body = code.substring(i + 2, endLoop)
              sb ++= s"[${times}${expandUltra(body)}]"
              i = endLoop + 1
            else i += 1
          else i += 1

        case c if c.isDigit =>
          // Solo digit = forward movement
          sb ++= s">${c.asDigit * 10}"
          i += 1

        case _ =>
          i += 1

    sb.toString


/** String interpolator for dense code */
extension (sc: StringContext)
  def r(args: Any*): Program =
    val code = sc.parts.mkString
    DenseCode.parse(code)
