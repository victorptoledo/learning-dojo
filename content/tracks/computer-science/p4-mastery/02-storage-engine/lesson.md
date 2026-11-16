# Storage Engine

Build the part of a database that keeps its promises when the power goes: a write-ahead log of
length-prefixed, CRC-checked frames forced to disk on every write, recovery that replays the log
and cuts off a torn tail, atomic batches, and checkpoints that write a snapshot and switch to it
with an atomic rename. The hidden tests play the crash for you: they truncate, corrupt and splice
your files between runs, and your store must come back with exactly the committed data. Press
**Enter** to create the kata folder and open it.
