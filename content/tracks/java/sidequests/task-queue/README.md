# Sidequest: Resilient task queue

Workers crash, networks drop, handlers throw. A queue that promises **at-least-once
delivery** never loses a task: a worker *leases* a task, and only an *ack* finishes it. A task
whose lease runs out comes back for another worker. Tasks that keep failing wait longer and
longer (exponential backoff) and finally land in a dead-letter list for a human to look at.

Build `dojo.tasks.TaskQueue`. The starter gives you the contract types (`Task`,
`QueueConfig`, `Status`) and the method signatures; how you store and order tasks is yours.

Time comes from a `java.time.Clock` given to the constructor: never call `Instant.now()`
directly. The contract tests pass a clock they move by hand, so a five-minute lease takes no
time at all to expire.

The last stage moves the design to PostgreSQL: the same states and rules as a table and a
claim query, checked in the `dojo-sql` image. It is the shape a real queue takes when the
tasks must survive the process.

Use the JDK only.
