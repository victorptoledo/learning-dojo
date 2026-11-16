# Index Lab: The Right Index for Each Workload

Six tables, six workloads, six kinds of index: BRIN for an append-only time series, GIN for
arrays and documents, a unique expression index for case-insensitive emails, a partial index for
a queue, trigrams for fragments, and a covering index for an index-only scan. The tests read the
plans and check the type and size of every index. Press **Enter** to create the kata folder and
open it.
