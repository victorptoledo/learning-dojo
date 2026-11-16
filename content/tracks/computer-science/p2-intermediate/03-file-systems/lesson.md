# File Systems, Storage and I/O

Memory forgets when the power goes; storage does not. Between your `write` call and bits on a
flash chip sit a page cache, a file system with its own on-disk data structures, a block layer,
a driver and a device with its own cache. Each layer makes I/O fast, and each can lose data if
you do not know what it promises.

## Devices and how the kernel talks to them

- **Hard disk drives** (HDDs) have spinning platters and a moving head. A random access costs a
  **seek** plus rotation, 5–10 ms; sequential transfer is 100–250 MB/s. Random I/O is ~100 times
  slower than sequential.
- **Solid-state drives** (SSDs, NVMe) have no moving parts: random reads take tens of
  microseconds, and an NVMe drive handles hundreds of thousands of operations per second over many
  parallel queues. Flash is written in pages but erased in large blocks, so the drive's firmware
  (the **flash translation layer**) remaps writes and garbage-collects; this is why SSDs have
  write amplification and a limited write endurance.
- Devices are slow compared to the CPU, so the kernel does not wait in a loop. It sends a command
  and the device moves data with **DMA** (direct memory access) into RAM by itself, then raises an
  **interrupt** when done. A **driver** is the kernel code that speaks one device's protocol.
- The **block layer** queues and merges requests (I/O schedulers such as `mq-deadline` and `none`
  for NVMe).

## I/O from a program's point of view

Everything is a **file descriptor**: an integer index into the process's table of open files,
sockets and pipes. `read` and `write` are system calls on it.

- **Blocking I/O**: the thread sleeps until the data is there. Simple; needs a thread per
  concurrent operation.
- **Non-blocking I/O with readiness** (`epoll` on Linux, `kqueue` on BSD and macOS): one thread
  asks "which of these thousands of sockets are ready?" This is what event loops (Node.js, Netty,
  Go's network poller, Python's `asyncio`) are built on.
- **Asynchronous I/O with completion** (`io_uring` on Linux, IOCP on Windows): submit operations
  and collect completions later, with few system calls.
- **Buffering**: libraries batch small writes in user space (`BufferedWriter`, `bufio`, Python's
  file objects) because each system call has a fixed cost.

## The page cache

The kernel caches file data in otherwise free RAM: the **page cache**. A `read` of cached data is
a memory copy; a `write` only copies into the cache, marks the pages **dirty** and returns. The
kernel writes dirty pages back later (typically within 5–30 seconds) or under memory pressure.

- This is why the second run of a program that reads a big file is much faster.
- This is why "free" memory on Linux is usually low: the cache uses it and gives it back on
  demand.
- And this is why a successful `write` does **not** mean the data survives a power cut.

## On-disk structures: inodes and directories

A Unix file system (ext4, XFS, and conceptually most others) stores:

- **Superblock**: the file system's size, layout and state.
- **Inodes**: one per file, holding its metadata (type, size, owner, permissions, timestamps, link
  count) and where its data lives: block pointers or **extents** (start block + length). The
  inode does **not** hold the file's name.
- **Data blocks** (usually 4 KiB), and bitmaps or trees tracking which are free.
- **Directories**: special files whose content is a list of (name → inode number) entries.

Resolving `/home/ana/notes.txt` reads the root directory's inode, finds `home`, reads that
directory, finds `ana`, and so on: one lookup per component (cached in the kernel's dentry
cache).

- A **hard link** is a second directory entry for the same inode; the file is deleted only when
  the link count reaches 0 *and* no process has it open (that is why deleting a log file that a
  process still writes frees no space).
- A **symbolic link** is a small file containing a path, resolved at access time; it can dangle.
- `rename` within one file system only changes directory entries: it is **atomic**.

## Crash consistency

Updating a file touches several blocks: the data, the inode, a bitmap, maybe a directory. A crash
in the middle can leave them inconsistent (an inode pointing at a block marked free). Solutions:

- **fsck**: scan everything after a crash and repair. Slow on large disks.
- **Journaling** (ext4, XFS, NTFS): write the intended changes to a log (the **journal**) first,
  commit, then apply them in place. After a crash, replay committed journal entries. By default
  ext4 journals only **metadata** (`data=ordered` writes the data blocks before committing the
  metadata that points to them).
- **Copy-on-write** file systems (ZFS, Btrfs, APFS): never overwrite in place; write new blocks
  and atomically switch a root pointer. This also gives cheap snapshots.

Journaling is the same idea as a database's write-ahead log, which you will build in phase 4.

## What durability really takes

- `fsync(fd)` forces the file's dirty data and metadata to stable storage, including a flush of
  the device's volatile cache. It costs from tens of microseconds (good NVMe) to milliseconds.
- `fsync` on the file does not make a *new name* durable: for that, `fsync` the **directory**.
- To replace a file safely: write a temporary file in the same directory, `fsync` it, `rename` it
  over the old one, `fsync` the directory. Readers see the old or the new content, never a mix.
- `O_DIRECT` bypasses the page cache (databases that manage their own cache use it); `O_SYNC`
  makes every write synchronous.
- Batch durability: databases **group commit**, one `fsync` for many transactions.
- PostgreSQL's 2018 "fsyncgate": on Linux, a failed writeback could be reported once and then
  forgotten, so a later `fsync` succeeded although data was lost. The lesson: treat an `fsync`
  error as fatal.

## Summary

- HDDs punish random I/O; SSDs are fast at random reads but have erase blocks and write
  amplification.
- Devices use DMA and interrupts; programs see file descriptors and blocking, readiness-based or
  completion-based I/O.
- The page cache makes reads fast and writes lazy: a `write` is not durable.
- Inodes hold metadata and block locations; directories map names to inodes; rename is atomic.
- Journaling and copy-on-write make file systems crash-consistent; `fsync` plus atomic rename
  makes your files crash-safe.

## Further reading

- Arpaci-Dusseau and Arpaci-Dusseau, *Operating Systems: Three Easy Pieces*, chapters 36–45 (I/O devices, disks, SSDs, files and directories, file system implementation, crash consistency and journaling): <https://pages.cs.wisc.edu/~remzi/OSTEP/>
- Silberschatz, Galvin and Gagne, *Operating System Concepts*, 10th ed., chapters 11–15 (mass storage, I/O systems, file systems)
- Kerrisk, *The Linux Programming Interface*, chapters 13 "File I/O Buffering" and 14 "File Systems"
- Pillai et al., "All File Systems Are Not Created Equal: On the Complexity of Crafting Crash-Consistent Applications" (OSDI 2014): <https://www.usenix.org/conference/osdi14/technical-sessions/presentation/pillai>
- Dan Luu, "Files are hard": <https://danluu.com/file-consistency/>
