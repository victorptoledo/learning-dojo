A topic is a set of partitions; a partition is a log where every message gets the next
offset, from 0, and is never changed.

```java
public Broker(Clock clock, BrokerConfig config)
public BrokerConfig config()
public void createTopic(String topic, int partitions, Duration retention)
public int partitions(String topic)
public Sent send(String topic, String key, String value)
public List<Message> read(String topic, int partition, long fromOffset, int max)
public long startOffset(String topic, int partition)   // the first offset still kept
public long endOffset(String topic, int partition)     // the offset the next message gets
public int enforceRetention()                           // how many messages it deleted
```

**Sending**

- A message with a key goes to partition `Math.floorMod(Hashing.hash64(key), partitions)`, so
  all the messages of a key are in one partition, in the order they were sent. That is the
  only order a log promises.
- A message without a key (`null`) goes round the partitions of its topic: 0, 1, 2, ..., 0.
- `send` returns `Sent(partition, offset)` and stores a `Message` with the topic, partition,
  offset, key, value and the clock's time.

**Reading**

- `read` returns up to `max` messages of one partition from `fromOffset` on, in offset order.
  Reading at `endOffset` returns an empty list (nothing new yet).
- An offset below `startOffset` or above `endOffset` throws `OffsetOutOfRangeException`: the
  consumer must decide where to start again.

**Retention**

- `enforceRetention` deletes, in every partition, the messages whose time plus the topic's
  retention is at or before now. Only the oldest messages go: a log is cut from its start, and
  offsets are never reused (`startOffset` moves on, `endOffset` does not change).

**Errors**: an unknown topic or partition throws `NoSuchElementException`; a topic that exists
already, a blank name, fewer than 1 partition or a retention that is not positive throws
`IllegalArgumentException`.
