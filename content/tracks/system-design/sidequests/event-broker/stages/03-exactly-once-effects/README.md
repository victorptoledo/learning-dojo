At-least-once delivery means duplicates. Remove them at both ends.

**An idempotent producer.** A producer that sends, loses the acknowledgement and sends again
must not write the message twice.

```java
public Sent send(String topic, String key, String value, String producerId, long sequence)
```

- Each producer numbers its messages 0, 1, 2... (one sequence per producer, whatever the
  topic). A sequence the broker already accepted from that producer appends nothing and
  returns the `Sent` it got the first time.
- A sequence that skips one (`last + 1` expected) throws `OutOfOrderSequenceException` and
  appends nothing: a message in between was lost.

**A consumer with exactly-once effects.** The consumer cannot stop the broker from
redelivering, but it can make a redelivery harmless: store the offset **in the same atomic
write** as the effects, and trust that offset over the broker's.

```java
public Processor(Broker broker, String group, String topic, String member, Store store, Handler handler)
public int runOnce(int max)   // how many messages had their effects applied
```

`Store` (a database with atomic batches of writes) and `Handler` (what a message does:
`Map<String, String> handle(Message message, Store store)`, the writes to make) are new in the
starter, and the broker gets one more method:

```java
public void seek(String group, String member, int partition, long offset)
```

- `seek` moves the member's position in one of its partitions (`IllegalStateException` when
  the partition is not the member's).
- `runOnce` joins the group (joining twice changes nothing), and when the generation is not
  the one it saw last time, seeks each of its partitions to the offset kept in the store.
  Then it polls up to `max` messages.
- For each message, the store key `offset:<topic>:<partition>` says where the partition is.
  A message below it was applied before: skip it. Otherwise call the handler and apply its
  writes **plus** the new value of that key (the message's offset + 1) in one `store.apply`.
- After the batch it commits the broker offsets too (they help a group that starts without
  the store). A `CommitFailedException` there is ignored: the store has the truth.
- A handler that throws stops `runOnce` with that exception: nothing of that message is
  written, and the process may be gone (the tests throw an `Error` to play a crash). A new
  processor on the same store, as another member, picks up where the store says.
