---
from: java
status: draft
concept: "Packages, modules and artifacts"
from_way: "Packages that mirror folders, imports resolved at compile time from the classpath, JPMS module-info for exports, JARs as artifacts, Maven coordinates and publishing to Maven Central."
native_way: "Packages are folders with __init__.py, imports run module code at run time from sys.path, __all__ and _private names mark the API, wheels are the artifacts, and uv publish uploads to PyPI."
sensei_tip: "A Python import executes code, unlike a Java import, which only names a class. Keep module top levels free of work, or importing your package will be slow and full of side effects."
---

| Java | Python |
| --- | --- |
| `package com.acme.invoicer;` | the folder `src/invoicer/` |
| `import com.acme.invoicer.Invoice;` | `from invoicer import Invoice` |
| classpath | `sys.path` |
| `module-info.java` exports | `__all__` and `_private` names |
| JAR | wheel (`.whl`) |
| `Main-Class` in the manifest | `[project.scripts]` |
| `mvn deploy` to Maven Central | `uv publish` to PyPI |
| `groupId:artifactId:version` | `name` and `version` in `[project]` |
