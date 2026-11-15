---
from: javascript
status: draft
concept: "JIT vs AOT compilation"
from_way: "Node starts fast and V8 compiles just in time; bundlers such as esbuild produce single files."
native_way: "A JIT runtime by default, or a native binary built by GraalVM."
sensei_tip: "Like bundling with esbuild, native image does tree shaking: code not reachable at build time is removed, so dynamic loading needs configuration."
---

| JavaScript / TypeScript | Java 25 |
| --- | --- |
| esbuild bundle | native-image |
| node --snapshot-blob | the JVM AOT cache |
| dynamic require() | reflection or resource config |
| startup in ms | native image startup in ms |
