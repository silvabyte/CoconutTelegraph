package coconut

import scala.annotation.targetName

/**
 * The Dense Symbolic Language
 *
 * Each symbol encodes a robot operation. Combined, they form
 * programs that would take hundreds of lines in Java.
 *
 * Symbol Reference (using ASCII-safe operators):
 *   >   <       Movement forward/backward
 *   |>  <|      Turn right/left
 *   ?           Sensor read
 *   !           Actuator write
 *   ++  --      Increment/decrement
 *   ~           Execute behavior
 *   ??          Conditional
 *   ||          Parallel execution
 *   ***         Loop/repeat
 *   =>  <=      State transitions
 *   &   |   ^   Logic operations
 *   ~=          Approximate comparison
 *   @           Until condition
 */

// ============================================================================
// INSTRUCTION SET - The dense encoding
// ============================================================================

/** A robot instruction - the atomic unit of robot programs */
sealed trait Instr:
  def execute(robot: Robot): Unit
  def pretty: String

object Instr:
  /** Movement instructions */
  case class Move(direction: Dir, distance: Int) extends Instr:
    def execute(robot: Robot): Unit =
      robot.log(s"Moving $direction for $distance units")
      direction match
        case Dir.Forward  => robot.motors.foreach(_.set(50))
        case Dir.Backward => robot.motors.foreach(_.set(-50))
        case Dir.Left     => robot.motors(0).set(-30); robot.motors(1).set(30)
        case Dir.Right    => robot.motors(0).set(30); robot.motors(1).set(-30)
    def pretty = s"MOVE($direction, $distance)"

  case class Turn(degrees: Int) extends Instr:
    def execute(robot: Robot): Unit =
      robot.log(s"Turning $degrees degrees")
    def pretty = s"TURN($degrees deg)"

  case class Sense[S <: Sensor](sensor: S, into: String) extends Instr:
    def execute(robot: Robot): Unit =
      val value = sensor.read
      robot.memory(into) = value
      robot.log(s"Read ${sensor.getClass.getSimpleName}#${sensor.id} = $value")
    def pretty = s"SENSE(${sensor.getClass.getSimpleName}#${sensor.id} -> $into)"

  case class Actuate(actuator: Actuator, value: Double) extends Instr:
    def execute(robot: Robot): Unit =
      actuator.set(value)
      robot.log(s"Set ${actuator.getClass.getSimpleName}#${actuator.id} = $value")
    def pretty = s"ACTUATE(${actuator.getClass.getSimpleName}#${actuator.id} <- $value)"

  case class Cond(test: Robot => Boolean, then_ : Seq[Instr], else_ : Seq[Instr]) extends Instr:
    def execute(robot: Robot): Unit =
      if test(robot) then then_.foreach(_.execute(robot))
      else else_.foreach(_.execute(robot))
    def pretty = s"IF(?) THEN [${then_.map(_.pretty).mkString("; ")}] ELSE [${else_.map(_.pretty).mkString("; ")}]"

  case class Loop(times: Int, body: Seq[Instr]) extends Instr:
    def execute(robot: Robot): Unit =
      robot.log(s"Looping $times times")
      (1 to times).foreach: i =>
        robot.log(s"  Iteration $i")
        body.foreach(_.execute(robot))
    def pretty = s"LOOP($times) [${body.map(_.pretty).mkString("; ")}]"

  case class While(cond: Robot => Boolean, body: Seq[Instr]) extends Instr:
    def execute(robot: Robot): Unit =
      var iterations = 0
      val maxIter = 1000 // Safety limit
      while cond(robot) && iterations < maxIter do
        body.foreach(_.execute(robot))
        iterations += 1
    def pretty = s"WHILE(?) [${body.map(_.pretty).mkString("; ")}]"

  case class Parallel(branches: Seq[Seq[Instr]]) extends Instr:
    def execute(robot: Robot): Unit =
      robot.log(s"Parallel execution of ${branches.size} branches")
      // In real robot, these would be concurrent
      branches.foreach(branch => branch.foreach(_.execute(robot)))
    def pretty = s"PARALLEL(${branches.map(b => s"[${b.map(_.pretty).mkString("; ")}]").mkString(" || ")})"

  case class Wait(ms: Int) extends Instr:
    def execute(robot: Robot): Unit =
      robot.log(s"Waiting ${ms}ms")
      Thread.sleep(ms.min(100)) // Capped for demo
    def pretty = s"WAIT(${ms}ms)"

  case class Log(msg: String) extends Instr:
    def execute(robot: Robot): Unit = robot.log(msg)
    def pretty = s"LOG($msg)"

  case object Halt extends Instr:
    def execute(robot: Robot): Unit =
      robot.motors.foreach(_.set(0))
      robot.log("HALT")
    def pretty = "HALT"

/** Movement directions */
enum Dir:
  case Forward, Backward, Left, Right

// ============================================================================
// SYMBOLIC OPERATORS - Making the DSL feel native
// ============================================================================

/** Symbol namespace - import this to use dense syntax */
object S:
  // Direction symbols (ASCII-safe)
  val fwd = Dir.Forward
  val bwd = Dir.Backward
  val lft = Dir.Left
  val rgt = Dir.Right

  /** Sensor factory: S.sen(0).prox */
  def sen(id: Int): SensorBuilder = SensorBuilder(id.toByte)

  /** Actuator factory: S.act(0).motor */
  def act(id: Int): ActuatorBuilder = ActuatorBuilder(id.toByte)

  /** Loop construct: S.rep(4)(instructions...) */
  def rep(times: Int)(body: Instr*): Instr.Loop = Instr.Loop(times, body)

  /** Parallel construct */
  def par(branches: Seq[Instr]*): Instr.Parallel = Instr.Parallel(branches)

  /** Conditional: S.cnd(test)(then...)(else...) */
  def cnd(test: Robot => Boolean)(then_ : Instr*)(else_ : Instr*): Instr.Cond =
    Instr.Cond(test, then_, else_)

  /** While loop */
  def whl(cond: Robot => Boolean)(body: Instr*): Instr.While =
    Instr.While(cond, body)

  /** Wait */
  def w(ms: Int): Instr.Wait = Instr.Wait(ms)

  /** Halt */
  val stp = Instr.Halt

  /** Log */
  def log(msg: String): Instr.Log = Instr.Log(msg)

/** Builder for sensors with dense syntax */
case class SensorBuilder(id: Byte):
  def prox: Proximity = Proximity(id)
  def temp: Temperature = Temperature(id)
  def light: Light = Light(id)
  def enc: Encoder = Encoder(id)
  def gyro: Gyro = Gyro(id)

  // Ultra-dense: type encoded in single char
  def p: Proximity = prox
  def t: Temperature = temp
  def l: Light = light
  def e: Encoder = enc
  def g: Gyro = gyro

/** Builder for actuators with dense syntax */
case class ActuatorBuilder(id: Byte):
  def motor: Motor = Motor(id)
  def servo: Servo = Servo(id)
  def led: LED = LED(id)

  // Ultra-dense
  def m: Motor = motor
  def s: Servo = servo
  def L: LED = led

// ============================================================================
// INSTRUCTION BUILDER DSL
// ============================================================================

extension (d: Dir)
  /** Move in direction for distance: fwd * 100 */
  @targetName("moveBy")
  def *(distance: Int): Instr.Move = Instr.Move(d, distance)

extension (sensor: Sensor)
  /** Read sensor into variable: sensor >> "name" */
  @targetName("readInto")
  def >>(varName: String): Instr.Sense[sensor.type] = Instr.Sense(sensor, varName)

extension (actuator: Actuator)
  /** Set actuator value: actuator << 50 */
  @targetName("setTo")
  def <<(value: Double): Instr.Actuate = Instr.Actuate(actuator, value)

extension (deg: Int)
  /** Turn by degrees: 90.deg */
  def deg: Instr.Turn = Instr.Turn(deg)

// ============================================================================
// PROGRAM COMPOSITION
// ============================================================================

/** A complete robot program */
case class Program(name: String, instructions: Seq[Instr]):
  def run(robot: Robot): Unit =
    robot.log(s"=== Running program: $name ===")
    instructions.foreach(_.execute(robot))
    robot.log(s"=== Program complete: $name ===")

  def pretty: String =
    s"""Program: $name
       |${instructions.map(i => s"  ${i.pretty}").mkString("\n")}""".stripMargin

  /** Sequence programs: p1 >> p2 */
  @targetName("andThen")
  def >>(other: Program): Program =
    Program(s"$name >> ${other.name}", instructions ++ other.instructions)

  /** Parallel execution: p1 || p2 */
  @targetName("parallel")
  def ||(other: Program): Program =
    Program(s"$name || ${other.name}", Seq(Instr.Parallel(Seq(instructions, other.instructions))))

/** Program builder DSL */
object Program:
  @targetName("fromVarargs")
  def apply(name: String)(instrs: Instr*): Program = new Program(name, instrs.toSeq)

/** Implicit program builder from instructions */
given Conversion[Seq[Instr], Program] = instrs => Program("anonymous", instrs)
