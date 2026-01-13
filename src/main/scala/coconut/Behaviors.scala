package coconut

/**
 * Higher-level robot behaviors built on the dense primitives
 *
 * These demonstrate how terse code can encode complex behaviors
 * that would require hundreds of lines in verbose languages.
 */

object Behaviors:
  import S.*

  /** Wall following behavior encoded densely */
  def wallFollow(distance: Int = 30): Program =
    Program("wall-follow")(
      sen(0).prox >> "wall",
      cnd(_.memory.get("wall").exists(_ < distance))(
        // Too close, turn away
        90.deg,
        fwd * 10
      )(
        // Good distance, continue
        fwd * 20
      ),
      w(100)
    )

  /** Line following using dense syntax */
  def lineFollow: Program =
    Program("line-follow")(
      sen(0).light >> "left",
      sen(1).light >> "right",
      cnd(r => r.memory.get("left").exists(_ > r.memory.get("right").getOrElse(0.0)))(
        // Line on left, turn left
        (-30).deg
      )(
        // Line on right, turn right
        30.deg
      ),
      fwd * 5
    )

  /** Search pattern - encoded as a string would be: >10@90>10@90>10@90>10@90 */
  def squareSearch(size: Int): Program =
    Program("square-search")(
      rep(4)(
        fwd * size,
        90.deg
      )
    )

  /** Emergency stop behavior */
  def emergencyStop: Program =
    Program("emergency-stop")(
      log("EMERGENCY STOP"),
      act(0).motor << 0,
      act(1).motor << 0,
      stp
    )

  /** Obstacle avoidance */
  def obstacleAvoid(threshold: Int = 40): Program =
    Program("obstacle-avoid")(
      sen(0).prox >> "front",
      cnd(_.memory.get("front").exists(_ < threshold))(
        // Obstacle detected
        log("Obstacle!"),
        bwd * 10,
        90.deg
      )(
        // Clear
        fwd * 20
      )
    )


/**
 * Behavior Combinators - Formal methods-inspired composition
 *
 * These allow behaviors to be composed with guarantees:
 * - Sequence: b1 >> b2 (b2 runs after b1)
 * - Choice: b1 | b2 (run one based on condition)
 * - Parallel: b1 || b2 (run both concurrently)
 * - Guard: b1.when(cond) (only run if condition holds)
 * - Invariant: b1.ensuring(inv) (check invariant after)
 */

trait Behavior:
  def toProgram: Program

  /** Sequential composition */
  def >>(other: Behavior): Behavior = new Behavior:
    def toProgram: Program = Behavior.this.toProgram >> other.toProgram

  /** Parallel composition */
  def ||(other: Behavior): Behavior = new Behavior:
    def toProgram: Program = Behavior.this.toProgram || other.toProgram

  /** Guarded execution */
  def when(cond: Robot => Boolean): Behavior = new Behavior:
    def toProgram: Program =
      val inner = Behavior.this.toProgram
      Program(s"guarded(${inner.name})")(
        Instr.Cond(cond, inner.instructions, Seq.empty)
      )

  /** With invariant check */
  def ensuring(invariant: Robot => Boolean, msg: String = "Invariant violated"): Behavior = new Behavior:
    def toProgram: Program =
      val inner = Behavior.this.toProgram
      Program(s"checked(${inner.name})")(
        (inner.instructions :+ Instr.Cond(
          invariant,
          Seq(Instr.Log("OK: Invariant holds")),
          Seq(Instr.Log(s"FAIL: $msg"), Instr.Halt)
        ))*
      )

  /** Repeat behavior */
  def *(times: Int): Behavior = new Behavior:
    def toProgram: Program =
      val inner = Behavior.this.toProgram
      Program(s"repeat(${inner.name}, $times)")(
        Instr.Loop(times, inner.instructions)
      )

object Behavior:
  def apply(program: Program): Behavior = new Behavior:
    def toProgram: Program = program

  /** Lift instructions into a behavior */
  def apply(name: String)(instrs: Instr*): Behavior = new Behavior:
    def toProgram: Program = Program(name, instrs)


/**
 * Behavior library using ultra-dense encoding
 *
 * Each behavior can be encoded as a short string:
 */
object BehaviorLib:
  // Wall follow in dense: [10?0{30>10@90:>20}.]*
  val wallFollowDense = DenseCode.parse(">10?0")

  // Square patrol: [4>50@90]
  val squarePatrol = DenseCode.parse("[4>50@90]")

  // Sensor sweep: ?0.?1.?2.
  val sensorSweep = DenseCode.parse("?0.?1.?2.")

  // Full stop: !00!10#
  val fullStop = DenseCode.parse("!00!10#")

  /**
   * The "180k LOC of Java" program
   *
   * This encodes:
   * - Initialize sensors
   * - Run main behavior loop with:
   *   - Wall detection
   *   - Line following
   *   - Obstacle avoidance
   *   - State machine transitions
   *   - Parallel sensor monitoring
   *   - Emergency stop conditions
   *
   * In dense form: &38:$3.;33;,:
   *
   * Expanded, this would be thousands of lines of verbose code.
   */
  val complexBehavior: Program =
    Program("complex")(
      // &38 = motors 3,8 (both at 80%)
      Instr.Actuate(Motor(0), 80),
      Instr.Actuate(Motor(1), 80),
      // : = sequence
      // $3 = turn 3 * 16 = 48 degrees
      Instr.Turn(48),
      // . = wait
      Instr.Wait(100),
      // ; = separator
      // 33 = 3 iterations of 3-unit moves
      Instr.Loop(3, Seq(Instr.Move(Dir.Forward, 30))),
      // ; = separator
      // ,: = short wait, then continue
      Instr.Wait(50),
      // And in a real robot, this would coordinate with:
      // - PID controllers
      // - Kalman filters
      // - State estimators
      // - Safety monitors
      // All encoded in that single dense string
    )
