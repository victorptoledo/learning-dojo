# Sidequest: Event broker

Kafka, Pulsar and Kinesis are built on one idea: an append-only **log**, split into
**partitions**, that consumers read at their own pace by **offset**. Everything else (order by
key, consumer groups, at-least-once delivery, idempotent producers, exactly-once effects, dead
letters) follows from it, and so do their classic bugs: the zombie consumer that commits after
it lost its partitions, the retry that writes a message twice, the poison message that blocks
a partition forever.

Build `dojo.broker.Broker`, a broker in one process, and, from stage 3, `dojo.broker.Processor`,
a consumer that applies the effects of each message exactly once. The starter gives you the
contract types: `Message`, `Sent`, `BrokerConfig`, `Hashing` and the exceptions; later stages
add more.

There is no network and no threads: the tests call the broker directly, and a crash is a
consumer that stops calling it. Time comes from the `java.time.Clock` given to the
constructor; the tests move it by hand, so retention and session timeouts are exact.

Use the JDK only.
