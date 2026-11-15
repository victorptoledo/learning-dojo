---
from: python
status: draft
concept: "Many concurrent tasks without callbacks"
from_way: "asyncio for many concurrent I/O tasks, or threads limited by the GIL."
native_way: "Virtual threads: write plain blocking code and run millions of cheap threads."
sensei_tip: "Virtual threads give you asyncio's scale without async and await: each request gets a thread that blocks normally, and the JVM parks it while it waits."
---

| Python | Java 25 |
| --- | --- |
| async def handle() | a normal method run on a virtual thread |
| asyncio.gather(a(), b()) | executor.submit(a); executor.submit(b) in try-with-resources |
| asyncio.Semaphore | Semaphore |
| run_in_executor | not needed: blocking is fine |
