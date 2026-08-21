# ADR-001: Deadlock prevention strategy

## Context

Relic Rush requires every adventurer to acquire two forge stations before
crafting a relic. In the initial implementation, the locks were acquired in
the order requested by each player. This allowed two threads to acquire the
same stations in opposite orders, creating a circular wait and producing a
deadlock.

The solution must prevent deadlocks while preserving concurrency between
players that use different forge stations.

## Decision

We decided to define a deterministic global order for the forge stations.
Every thread must acquire the two station locks following the same order
before executing the craft operation.

This prevents the circular wait condition because two threads cannot acquire
the same pair of stations in opposite orders.

## Alternatives considered

One alternative was to use one global lock for all craft operations. This
would prevent deadlocks, but it would reduce concurrency because only one
player could craft at a time.

Another alternative was to use timeouts or retries when acquiring locks.
This could allow threads to recover from some conflicts, but it would make
the solution more complex and would not eliminate the root cause of the
deadlock.

## Quality attributes affected

- Correctness / reliability: the deterministic ordering prevents circular
wait and eliminates deadlocks caused by inconsistent lock acquisition order.
- Performance / throughput: independent craft operations can still execute
concurrently because there is no global lock.
- Maintainability: the locking rule is explicit and can be followed in future
changes.
- Scalability: more players can participate concurrently, although contention
can increase when many players compete for the same stations.

## Evidence

Before the fix, DeadlockProbe detected a deadlock between two threads
acquiring the Anvil and Furnace in opposite orders.

After applying the deterministic lock ordering, DeadlockProbe reported:

NO DEADLOCK DETECTED within 2 seconds.

The stress tests also completed successfully:

8 players, 6 stations, 50 rounds: invariant=OK
32 players, 8 stations, 100 rounds: invariant=OK
128 players, 8 stations, 100 rounds: invariant=OK

## Consequences

The solution prevents circular wait without serializing the entire game.
Players using different forge stations can still craft concurrently.

However, threads that require the same stations may still need to wait until
those resources are released.

## Risks

A future change could introduce deadlocks again if new code acquires forge
station locks without following the same deterministic ordering rule.

Another risk is increased contention when the number of players grows while
the number of available forge stations remains limited.
