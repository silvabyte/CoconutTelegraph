# CoconutTelegraph

**A token-dense robot programming DSL demonstrating Scala 3's metaprogramming power**

> "What would a token-dense programming language made for robots that has formal method origins look like?"
>
> tired: Java verbosity
> wired: `&38:$3.;33;,:` = 180k LOC of Java

## The Concept

This project explores how Scala 3's extensibility allows us to create languages that feel native while encoding massive amounts of logic in minimal syntax.

### The Density Spectrum

```scala
// VERBOSE (Java-style) - ~50 lines
val proximitySensor0 = Proximity(0)
val frontDistance = proximitySensor0.read
robot.memory("front") = frontDistance
if frontDistance < 30 then
  leftMotor.set(-50)
  rightMotor.set(50)
// ... continues for 40+ more lines

// INTERMEDIATE (Scala DSL) - ~15 lines
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

// DENSE (String encoding) - ~30 chars
r"?0{30:@-90.:!050!150}"

// ULTRA-DENSE - ~10 chars
UltraDenseCode.ultra("&05&15~0")
```

## Features

### 1. Refinement Types (Formal Methods Flavor)

```scala
// Compile-time bounded integers
type Pct = Bounded[0, 100]     // Percentage: always 0-100
type Deg = Bounded[0, 359]     // Angle in degrees
type Pwr = Bounded[-100, 100]  // Motor power

// This compiles:
val validPower: Pwr = Bounded[0, 100](50)

// This fails at compile time:
val invalidPower: Pct = Bounded[0, 100](150)  // Error!
```

### 2. Type-Safe State Machines

```scala
// Valid transitions proven at compile time
given CanTransition[RobotState.Idle.type, RobotState.Moving.type]
given CanTransition[RobotState.Moving.type, RobotState.Turning.type]

// This compiles:
robot.transition(RobotState.Idle, RobotState.Moving)

// Invalid transitions won't compile
```

### 3. Dense Symbolic DSL

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

In a sufficiently advanced token-dense language, this 13-character string could encode:

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

**Equivalent code:**
- Java: ~180,000 lines (AbstractRobotControllerFactoryBuilderStrategyVisitor...)
- Scala DSL: ~500 lines
- Dense encoding: **13 characters**

## Running

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
  DenseCode.scala  # String-based dense code compiler
  Behaviors.scala  # Higher-level behavior library
  Examples.scala   # Demo showing the density spectrum
```

## Scala 3 Features Used

- **Opaque types** - Zero-cost type safety for bounded values and sensor readings
- **Extension methods** - Operator overloading (`*`, `>>`, `<<`, `.deg`)
- **Given instances** - Type-safe state transitions
- **Inline/macros** - Compile-time code validation
- **String interpolators** - `r"..."`for dense code
- **Enum types** - Robot states and directions
- **Union types** - Flexible instruction composition
- **Contextual abstractions** - Implicit conversions for DSL fluency

## Inspiration

This project demonstrates how language design choices impact expressivity. The same robot behavior can be expressed in:

1. **Verbose** - Every operation explicit, lots of boilerplate
2. **DSL** - Domain-specific abstractions, readable but compact
3. **Dense** - Maximum information per character, write-only but powerful

The "formal methods origins" manifests in:
- Refinement types (bounded integers)
- Type-safe state machines
- Compile-time validation
- Behavior invariants

Real robots need all three levels - dense for transmission/storage, DSL for development, verbose for debugging.
