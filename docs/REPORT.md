# ARSW Lab 3 - Relic Rush - Delivery Report

## Team

| Student | ID | GitHub |
|---|---|---|
| | | |
| | | |
| | | |

Repository: `URL`

Final commit: `SHA`

## 1. Baseline observations

- Command(s) executed:
- `java -cp target/classes edu.eci.arsw.relicrush.app.LedgerRaceProbe 64 5000`
- `java -cp target/classes edu.eci.arsw.relicrush.app.DeadlockProbe`

- What happened?
the LedgerRaceProbe showed different values between the expected
number of crafted relics, totalCrafted and eventCount. on the other
hand DeadlockProbe detected a deadlock between two threads 
that are trying to acquire the same forge stations in a different order

- Was the round invariant always preserved?
no, the invariant was broken bc of the values expected, also
totalCrafted and EventCount were different.

- Did the game stop unexpectedly?
the DeadlockProbe detected that two threads were blocked 
while waiting for resources owned by each other

Evidence:

```text
PS C:\Users\User\Downloads\ARSW\ARSW--LAB-3--Relic-Rush-Playbook> java -cp target/classes edu.eci.arsw.relicrush.app.LedgerRaceProbe 64 5000                                                                  
expected=320000 totalCrafted=8217 eventCount=251457 invariant=BROKEN
PS C:\Users\User\Downloads\ARSW\ARSW--LAB-3--Relic-Rush-Playbook> java -cp target/classes edu.eci.arsw.relicrush.app.LedgerRaceProbe 64 5000                                                                  
expected=320000 totalCrafted=5638 eventCount=246315 invariant=BROKEN
```

## 2. Coordination analysis

Explain the responsibility of both barriers:

- `roundStart`: this works as a synchronization at the beginning
of each round, each adventurer needs to wait until the coordinator 
is ready, this in order to not let players start the round 
before the others

- `roundEnd`: this work as a point of synchronization where 
the coordinator waits until the end of the turn of all the 
adventurer to read the scores and the ledger, in order to
not read an incomplete state while other threads are still 
working

Why is `Thread.sleep(...)` not a valid replacement for a barrier?
R/ is not a valid replacement bc it only stops the threads for 
an exact amount of time, this doesn´t guarantee that in this time
all the other threads will have finished their work. On the other
hand reading the snapshot after the barrier guarantees that all the 
changes made by the adventurer threads before reaching the barrier
are visible by the coordinator

## 3. Thread-safety problems

| Shared state  | Problem                                                                                            | Invariant at risk                                                  | Solution                                 | Why this solution?                                                       |
|---------------|----------------------------------------------------------------------------------------------------|--------------------------------------------------------------------|------------------------------------------|--------------------------------------------------------------------------|
| `totalCrafted` | The update is a non-atomic read-modify-write operation, so concurrent threads can lose increments. | `sum of player scores == totalCrafted == number of events`         | `synchronized` in the critical operation | It prevents multiple threads from updating the counter at the same time. |
| `events`      | `ArrayList` is not thread-safe for concurrent writes.                                              | Every crafted relic must be registered exactly once in the ledger. | `synchronized` access to the list        | It prevents concurrent modifications from losing or corrupting events.   |

## 4. Deadlock diagnosis

### 4.1 Evidence

```text
PS C:\Users\User\Downloads\ARSW\ARSW--LAB-3--Relic-Rush-Playbook> java -cp target/classes edu.eci.arsw.relicrush.app.DeadlockProbe                                                                            
DEADLOCK DETECTED
- probe-A-anvil-then-furnace waiting on edu.eci.arsw.relicrush.model.ForgeStation@79fc0f2f owned by probe-B-furnace-then-anvil
- probe-B-furnace-then-anvil waiting on edu.eci.arsw.relicrush.model.ForgeStation@17a7cec2 owned by probe-A-anvil-then-furnace

```

### 4.2 Coffman conditions in Relic Rush

- Mutual exclusion:
- Hold and wait:
- No preemption:
- Circular wait:

### 4.3 Wait-for graph

Describe or add a diagram.

### 4.4 Fix

What condition did you break?

How did you preserve concurrency between independent forge operations?

## 5. Verification

| Players | Stations | Rounds | Deadlock? | Invariant result |
|---:|---:|---:|---|---|
| 8 | 6 | 50 | | |
| 32 | 8 | 100 | | |
| 128 | 8 | 100 | | |

## 6. Architectural trade-offs

Discuss:

- Correctness / reliability
- Performance / throughput
- Contention
- Maintainability
- Scalability

## 7. Mini ADR

### Context

### Decision

### Alternatives considered

### Consequences

### Evidence

## 8. Conclusions

1.
2.
3.
