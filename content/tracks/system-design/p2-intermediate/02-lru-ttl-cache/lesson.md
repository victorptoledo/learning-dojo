# LRU + TTL Cache

Build the in-process cache every service ends up needing: bounded by size with
least-recently-used eviction, bounded in staleness with a time to live per entry, with hit and
miss statistics and a `getOrLoad` that loads a missing key only once even when many threads ask
at the same time. Press **Enter** to create the kata folder and open it.
