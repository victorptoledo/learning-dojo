---
from: python
status: draft
concept: "subprocess in Node.js"
from_way: "subprocess.run with a list of arguments, check=True, capture_output and timeout, shell=True as the dangerous option, and Popen for streaming."
native_way: "execFile for subprocess.run-style calls, spawn for Popen-style streaming, the shell option as the dangerous one, and the timeout option or a timer."
sensei_tip: "The rule is identical: a list of arguments, never shell=True with user input. Node.js has no check=True, so test the exit code yourself (or use a small runOrThrow like the kata's)."
---

| Python | Node.js |
| --- | --- |
| `subprocess.run(["git", "log"], capture_output=True)` | `await execFileAsync("git", ["log"])` |
| `check=True` | Check `code`, or the rejected promise of promisified execFile |
| `timeout=30` | `{ timeout: 30_000 }` |
| `Popen(..., stdout=PIPE)` | `spawn(...)`, `child.stdout` |
| `shell=True` | `exec(...)` / `{ shell: true }` |
| `proc.terminate()` / `proc.kill()` | `kill("SIGTERM")` / `kill("SIGKILL")` |
