`kvd --port 0 --data <file>` keeps its data in `<file>`.

- At start, the data in `<file>` is loaded (a missing file means no data yet).
- Every write (`SET`, `DEL`, `INCR`) is on disk **before** its reply is sent. The tests kill
  the server with `SIGKILL` right after a reply and start it again: every acknowledged write
  must be there. The file format is yours (values may hold spaces).
- The file holds data clients wrote: create it with mode `0600` (only its owner can read it).
- A crash halfway through writing must never leave a broken file. Write the new content to a
  temporary file in the same folder, then `rename` it over the old one: on Linux a rename is
  atomic. No temporary file may be left behind after a write.

Without `--data`, the data lives in memory only, as before.
