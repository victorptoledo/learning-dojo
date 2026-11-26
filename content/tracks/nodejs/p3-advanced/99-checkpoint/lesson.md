# Checkpoint 3: Advanced

This checkpoint closes the **Advanced** phase. It has two parts, and you need both:

1. **The quiz** (press **Q**): eight questions over the whole phase. You need 7 right.
2. **The integration kata** (press **Enter**): a batch job runner. It reads a manifest of jobs
   that someone else wrote, hashes files on worker threads, runs a few allow-listed programs in
   child processes and never lets a path leave its folder.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| Worker Threads for CPU-Bound Work | Which work belongs on a worker, and which does libuv already do off the main thread? What does `postMessage` copy, and what can be transferred? How does a worker's error reach the main thread? |
| Worker Pool (kata) | Why reuse workers? How do you know which reply belongs to which task? What happens to a task whose worker dies? |
| Child Processes: spawn, exec and Pipes | Why an argument array instead of a shell string? What does a child inherit from its parent? How do you stop a child that hangs, and when is the result known? |
| Run Command (kata) | Why keep reading output you are going to drop? What does `"close"` tell you that `"exit"` does not? |
| Secure Node.js | Where do you validate untrusted input? How do prototype pollution and path traversal work, and what about a symbolic link? What does the permission model limit? |

> [!TIP]
> The manifest in the kata is **untrusted input**, like a request body. Every rule in the
> README answers a real attack: `__proto__` keys, an argument that becomes an option, a file
> name with `..`, a link to `/etc`, a tool that never ends or prints gigabytes.

> [!TIP]
> If a quiz question surprises you, open the lesson it comes from again before retrying. The
> explanation after each answer says which idea it tests.
