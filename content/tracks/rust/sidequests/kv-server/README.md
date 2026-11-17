# Sidequest: TCP key-value server

Build `kvd`, a small Redis-like server: clients connect over TCP and send one command per
line. You start from an empty `main` and the standard library only (`std::net`,
`std::thread`, `std::sync`, `std::fs`): no async runtime, no crates, since the checks run
offline. Threads, channels, `Arc<Mutex<..>>` or `RwLock` are all fair game, and choosing
between them is part of the work.

The contract tests start your binary as a real process, connect to it with `TcpStream`, and
check the replies, the files it writes and how it exits.

## Starting

`kvd --port <port>` listens on `127.0.0.1:<port>`. With `--port 0` the system picks a free
port. Once it is listening, the **first line on stdout** is `listening on 127.0.0.1:<port>`
with the real port (flush it: the tests wait for that line).

## The protocol

A command is one line ending in `\n` (accept `\r\n` too). Words are separated by one space;
commands are upper case. Every command gets exactly one reply line.

Keep the package name `kvd`.
