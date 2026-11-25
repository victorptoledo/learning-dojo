# Checkpoint 3: Advanced

This checkpoint closes the **Advanced** phase. It has two parts, and you need both:

1. **The quiz** (press **Q**): eight questions over the whole phase. You need 7 right.
2. **The integration kata** (press **Enter**): a partitioned store whose leaders replicate
   asynchronously. Its router sends each read to a follower that already has what the session
   saw (**read-your-writes** and **monotonic reads**). It **fails over** by epochs, and it never
   lets a node or a session use a write that the failover lost.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| Replication, Sharding and Partitioning | What can an asynchronous failover lose? Which anomaly does each session guarantee prevent? |
| Partition Rebalancer (kata) | Why fixed partitions? What moves when a node joins? |
| CAP, PACELC and Quorums | Which operations need linearizability? What do I trade for latency when there is no partition? |
| Quorum Store (kata) | Why does `W + R > N` overlap? What does read repair fix? |
| Messaging and Event Patterns | Queue or log? Where is order guaranteed? What makes a consumer idempotent? |

> [!TIP]
> A follower that copies the leader's log is a consumer with an offset, and its lag is the
> consumer lag of the messaging lesson. Keep that picture in mind for the kata.

> [!TIP]
> If a quiz question surprises you, open the lesson it comes from again before retrying. The
> explanation after each answer says which idea it tests.
