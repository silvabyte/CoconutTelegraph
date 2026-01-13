package coconut

import scala.collection.mutable

/**
 * The Robot runtime - executes dense programs
 */
class Robot(val name: String):
  // Hardware
  val motors: Array[Motor] = Array(Motor(0), Motor(1))
  val servos: Array[Servo] = Array(Servo(0), Servo(1))
  val leds: Array[LED] = Array(LED(0), LED(1), LED(2))

  val proxSensors: Array[Proximity] = Array(Proximity(0), Proximity(1))
  val tempSensor: Temperature = Temperature(0)
  val lightSensor: Light = Light(0)
  val encoders: Array[Encoder] = Array(Encoder(0), Encoder(1))
  val gyro: Gyro = Gyro(0)

  // Memory for sensor readings and variables
  val memory: mutable.Map[String, Double] = mutable.Map()

  // State
  private var _state: RobotState = RobotState.Idle
  def state: RobotState = _state

  // Execution log
  private val _log: mutable.ArrayBuffer[String] = mutable.ArrayBuffer()
  def executionLog: Seq[String] = _log.toSeq

  def log(msg: String): Unit =
    _log += s"[$name] $msg"
    println(_log.last)

  // State transitions with compile-time checking
  def transition[From <: RobotState, To <: RobotState](
    from: From, to: To
  )(using CanTransition[From, To]): Unit =
    _state = to
    log(s"State: $from -> $to")

  // Direct state set (less safe, for runtime flexibility)
  def setState(s: RobotState): Unit =
    log(s"State: $_state -> $s")
    _state = s

  /** Execute a program */
  def run(program: Program): Unit =
    program.run(this)

  /** Execute instructions directly */
  def exec(instrs: Instr*): Unit =
    instrs.foreach(_.execute(this))

  /** Reset robot state */
  def reset(): Unit =
    motors.foreach(_.set(0))
    servos.foreach(_.set(0))
    leds.foreach(_.set(0))
    memory.clear()
    _state = RobotState.Idle
    _log.clear()
    log("Robot reset")

object Robot:
  def apply(name: String): Robot = new Robot(name)
