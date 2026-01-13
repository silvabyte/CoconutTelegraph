# CoconutTelegraph

**A token-dense robot programming language built with Scala 3 metaprogramming**

Which is A PoC based on this tweet: <https://x.com/GeoffreyHuntley/status/2011064850634719461>

> what would a token dense
> programming language
> made for robots
 that has formal
 method origins

 looks like?

 tired: java verbosity

 wired: “&38:$3.;33;,:” = 180k LOC of java

## The Concept

This project explores how Scala 3's extensibility allows us to create languages that feel native while encoding massive amounts of logic in minimal syntax. The key insight: Scala 3's metaprogramming features (opaque types, extension methods, macros, given instances) enable us to build a complete robot DSL that compiles down to efficient code with compile-time safety guarantees.

### The Density Spectrum

```scala
// EXPLICIT - Every step written out (~50 lines)
val proximitySensor0 = Proximity(0)
val frontDistance = proximitySensor0.read
robot.memory("front") = frontDistance
if frontDistance < 30 then
  leftMotor.set(-50)
  rightMotor.set(50)
// ... continues

// DSL - Symbolic operators (~15 lines)
Program("behavior")(
  sen(0).prox >> "front",
  cnd(_.memory.get("front").exists(_ < 30))(
    (-90).deg,
    w(500)
  )(
    act(0).motor << 50,
    act(1).motor << 50
  )
)

// DENSE - String encoding (~30 chars)
r"?0{30:@-90.:!050!150}"

// ULTRA-DENSE (~10 chars)
UltraDenseCode.ultra("&05&15~0")
```

## Features

### 1. Refinement Types (Formal Methods)

Compile-time bounded integers using opaque types and inline macros:

```scala
// Type-safe bounded values
type Pct = Bounded[0, 100]     // Percentage: always 0-100
type Deg = Bounded[0, 359]     // Angle in degrees
type Pwr = Bounded[-100, 100]  // Motor power

// This compiles:
val validPower: Pwr = Bounded[0, 100](50)

// This fails at compile time:
val invalidPower: Pct = Bounded[0, 100](150)  // Error!
```

### 2. Type-Safe State Machines

State transitions proven at compile time via given instances:

```scala
// Valid transitions declared as evidence
given CanTransition[RobotState.Idle.type, RobotState.Moving.type]
given CanTransition[RobotState.Moving.type, RobotState.Turning.type]

// This compiles - valid transition
robot.transition(RobotState.Idle, RobotState.Moving)

// Invalid transitions won't compile - no evidence exists
```

### 3. Dense Symbolic DSL

Extension methods create natural operators:

```scala
import S.*

// Movement: direction * distance
fwd * 100    // Move forward 100 units
bwd * 50     // Move backward 50 units

// Sensors: sen(id).type >> varname
sen(0).prox >> "front"   // Read proximity sensor 0 into "front"
sen(1).light >> "left"   // Read light sensor 1 into "left"

// Actuators: act(id).type << value
act(0).motor << 80       // Set motor 0 to 80%
act(1).servo << 45       // Set servo 1 to 45 degrees

// Control flow
rep(4)(fwd * 50, 90.deg)                    // Loop 4 times
cnd(test)(thenBranch)(elseBranch)           // Conditional
whl(condition)(body)                         // While loop
par(branch1, branch2)                        // Parallel execution
```

### 4. String-Based Dense Encoding

Compile-time validated via quoted macros:

```scala
// Dense instruction set
r">50@90<30"     // Forward 50, turn 90, backward 30
r"?0?1?2"        // Read sensors 0, 1, 2
r"!050!180"      // Set motor 0 to 50, motor 1 to 80
r"[4>50@90]"     // Loop 4x: forward 50, turn 90
r"{30:@90:>20}"  // If sensor > 30 then turn 90 else forward 20
```

### 5. Behavior Composition with Invariants

```scala
val patrol = Behavior("patrol")(rep(4)(fwd * 50, 90.deg))
val detect = Behavior("detect")(sen(0).prox >> "d", ...)

// Compose with safety invariants
val safe = (patrol >> detect)
  .ensuring(
    r => r.memory.get("d").forall(_ > 10),
    "Safety distance violated"
  )
```

## The Mythical `&38:$3.;33;,:` Explained

In a sufficiently advanced token-dense language, this 13-character string encodes:

| Char | Meaning |
|------|---------|
| `&`  | Behavior composition operator |
| `3`  | Pattern #3 (patrol behavior) |
| `8`  | Power level 80% |
| `:`  | Sequence separator |
| `$`  | Turn command |
| `3`  | 3 * base_angle degrees |
| `.`  | Synchronization point |
| `;`  | State transition |
| `33` | Loop 3 times, move 3 units |
| `,`  | Short delay |

**What this encodes:**

- Robot initialization sequence
- PID controller configuration
- 5-state behavior state machine
- Sensor fusion pipeline
- Safety monitor with emergency stop
- Inter-robot communication sync

## Running

Install the Scala CLI (`scala`) (see <https://docs.scala-lang.org/getting-started/install-scala.html>):

Then compile and run with the `scala` command:

```bash
# Compile
scala compile .

# Run demo
scala run .
```

## Project Structure

```
src/main/scala/coconut/
  Core.scala       # Refinement types, sensors, actuators, state machine
  Symbols.scala    # DSL operators and instruction set
  Robot.scala      # Robot runtime
  DenseCode.scala  # String-based dense code compiler with macros
  Behaviors.scala  # Higher-level behavior library
  Examples.scala   # Demo showing the density spectrum
```

## Scala 3 Metaprogramming Features

This project showcases Scala 3's metaprogramming capabilities:

| Feature | Usage |
|---------|-------|
| **Opaque types** | Zero-cost type safety for `Bounded`, `Reading` |
| **Extension methods** | Operators `*`, `>>`, `<<`, `.deg` |
| **Given instances** | Type-safe state transitions as evidence |
| **Inline/macros** | Compile-time dense code validation |
| **String interpolators** | `r"..."` custom syntax |
| **Enum types** | `RobotState`, `Dir` |
| **Contextual abstractions** | Implicit conversions for DSL fluency |

## Why Token Density Matters for Robots

Real robots need different levels of expression:

1. **Dense** - For transmission, storage, and bandwidth-constrained communication
2. **DSL** - For development and readable behavior specification
3. **Explicit** - For debugging and understanding exactly what happens

Scala 3 lets us build all three levels as a unified system where each can be converted to the others, with compile-time guarantees at every level.

## Formal Methods Origins

The "formal methods flavor" manifests in:

- **Refinement types** - Bounded integers proven at compile time
- **Type-safe state machines** - Transitions verified via given instances
- **Compile-time validation** - Dense code checked by macros before runtime
- **Behavior invariants** - Safety conditions enforced compositionally
