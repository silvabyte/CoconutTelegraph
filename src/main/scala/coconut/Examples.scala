package coconut

/**
 * Examples demonstrating the density spectrum
 *
 * From explicit imperative code to ultra-dense robot encoding
 */
object Examples:

  // ============================================================================
  // EXPLICIT STYLE - Imperative, step-by-step
  // ============================================================================

  def explicitExample(): Unit =
    val robot = Robot("Verbose")

    // Create sensors
    val proximitySensor0 = Proximity(0)
    val proximitySensor1 = Proximity(1)
    val lightSensor = Light(0)

    // Create actuators
    val leftMotor = Motor(0)
    val rightMotor = Motor(1)
    val headServo = Servo(0)

    // Read sensors
    val frontDistance = proximitySensor0.read
    robot.memory("front") = frontDistance

    val leftDistance = proximitySensor1.read
    robot.memory("left") = leftDistance

    // Decision logic
    if frontDistance < 30 then
      // Obstacle ahead - turn
      leftMotor.set(-50)
      rightMotor.set(50)
      Thread.sleep(500)
    else if leftDistance < 20 then
      // Too close to wall - veer right
      leftMotor.set(60)
      rightMotor.set(40)
    else
      // Clear path - go forward
      leftMotor.set(50)
      rightMotor.set(50)


  // ============================================================================
  // INTERMEDIATE STYLE (Scala DSL, ~10-20 lines)
  // ============================================================================

  def intermediateExample(): Program =
    import S.*

    Program("intermediate")(
      // Sense
      sen(0).prox >> "front",
      sen(1).prox >> "left",

      // Decide and act
      cnd(_.memory.get("front").exists(_ < 30))(
        // Obstacle
        (-90).deg,
        w(500)
      )(
        cnd(_.memory.get("left").exists(_ < 20))(
          // Wall too close
          act(0).motor << 60,
          act(1).motor << 40
        )(
          // Clear
          act(0).motor << 50,
          act(1).motor << 50
        )
      )
    )


  // ============================================================================
  // DENSE STYLE (String encoding, ~20-40 chars)
  // ============================================================================

  def denseExample(): Program =
    // Same behavior as above, encoded densely
    r"?0?1{30:@-90.:!060!140:!050!150}"


  // ============================================================================
  // ULTRA-DENSE STYLE (Compressed, <20 chars)
  // ============================================================================

  def ultraDenseExample(): Program =
    // &05 = motor 0 at 50%
    // &15 = motor 1 at 50%
    // ~0  = read sensor 0
    // $5A = turn 90deg (0x5A = 90)
    UltraDenseCode.ultra("&05&15~0")


  // ============================================================================
  // THE MYTHICAL "&38:$3.;33;,:" PROGRAM
  // ============================================================================

  /**
   * What does "&38:$3.;33;,:" represent?
   *
   * In a sufficiently advanced token-dense language, this could encode:
   *
   * & = Behavior composition operator
   * 3 = Third behavior pattern (patrol)
   * 8 = Power level 80%
   * : = Sequence separator
   * $ = Turn command
   * 3 = 3 * base_angle degrees
   * . = Synchronization point
   * ; = State transition
   * 33 = Loop 3 times, move 3 units
   * ; = State transition
   * , = Short delay
   * : = Continue
   *
   * This ~12 character string encodes:
   * - Robot initialization
   * - Sensor calibration
   * - Main control loop
   * - PID controller setup
   * - State machine with 5 states
   * - Safety monitors
   * - Emergency stop handlers
   * - Communication protocols
   */
  val theCode = "&38:$3.;33;,:"

  def decode(code: String): Unit =
    println(s"\nDecoding: $code")
    println("=" * 50)

    code.zipWithIndex.foreach { case (c, i) =>
      val meaning = c match
        case '&' => "COMPOSE: Start behavior composition"
        case '3' if i == 1 => "PATTERN: Patrol behavior #3"
        case '8' => "POWER: Set 80% power level"
        case ':' => "SEQ: Sequence separator"
        case '$' => "TURN: Rotation command"
        case '3' if i == 4 => "ANGLE: 3 * 16deg = 48deg"
        case '.' => "SYNC: Wait for sync point"
        case ';' => "STATE: Transition to next state"
        case '3' if i == 7 => "LOOP: 3 iterations"
        case '3' if i == 8 => "DIST: 3 units per iteration"
        case ',' => "WAIT: Short delay (50ms)"
        case _ => "???"
      println(f"  [$i] '$c' -> $meaning")
    }

    println("\nThis encodes:")
    println("  * Robot initialization sequence")
    println("  * PID controller configuration")
    println("  * 5-state behavior state machine")
    println("  * Sensor fusion pipeline")
    println("  * Safety monitor with emergency stop")
    println("  * Inter-robot communication sync")
    println()
    println(s"Dense encoding: ${code.length} characters")
    println("Equivalent Scala DSL: ~500 lines")


  // ============================================================================
  // FORMAL METHODS FLAVOR - Type-safe composition
  // ============================================================================

  def formalExample(): Unit =
    import S.*

    // Behaviors with compile-time guarantees
    val patrol = Behavior("patrol")(
      rep(4)(fwd * 50, 90.deg)
    )

    val detect = Behavior("detect")(
      sen(0).prox >> "d",
      cnd(_.memory.get("d").exists(_ < 30))(
        log("Object detected!")
      )()
    )

    // Compose with invariants
    val safe = (patrol >> detect)
      .ensuring(
        r => r.memory.get("d").forall(_ > 10),
        "Safety distance violated"
      )

    // Run
    val robot = Robot("Formal")
    robot.run(safe.toProgram)


  // ============================================================================
  // LIVE DEMO
  // ============================================================================

  def demo(): Unit =
    println("\n" + "=" * 60)
    println("  COCONUT TELEGRAPH - Token-Dense Robot Language")
    println("=" * 60)

    println("\n[1] Explicit Style (Imperative)")
    println("-" * 40)
    explicitExample()

    println("\n[2] Intermediate Style (Scala DSL)")
    println("-" * 40)
    val robot2 = Robot("DSL")
    robot2.run(intermediateExample())

    println("\n[3] Dense Style (String encoding)")
    println("-" * 40)
    val robot3 = Robot("Dense")
    val denseProgram = denseExample()
    println(s"Code: ${denseProgram.name}")
    println(denseProgram.pretty)
    robot3.run(denseProgram)

    println("\n[4] Ultra-Dense Style")
    println("-" * 40)
    val robot4 = Robot("Ultra")
    val ultraProgram = ultraDenseExample()
    robot4.run(ultraProgram)

    println("\n[5] The Mythical Code")
    println("-" * 40)
    decode(theCode)

    println("\n[6] Formal Methods Composition")
    println("-" * 40)
    formalExample()

    println("\n" + "=" * 60)
    println("  Demo complete!")
    println("=" * 60)


@main def runDemo(): Unit =
  Examples.demo()
