# Checkpoint 1: Fundamentals

This checkpoint closes the **Fundamentals** phase. It has two parts, and you need both:

1. **The quiz** (press **Q**): eight questions over the whole phase. You need 7 right.
2. **The integration kata** (press **Enter**): `dupes`, a command-line tool that walks a folder
   with an async generator, hashes files, groups duplicates and reports them with the right
   exit code.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| The Event Loop and libuv | In which order do nextTick, promises, setImmediate and timers run? What blocks the loop, and what uses the thread pool? |
| Callbacks, Promises and async/await | Why can a `throw` in a callback not be caught? When `Promise.all`, when sequential `await`? What does an unhandled rejection do? |
| EventEmitter | Why must every emitter that can fail have an `"error"` listener? Why not emit in a constructor? |
| The File System, Paths and URLs | How do you tell "missing" from "forbidden"? Relative to what is `./file`? How do you write a file atomically? |
| Processes and Command-Line Programs | What goes to stdout and what to stderr? Why `process.exitCode` and not `process.exit()`? |

> [!TIP]
> If a quiz question surprises you, open the lesson it comes from again before retrying. The
> explanation after each answer says which idea it tests.
