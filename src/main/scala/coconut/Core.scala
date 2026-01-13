package coconut

import scala.compiletime.ops.int.*
import scala.annotation.targetName

/**
 * CoconutTelegraph - A token-dense robot programming DSL
 *
 * Demonstrates how Scala 3 can create languages that feel native
 * while encoding complex robot behaviors in minimal syntax.
 */

// ============================================================================
// REFINEMENT TYPES - Formal method flavor with compile-time guarantees
// ============================================================================

/** Bounded integers - values proven at compile time to be within range */
opaque type Bounded[Min <: Int, Max <: Int] = Int

object Bounded:
  inline def apply[Min <: Int, Max <: Int](inline value: Int): Bounded[Min, Max] =
    inline val min = compiletime.constValue[Min]
    inline val max = compiletime.constValue[Max]
    inline if value >= min && value <= max then value
    else compiletime.error("Value out of bounds")

  extension [Min <: Int, Max <: Int](b: Bounded[Min, Max])
    def value: Int = b
    def widen: Int = b

/** Percentage type - always 0-100 */
type Pct = Bounded[0, 100]

/** Angle in degrees - always 0-359 */
type Deg = Bounded[0, 359]

/** Motor power - always -100 to 100 */
type Pwr = Bounded[-100, 100]

// ============================================================================
// CORE ROBOT PRIMITIVES
// ============================================================================

/** Sensor reading - tagged phantom type for type-safe sensor data */
opaque type Reading[S <: Sensor] = Double

object Reading:
  def apply[S <: Sensor](value: Double): Reading[S] = value
  extension [S <: Sensor](r: Reading[S])
    def value: Double = r
    def >(threshold: Double): Boolean = r > threshold
    def <(threshold: Double): Boolean = r < threshold
    @targetName("geq") def >=(threshold: Double): Boolean = r >= threshold
    @targetName("leq") def <=(threshold: Double): Boolean = r <= threshold

/** Base sensor trait */
sealed trait Sensor:
  def id: Byte
  def read: Double

/** Base actuator trait */
sealed trait Actuator:
  def id: Byte
  def set(value: Double): Unit

// Concrete sensor types
case class Proximity(id: Byte) extends Sensor:
  def read: Double = scala.util.Random.nextDouble() * 100 // Simulated

case class Temperature(id: Byte) extends Sensor:
  def read: Double = 20.0 + scala.util.Random.nextDouble() * 10

case class Light(id: Byte) extends Sensor:
  def read: Double = scala.util.Random.nextDouble() * 1000

case class Encoder(id: Byte) extends Sensor:
  def read: Double = scala.util.Random.nextDouble() * 360

case class Gyro(id: Byte) extends Sensor:
  def read: Double = scala.util.Random.nextDouble() * 360

// Concrete actuator types
case class Motor(id: Byte, var power: Double = 0) extends Actuator:
  def set(value: Double): Unit = power = value.max(-100).min(100)

case class Servo(id: Byte, var angle: Double = 0) extends Actuator:
  def set(value: Double): Unit = angle = value % 360

case class LED(id: Byte, var brightness: Double = 0) extends Actuator:
  def set(value: Double): Unit = brightness = value.max(0).min(100)

// ============================================================================
// ROBOT STATE MACHINE
// ============================================================================

/** Type-safe state representation */
enum RobotState:
  case Idle, Moving, Turning, Sensing, Acting, Error

/** State transition proof - compile-time checked valid transitions */
sealed trait CanTransition[From <: RobotState, To <: RobotState]

object CanTransition:
  given idleToMoving: CanTransition[RobotState.Idle.type, RobotState.Moving.type] with {}
  given idleToSensing: CanTransition[RobotState.Idle.type, RobotState.Sensing.type] with {}
  given movingToIdle: CanTransition[RobotState.Moving.type, RobotState.Idle.type] with {}
  given movingToTurning: CanTransition[RobotState.Moving.type, RobotState.Turning.type] with {}
  given movingToError: CanTransition[RobotState.Moving.type, RobotState.Error.type] with {}
  given turningToMoving: CanTransition[RobotState.Turning.type, RobotState.Moving.type] with {}
  given turningToIdle: CanTransition[RobotState.Turning.type, RobotState.Idle.type] with {}
  given sensingToActing: CanTransition[RobotState.Sensing.type, RobotState.Acting.type] with {}
  given sensingToIdle: CanTransition[RobotState.Sensing.type, RobotState.Idle.type] with {}
  given actingToIdle: CanTransition[RobotState.Acting.type, RobotState.Idle.type] with {}
  given actingToSensing: CanTransition[RobotState.Acting.type, RobotState.Sensing.type] with {}
  given errorToIdle: CanTransition[RobotState.Error.type, RobotState.Idle.type] with {}
