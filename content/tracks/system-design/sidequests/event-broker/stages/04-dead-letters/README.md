One message the handler can never process (a bad payload, a deleted account) and the
partition stops: every run fails on the same offset. That is a **poison message**.

**Retries and dead letters**

- A handler that throws a `RuntimeException` is called again for the same message, right
  away, up to `config.maxAttempts()` calls in all. An `Error` still escapes at once: that is
  the process dying, not the message failing.
- After the last failed call the message goes to the dead-letter topic `<topic>.dlq`, created
  when first needed with the partitions and retention of the topic (the broker gets
  `public Duration retention(String topic)` for that). The dead letter keeps the key and the
  value, so it lands in the same partition number.
- Then the partition moves on: the store records the message's offset + 1 (with no effect
  writes), so the poison message is never handled again, and the messages behind it are.
  `runOnce` counts only messages whose effects were applied.
- A failing message in one partition never stops the other partitions.

**Lag**

```java
public long lag(String group)
```

- The lag of a group is, over every partition of its topic, `endOffset` minus the committed
  offset (`startOffset` when there is no commit, or when the commit is older than it). It is
  the number every operations dashboard of a consumer shows: how far behind the producers it
  is. An unknown group throws `NoSuchElementException`.
