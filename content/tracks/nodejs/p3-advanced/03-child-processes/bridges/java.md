---
from: java
status: draft
concept: "Starting other programs"
from_way: "ProcessBuilder with a command list, redirectOutput or getInputStream, waitFor with a timeout, destroy and destroyForcibly, and Runtime.exec(String) splitting on spaces."
native_way: "spawn with a command and an argument array, child.stdout and stdin as streams, the close event with code and signal, kill(\"SIGTERM\") then kill(\"SIGKILL\")."
sensei_tip: "ProcessBuilder(List) maps to spawn(cmd, args): no shell either way. destroy() is SIGTERM and destroyForcibly() is SIGKILL; there is no waitFor(timeout), so set the timeout option or a timer."
---

| Java | Node.js |
| --- | --- |
| `new ProcessBuilder("git", "log")` | `spawn("git", ["log"])` |
| `pb.directory(dir)` | `{ cwd: dir }` |
| `pb.environment().put("X", "1")` | `{ env: { ...process.env, X: "1" } }` |
| `process.getInputStream()` | `child.stdout` |
| `process.waitFor()` | `once(child, "close")` |
| `destroy()` / `destroyForcibly()` | `kill("SIGTERM")` / `kill("SIGKILL")` |
