# Sidequest: Job scheduler

Build `dojo/scheduler`, the engine behind nightly builds, report generators and data jobs:
it runs jobs on a fixed number of workers, retries the flaky ones, gives up on the stuck
ones, runs jobs in dependency order, and stops cleanly when the process must go.

The starter fixes the contract: `Job`, `Result`, `Status`, `New`, `Submit` and `Wait`, and
later stages add fields and one method. The contract tests submit jobs that block on
channels, fail, panic or sleep, and check what ran, in which order, how many at once, and
what `Wait` reports. Mutexes, condition variables, channels, a dispatcher goroutine: the
design is yours. Every check runs under the race detector, so shared state must be guarded.

A job's `Run` receives a `context.Context`; good jobs return soon after it is done.
