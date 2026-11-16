# Storage Engine

Write the core of a database's durability in `KvStore.java`: a key-value store whose every change
is appended to a **write-ahead log** of checksummed frames and forced to disk before the call
returns, that **recovers** after a crash by replaying the log and cutting off a torn tail, applies
**batches atomically**, and **checkpoints** to a snapshot file replaced atomically. `Mutation` and
`RecoveryInfo` are already written.

## The API

| Method | Does |
| --- | --- |
| `KvStore.open(dir)` | creates `dir` if needed, recovers (see below) and returns the store |
| `get(key)` | the value, or `Optional.empty()` |
| `put(key, value)`, `delete(key)` | a batch of one mutation |
| `apply(List<Mutation>)` | all mutations as **one** frame, applied in order; an empty list does nothing |
| `size()`, `entries()` | the number of keys, and a sorted **copy** of all entries |
| `lastRecovery()` | what `open` found: `RecoveryInfo(batchesReplayed, bytesDiscarded)` from the log |
| `checkpoint()` | writes a snapshot and empties the log |
| `close()` | closes the files; calling it again does nothing |

Keys must be non-empty (`IllegalArgumentException`; validate a whole batch before writing any of
it). Values may be empty. Any call except `close` and `lastRecovery` on a closed store throws
`IllegalStateException`.

## The log format

The log is the file `wal.log` in `dir`, a sequence of **frames**, all integers big-endian (as
`DataOutputStream` and `ByteBuffer` write them):

```text
frame    := length:int32  crc:int32  payload            length = bytes of payload
payload  := count:int32  mutation × count
mutation := 1:int8  keyLen:int32 key  valueLen:int32 value       a put
          | 2:int8  keyLen:int32 key                             a delete
```

Strings are UTF-8. `crc` is the `java.util.zip.CRC32` of the payload, stored as `(int)
crc.getValue()`. A write appends its frame and calls `FileChannel.force` before returning: when
`put` returns, the frame is on disk, byte for byte (the hidden tests read the file).

## Recovery

`open`:

1. Deletes a leftover `data.snapshot.tmp` (a checkpoint that crashed before its rename).
2. Loads `data.snapshot` if it exists. It is a single frame of puts; if it is damaged, `open`
   throws `IOException` (snapshots are replaced atomically, so damage is not a crash artefact).
3. Replays the frames of `wal.log` in order. It stops at the **first** frame that is incomplete,
   has an impossible length, or fails its checksum or parsing, **truncates** the file there and
   counts the bytes it removed. Later writes are appended after the last valid frame.

## Checkpoints

`checkpoint()` writes all entries, as one frame of puts **in key order**, to `data.snapshot.tmp`,
forces it, renames it over `data.snapshot` with `Files.move(..., ATOMIC_MOVE, REPLACE_EXISTING)`,
and only then truncates `wal.log` to zero. A crash at any point leaves a recoverable state, because
replaying puts and deletes over a snapshot that already contains them gives the same result.

## What the hidden tests check

The basics and Unicode; the exact bytes of the log after each write; a hand-written log; torn
writes cut inside the payload and inside the header; garbage, flipped bits, a damaged middle
frame, a torn batch and an impossible length; checkpoints, the snapshot's bytes, a stale
temporary file, a crash between rename and truncation, a corrupt snapshot; and 100,000 entries
within a time limit.

Run the visible tests with `mvn test` or from your IDE.
