# ARSW Lab 3 - Relic Rush - Delivery Report

## Team

| Student                     | ID         | GitHub           |
|-----------------------------|------------|------------------|
| Mabel Fernanda Bernal Amaya | 1000100629 | MabelBernalAmaya |
| Nicolas David Prieto Ramos  | 1000091873 | NicolasPrieto12  |
| Juan Eduardo Vera Acero     | 1000091871 | JUNE2908         |

Repository: `https://github.com/ARSW-LABORATORIOS/ARSW--LAB-3--Relic-Rush-Playbook.git`

Final commit: `SHA`

## 1. Baseline observations

- Command(s) executed:
  - `java -cp target/classes edu.eci.arsw.relicrush.app.LedgerRaceProbe 64 5000`
  - `java -cp target/classes edu.eci.arsw.relicrush.app.DeadlockProbe`

- What happened?
  `LedgerRaceProbe` showed different values between the expected number of crafted
  relics, `totalCrafted` and `eventCount`. `DeadlockProbe` detected a deadlock
  between two threads trying to acquire the same forge stations in opposite orders.

- Was the round invariant always preserved?
  No. The invariant was broken because `totalCrafted` and `eventCount` were different
  from the expected value due to lost increments and concurrent list writes.

- Did the game stop unexpectedly?
  Yes. `DeadlockProbe` detected that two threads were blocked waiting for resources
  owned by each other, causing the program to hang.

Evidence:

```text
PS C:\Users\User\Downloads\ARSW\ARSW--LAB-3--Relic-Rush-Playbook> java -cp target/classes edu.eci.arsw.relicrush.app.LedgerRaceProbe 64 5000
expected=320000 totalCrafted=8217 eventCount=251457 invariant=BROKEN

PS C:\Users\User\Downloads\ARSW\ARSW--LAB-3--Relic-Rush-Playbook> java -cp target/classes edu.eci.arsw.relicrush.app.LedgerRaceProbe 64 5000
expected=320000 totalCrafted=5638 eventCount=246315 invariant=BROKEN
```

## 2. Coordination analysis

Explain the responsibility of both barriers:

- `roundStart`: acts as a synchronization gate at the beginning of each round.
  Every adventurer waits here until the coordinator releases the round, so no
  player starts before all others are ready.

- `roundEnd`: acts as a synchronization point where the coordinator waits until
  every adventurer finishes its turn before reading the scores and the ledger.
  This guarantees the coordinator never reads an incomplete state while other
  threads are still working.

Why is `Thread.sleep(...)` not a valid replacement for a barrier?
It only stops a thread for a fixed amount of time, which does not guarantee that
all other threads will have finished their work within that time. A barrier
provides a hard guarantee: no thread passes until every participant has arrived.
Reading the snapshot after the barrier also guarantees that all writes made by
adventurer threads before reaching the barrier are visible to the coordinator
(happens-before guarantee).

## 3. Thread-safety problems

| Shared state   | Problem                                                                                             | Invariant at risk                                                   | Solution                          | Why this solution?                                                                                                                                  |
|----------------|-----------------------------------------------------------------------------------------------------|---------------------------------------------------------------------|-----------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------|
| `totalCrafted` | The update is a non-atomic read-modify-write operation, so concurrent threads can lose increments.  | `sum of player scores == totalCrafted == number of events`          | `AtomicInteger.incrementAndGet()` | Makes the read-modify-write a single atomic operation without needing a lock, so no increment is ever lost.                                         |
| `events`       | `ArrayList` is not thread-safe for concurrent writes.                                               | Every crafted relic must be registered exactly once in the ledger.  | `CopyOnWriteArrayList`            | Guarantees safe concurrent appends. Writes are infrequent (one per craft) and reads in `snapshot()` are completely lock-free, which fits the access pattern of this ledger. |

## 4. Deadlock diagnosis

### 4.1 Evidence

```text
PS C:\Users\User\Downloads\ARSW\ARSW--LAB-3--Relic-Rush-Playbook> java -cp target/classes edu.eci.arsw.relicrush.app.DeadlockProbe
DEADLOCK DETECTED
- probe-A-anvil-then-furnace waiting on edu.eci.arsw.relicrush.model.ForgeStation@79fc0f2f owned by probe-B-furnace-then-anvil
- probe-B-furnace-then-anvil waiting on edu.eci.arsw.relicrush.model.ForgeStation@17a7cec2 owned by probe-A-anvil-then-furnace
```

### 4.2 Coffman conditions in Relic Rush

- Mutual exclusion: each forge station can only be used by one thread at a time
  because the stations are protected using `synchronized` locks.

- Hold and wait: an adventurer holds the lock of the first forge station while
  waiting to acquire the second one.

- No preemption: a lock cannot be taken away from a thread. The thread must
  release the forge station itself after finishing the synchronized block.

- Circular wait: before the fix, two adventurers could request the same two
  stations in opposite orders. Thread A waited for a station owned by thread B
  while thread B waited for a station owned by thread A.

### 4.3 Wait-for graph

```text
Thread A -> Furnace -> Thread B -> Anvil -> Thread A
```

Thread A owns the Anvil and waits for the Furnace, while Thread B owns the
Furnace and waits for the Anvil. This creates a cycle — neither thread can
ever proceed.

### 4.4 Fix

What condition did you break?
Circular wait. The fix defines a deterministic order for the forge stations:
all threads always acquire the lock with the lower `id` first. This makes it
impossible for two threads to request the same pair of stations in opposite
orders, so the cycle can never form.

```java
ForgeStation lo = first.id() < second.id() ? first : second;
ForgeStation hi = first.id() < second.id() ? second : first;

synchronized (lo) {
    synchronized (hi) {
        action.run();
    }
}
```

How did you preserve concurrency between independent forge operations?
There is no global lock. Two adventurers using completely different stations
(e.g. `station-1 + station-3` and `station-2 + station-4`) never compete for
the same monitor and can still craft at the same time.

## 5. Verification

| Players | Stations | Rounds | Deadlock? | Invariant result |
|--------:|---------:|-------:|-----------|------------------|
|       8 |        6 |     50 | No        | OK               |
|      32 |        8 |    100 | No        | OK               |
|     128 |        8 |    100 | No        | OK               |

Evidence:

```text
InvariantProbe 8 6 50:
Total by players : 400
Ledger total     : 400
Ledger events    : 400

InvariantProbe 32 8 100:
Total by players : 3200
Ledger total     : 3200
Ledger events    : 3200

InvariantProbe 128 8 100:
Total by players : 12800
Ledger total     : 12800
Ledger events    : 12800
```

### Execution evidence

Maven tests:

command:
![Maven tests](images/imag5.png)
result:
![Maven tests](images/imag1.png)

Deadlock verification after the fix:

![Deadlock verification](images/imag2.png)

Invariant verification with 8 players:

command:
![InvariantProbe command](images/imag6.png)
result:
![InvariantProbe 8 players](images/imag3.png)

Stress verification with 128 players:

command:
![InvariantProbe 128 command](images/imag7.png)
result:
![InvariantProbe 128 players](images/imag4.png)

## 6. Architectural trade-offs

- Correctness / reliability:
  The final solution protects both game invariants. `AtomicInteger` and
  `CopyOnWriteArrayList` guarantee that every crafted relic is counted exactly
  once. The deterministic lock ordering in `LockPair.withBoth()` guarantees that
  no two threads can deadlock on station monitors.

- Performance / throughput:
  The solution still allows different players to craft at the same time when they
  use different forge stations. This is better than a global lock because the
  whole game does not need to wait for one player to finish before another can start.

- Contention:
  Contention appears when many players try to use the same forge stations at the
  same time. In that case some threads must wait until the stations are released.
  When the number of players increases and the number of stations stays the same,
  contention grows proportionally.

- Maintainability:
  The lock ordering rule is centralized in `LockPair.withBoth()`. No caller needs
  to know about the ordering — it is enforced unconditionally inside that method.
  Any future adventurer strategy that goes through `withBoth()` is automatically safe.

- Scalability:
  When the number of players grows while the number of forge stations stays constant,
  more threads compete for the same resources. The program still works correctly, but
  waiting time and contention increase. Even with 128 players and only 8 stations the
  verification finished successfully and the invariant remained OK in every round.

## 7. Mini ADR

### Context

Relic Rush requires every adventurer to acquire two forge stations before crafting
a relic. In the initial implementation, locks were acquired in the order requested
by each player. This allowed two threads to acquire the same stations in opposite
orders, creating a circular wait and producing a deadlock.

### Decision

Define a deterministic global order for the forge stations based on `ForgeStation.id()`.
Every thread acquires the two station locks in ascending id order before executing
the craft operation. This prevents circular wait because two threads can never request
the same pair of stations in opposite orders.

### Alternatives considered

- One global lock for all craft operations: prevents deadlocks but serializes the
  entire game — only one player can craft at a time, destroying concurrency.
- Timeouts and retries: allows threads to recover from some conflicts but adds
  complexity and does not eliminate the root cause of the deadlock.

### Consequences

The solution prevents circular wait without serializing the entire game. Players
using different forge stations can still craft concurrently. Threads that require
the same stations may still need to wait until those resources are released.

### Evidence

Before the fix, `DeadlockProbe` detected a deadlock between two threads acquiring
the Anvil and Furnace in opposite orders.

After applying the deterministic lock ordering, `DeadlockProbe` reported:

```text
NO DEADLOCK DETECTED within 2 seconds.
```

Stress tests with 8, 32 and 128 players all finished with `invariant=OK`.

## 8. Conclusions

1. With this lab we understood that concurrency problems are not only related to
   performance but also to correctness and coordination between threads.

2. We learned that shared state must be protected correctly to avoid race conditions,
   and that deadlocks can be prevented by using a deterministic order when acquiring
   multiple locks.

3. The final stress tests showed that the solution preserves the game invariant even
   with a high number of players, while still allowing concurrent execution between
   independent forge operations.
