---
from: python
status: draft
concept: "JIT vs AOT compilation"
from_way: "An interpreter that starts in milliseconds, and tools like PyInstaller to bundle apps."
native_way: "Ahead-of-time compilation with GraalVM native image: fast start, small memory, a closed world at build time."
sensei_tip: "A native image starts as fast as a Python script but needs to know at build time every class you load by reflection, which most frameworks now generate for you."
---

| Python | Java 25 |
| --- | --- |
| PyInstaller bundle | native-image binary |
| fast startup | native image or the AOT cache |
| importlib dynamic imports | reflection metadata needed |
| python image size | a small native image |
