# Checkpoint 3: Advanced

This checkpoint closes the **Advanced** phase. It has two parts, and you need both:

1. **The quiz** (press **Q**): eight questions over the whole phase. You need 7 right.
2. **The integration kata** (press **Enter**): a tiny lakehouse table (Parquet files and a
   transaction log with commits, conflicts, time travel, pruning, compaction and vacuum)
   and an exactly-once sink that moves events from a Kafka-like topic into it, with the
   offsets committed together with the rows.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| Warehouses, lakes and lakehouses | What does a table format add to a folder of Parquet files? Why does time travel cost storage? |
| Columnar storage and partitioning | Which decisions can a reader take from metadata alone? What makes too many partitions hurt? |
| A partitioned Parquet lake (kata) | Where could a reader see half a compaction, and how does a log close that gap? |
| Streaming with Kafka | When do you commit offsets for at most once, at least once and effectively once? |
| Event-time windows (kata) | Why partition by event time and not by arrival time? |
| Apache Spark | Which transformations shuffle? What does skew look like, and how do you fix it? |

> [!TIP]
> If a quiz question surprises you, open the lesson it comes from again before retrying. The
> explanation after each answer says which idea it tests.
