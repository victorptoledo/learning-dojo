A replicated log is only useful for what it feeds: a **state machine** that every node
builds by applying the committed entries in order. Make it a key-value store.

```java
public long submit(String client, long sequence, String operation)   // proposes "<client> <sequence> <operation>"
public long lastApplied()
public Map<String, String> data()
public Optional<String> read(String key)
```

**Applying.** Every node applies its committed entries in index order, as soon as its commit
index moves (`lastApplied` follows `commitIndex`). A command is
`<client> <sequence> set <key> <value>` (the value may contain spaces) or
`<client> <sequence> del <key>`; anything else (a plain `propose("hello")`, say) is applied as
nothing.

**Duplicates.** A client that gets no answer retries, and the same command can end up twice in
the log (the leader crashed after appending it). Each client numbers its commands from 1;
the state machine remembers the highest sequence applied per client and applies a command
only when its sequence is higher. A stale retry never undoes a newer write.

**Reads.** Reading a follower can return stale data, and so can reading a leader that was
deposed without knowing it. `read` serves a key only from a leader that can prove it still is
one:

- not the leader: `NotLeaderException` with the leader it knows;
- fewer than a majority (itself included) answered one of its `AppendEntries` in the last
  `config.electionTimeout()` ticks (counting ticks of the leader itself): it may have been
  replaced, so `NotLeaderException` with no hint;
- no entry of its current term is committed yet (a new leader does not know what is
  committed until then): `IllegalStateException`;
- otherwise the value from its state machine.
