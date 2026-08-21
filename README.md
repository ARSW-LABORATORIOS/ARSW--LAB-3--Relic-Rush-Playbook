# ARSW - Laboratory 3 - Relic Rush
## Thread Safety, Coordination and Deadlock Prevention - Java 21

**Course:** Arquitecturas de Software - ARSW  
**Period:** 2026-2  
**Technology:** Java 21 - Maven - JUnit 5  
**Suggested work mode:** teams of 2-3 students  
**Development time:** one week  

---

## 1. Purpose

In Laboratory 1 you measured the effect of concurrency. In Laboratory 2 you worked with race conditions and minimum critical regions. Laboratory 3 goes one step further:

> **How do we coordinate many threads, build thread-safe shared abstractions, detect deadlocks and prevent them without destroying concurrency?**

The focus is not simply to make the program "stop failing". You must explain the invariants, identify the coordination mechanism that each problem needs, and justify the trade-offs of the final design.

---

# 2. Game: Relic Rush

Relic Rush is a small concurrent game. Several adventurers compete to craft as many relics as possible.

Each adventurer is represented by a **platform thread**. During every round it must obtain **two exclusive forge stations** before it can craft a relic.

Examples of stations:

- Arcane Anvil
- Crystal Lens
- Rune Press
- Dragon Furnace
- Moon Altar
- Obsidian Table

When an adventurer gets both stations:

1. it crafts one relic;
2. its personal score increases;
3. a global event is registered in the `ForgeLedger`.

All adventurers play concurrently.

---

# 3. What is intentionally wrong in the starter?

The project contains concurrency defects on purpose.

You should expect to find at least these categories:

1. **Unsafe shared state** in the global ledger.
2. **A non-atomic read-modify-write operation**.
3. **A non-thread-safe collection** receiving concurrent writes.
4. **Nested lock acquisition in inconsistent order**, which can create deadlock.

The round coordination using `CyclicBarrier` is intentionally provided as a correct reference mechanism. Analyze it before modifying other parts.

---

# 4. Rules of the game and invariants

At the end of a completed round, the following values should agree:

```text
sum of all player scores
        ==
ForgeLedger.totalCrafted
        ==
number of ForgeEvent entries
```

Therefore, an important invariant is:

> **Every successfully crafted relic must be reflected exactly once in the player scores and exactly once in the global ledger.**

A second invariant is related to exclusive resources:

> **A forge station cannot be used simultaneously by two incompatible craft operations.**

Your solution must preserve these invariants without making the whole game sequential.

---

# 5. Requirements

- JDK 21
- Maven 3.9+
- Git

Verify:

```bash
java -version
mvn -version
```

---

# 6. Build

```bash
mvn clean test
```

If you only want to run the compiled classes:

```bash
mvn -q -DskipTests package
```

---

# 7. Run the game

Default configuration:

```bash
java -cp target/classes edu.eci.arsw.relicrush.app.RelicRushMain
```

Custom configuration:

```bash
java -cp target/classes edu.eci.arsw.relicrush.app.RelicRushMain <players> <stations> <rounds>
```

Example:

```bash
java -cp target/classes edu.eci.arsw.relicrush.app.RelicRushMain 16 6 50
```

A round snapshot looks like:

```text
ROUND 07 | scoreSum=56 | ledger=54 | events=55 | invariant=BROKEN
```

Do not assume that one correct run proves thread safety.

---

# 8. Part I - Understand the coordination model

Before changing code, study:

- `GameEngine`
- `Adventurer`
- `roundStart`
- `roundEnd`

The game uses two `CyclicBarrier` instances.

### Scenario A - Start gate

All workers wait until the coordinator releases the round.

### Scenario B - Round completion

The coordinator waits until every worker finishes its turn before reading the scoreboard.

Answer in `docs/REPORT_TEMPLATE.md`:

1. What problem does `roundStart` solve?
2. What problem does `roundEnd` solve?
3. Why would `Thread.sleep(...)` be an incorrect coordination strategy here?
4. What memory-consistency benefit do you obtain by reading the snapshot after the barrier?

---

# 9. Part II - Make the shared ledger thread-safe

Study `ForgeLedger`.

The starter currently contains:

- a shared integer updated with a read-modify-write sequence;
- an `ArrayList` modified concurrently.

Run the isolated ledger probe first:

```bash
java -cp target/classes edu.eci.arsw.relicrush.app.LedgerRaceProbe
java -cp target/classes edu.eci.arsw.relicrush.app.LedgerRaceProbe 64 5000
```

This lets you study the shared-state problem independently from the deadlock exercise.

Your task:

1. Reproduce an invariant violation.
2. Identify the exact shared state.
3. Select appropriate Java concurrency mechanisms.
4. Make the ledger thread-safe.
5. Explain why your solution is preferable to synchronizing the entire game.

Possible tools you may evaluate:

- `AtomicInteger`
- concurrent collections
- `synchronized`
- explicit locks

You are expected to choose, not blindly use all of them.

---

# 10. Part III - Reproduce and diagnose the deadlock

Run:

```bash
java -cp target/classes edu.eci.arsw.relicrush.app.DeadlockProbe
```

With the starter implementation, you should be able to obtain a result similar to:

```text
DEADLOCK DETECTED
- probe-A-anvil-then-furnace waiting on ... owned by probe-B-furnace-then-anvil
- probe-B-furnace-then-anvil waiting on ... owned by probe-A-anvil-then-furnace
```

If a deadlock is difficult to reproduce, execute the probe several times.

### Optional operating-system diagnosis

Find the Java process:

```bash
jps -l
```

Then inspect its threads:

```bash
jcmd <PID> Thread.print
```

or, when available:

```bash
jstack <PID>
```

Document evidence of the wait cycle.

---

# 11. Part IV - Explain the four deadlock conditions

Map the four Coffman conditions to the game:

1. **Mutual exclusion**
2. **Hold and wait**
3. **No preemption**
4. **Circular wait**

Your report must explain exactly where each one appears in the code.

---

# 12. Part V - Prevent the deadlock

Study:

```text
edu.eci.arsw.relicrush.concurrency.LockPair
```

The starter acquires two station monitors in the order requested by the player.

Your solution must prevent deadlocks **without using one global lock for every craft operation**.

Recommended direction:

> Define a deterministic global ordering for forge stations and always acquire locks in that order.

You may propose a different strategy if you can justify it technically.

After the fix:

```bash
java -cp target/classes edu.eci.arsw.relicrush.app.DeadlockProbe
```

Expected outcome:

```text
NO DEADLOCK DETECTED within 2 seconds.
```

---

# 13. Part VI - Stress verification

After fixing both thread safety and deadlocks, execute at least:

```bash
java -cp target/classes edu.eci.arsw.relicrush.app.InvariantProbe 8 6 50
java -cp target/classes edu.eci.arsw.relicrush.app.InvariantProbe 32 8 100
java -cp target/classes edu.eci.arsw.relicrush.app.InvariantProbe 128 8 100
```

For every completed round:

```text
invariant=OK
```

The game must finish normally.

---

# 14. Architectural analysis

### Correctness / reliability

**Protected invariants:**

Two invariants must hold at the end of every round:

```text
sum of player scores == ForgeLedger.totalCrafted == number of ForgeEvent entries
```

And:

```text
No forge station is held by two craft operations at the same time.
```

The first invariant is protected by `AtomicInteger.incrementAndGet()` in `ForgeLedger`, which makes the read-modify-write on `totalCrafted` a single atomic operation, and by `CopyOnWriteArrayList` for the `events` list, which guarantees safe concurrent appends without data loss.

The second invariant is protected by `LockPair.withBoth()`, which uses `synchronized` on each `ForgeStation` object as its own monitor. Only one thread can hold a station's monitor at a time — the JVM enforces this.

**Evidence:**

`DeadlockProbe` reports `NO DEADLOCK DETECTED` on every run after the fix. `InvariantProbe` run with three configurations confirms `invariant=OK` on every round:

```bash
java -cp target/classes edu.eci.arsw.relicrush.app.InvariantProbe 8 6 50
java -cp target/classes edu.eci.arsw.relicrush.app.InvariantProbe 32 8 100
java -cp target/classes edu.eci.arsw.relicrush.app.InvariantProbe 128 8 100
```

---

### Performance / throughput

**Where lock contention can appear:**

Contention appears when two or more adventurers try to acquire the same `ForgeStation` monitor at the same time. The probability of contention grows as the number of players increases relative to the number of stations — with 128 players and 6 stations, most threads will queue on the same few monitors.

There is also a brief contention point on `CopyOnWriteArrayList.add()` in `ForgeLedger.record()`, since each write creates a new internal array copy. This is acceptable because writes are infrequent (one per craft) and reads in `snapshot()` are completely lock-free.

**Why a global lock is undesirable:**

A single global lock on the entire craft operation would serialize every adventurer behind one monitor, regardless of which stations they need. Two adventurers using completely different stations — for example, one using `Arcane Anvil + Crystal Lens` and another using `Dragon Furnace + Moon Altar` — have no shared resource and could run in parallel. A global lock eliminates that parallelism entirely, reducing throughput to one craft at a time across the whole game.

**Which craft operations can still execute concurrently:**

Any two craft operations that use disjoint station pairs can execute in parallel. For example, adventurer A holding `station-1` and `station-3` does not block adventurer B from acquiring `station-2` and `station-4` at the same time. The fine-grained locking in `LockPair.withBoth()` preserves this concurrency — only operations that share at least one station are serialized against each other.

---

### Maintainability

**Lock ownership:**

Lock ownership is explicit and local. Each `ForgeStation` instance is its own monitor — the object that is locked is the same object that represents the resource being protected. There is no separate lock object to track or associate with a resource. Any developer reading `synchronized (lo)` immediately knows which station is being held.

**Lock ordering rule:**

The ordering rule is centralized in a single method: `LockPair.withBoth()`. The rule is stated in two lines:

```java
ForgeStation lo = first.id() < second.id() ? first : second;
ForgeStation hi = first.id() < second.id() ? second : first;
```

No caller needs to know about the ordering — it is enforced unconditionally inside `withBoth()`. Adding a new station type or a new adventurer strategy does not require remembering to apply the rule: any code that goes through `LockPair.withBoth()` is automatically safe. The only risk would be bypassing `withBoth()` and acquiring station monitors directly with `synchronized` elsewhere, which is easy to detect in a code review.

---

### Scalability

As the number of players grows while the number of stations stays constant, contention on station monitors increases. With `S` stations and `P` players per round, on average `P/S` players compete for each station. When `P >> S`, most threads spend most of their time blocked waiting for a monitor rather than doing useful work — throughput per player drops and the round takes longer to complete.

The ordering fix does not make this worse: it only changes the order in which threads queue on monitors, not the queue length. The bottleneck is the station count, not the locking strategy.

To scale further, the natural lever is increasing the number of stations. More stations means more disjoint pairs available, more operations that can run in parallel, and less average wait time per thread. The current design supports this directly — `GameConfig.stations()` controls station count and `LockPair.withBoth()` requires no changes regardless of how many stations exist.

---

# 15. Mini ADR

Create:

```text
docs/ADR-001-deadlock-prevention.md
```

Use this structure:

```markdown
# ADR-001: Deadlock prevention strategy

## Context
## Decision
## Alternatives considered
## Quality attributes affected
## Evidence
## Consequences
## Risks
```

---

# 16. Restrictions

- Java 21 only.
- Do not remove concurrency.
- Do not convert the game into sequential execution.
- Do not solve the whole game with one global lock.
- Do not delete the barriers to hide synchronization problems.
- Do not use arbitrary sleeps as a coordination mechanism.
- Do not replace the required reasoning with only screenshots.
- The final solution must pass repeated stress executions.

This lab deliberately uses **platform threads** instead of revisiting virtual threads. Virtual threads were already explored earlier in the course; here the learning objective is coordination, thread safety and deadlock behavior.

---

# 17. Deliverables

Your repository must include:

```text
README.md
pom.xml
src/
docs/REPORT.md
docs/ADR-001-deadlock-prevention.md
```

Use `docs/REPORT_TEMPLATE.md` as the starting point for `REPORT.md`.

---

# 18. Evaluation criteria

| Criterion | Weight |
|---|---:|
| Coordination analysis and correct use of barriers | 15% |
| Thread-safe shared state | 20% |
| Deadlock diagnosis and Coffman-condition analysis | 20% |
| Deadlock prevention without coarse global locking | 25% |
| Stress evidence and invariant verification | 10% |
| Architectural reasoning, ADR and code quality | 10% |

---

# 19. Learning sequence

```text
coordination
    -> thread-safe state
    -> invariant
    -> nested locks
    -> deadlock diagnosis
    -> prevention strategy
    -> stress evidence
    -> architectural trade-off
```

The objective is not merely to know `synchronized`.

> **The objective is to design a concurrent solution whose correctness and liveness can be explained and demonstrated.**

---

# 20. Bonus - Graphical interface

This is **not part of the graded lab** (it doesn't appear in section 18's rubric) - it's an extra desktop UI built on top of the same `GameEngine`, `ForgeLedger` and `LockPair` used everywhere else. If it's ever unstable, the console version (`RelicRushMain` and the probes) is what actually counts for the grade.

Build first if you haven't:

```bash
mvn -q -DskipTests package
```

Run it:

```bash
java -cp target/classes edu.eci.arsw.relicrush.app.RelicRushUIMain
```

A window opens with:

- **Adventurers**: the roster on the right, with each player's color and live relic count. Click one to see its current status in the detail line.
- **Forge stations and their state**: the map in the center - each station is a circle that changes color when an adventurer holds it, and shows a small "z" badge (in that adventurer's color) when someone else is blocked waiting on it.
- **Scores and relics crafted**: "total forjado" in the header, plus each adventurer's own count in the roster.
- **Simulation state and invariants**: the round counter, `corriendo`/`pausado`/`detenido` state, and `invariante: OK/ROTO (scoreSum = ledger = events)` in the header - same values `RelicRushMain` prints, just live.
- **Start / Pause / Resume / Stop**: Pause/Resume reuse the same `wait()`/`notifyAll()` pattern as the Warehouse Lab's `SimulationControl` - paused adventurers block without spinning, and Resume wakes them with `notifyAll()`. Stop interrupts every adventurer thread.

Notes:

- With the default config (8 players, 25 rounds) the game finishes in under a second, since there's no artificial delay anywhere in the real logic - you won't get to click Pause in time. Either raise "Rondas" a lot, or just watch: the UI adds a small 250ms pacing delay per station acquisition, but only inside its own listener code (`RelicRushUIMain`), never inside `Adventurer`, `GameEngine` or `LockPair`. Running the console/probes/tests is completely unaffected by this - it's UI-only pacing, not a coordination mechanism.
- `LockPair` exposes an optional `StationActivityListener` hook (`setListener`, `WAITING`/`ACQUIRED`/`RELEASED`) that the UI uses to know who holds or is waiting on each station. If no listener is registered - which is the case for `RelicRushMain`, every probe, and the tests - it's a no-op.
