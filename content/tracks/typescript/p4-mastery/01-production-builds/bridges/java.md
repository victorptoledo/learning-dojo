---
from: java
status: draft
concept: "From source to deployable artefact"
from_way: "javac or Gradle produce class files and a JAR; the compiler is the only way to run code, so it always checks types; CI runs mvn verify."
native_way: "Node can run .ts directly by stripping types, tsc compiles to .js and .d.ts, bundlers produce single files; none of the runners check types, so tsc runs as its own CI step."
sensei_tip: "There is no 'it compiled, so it type-checks' in TypeScript production. Make tsc --noEmit as mandatory as compilation is in Maven, and pin the compiler like a Maven plugin version."
---

| Java | TypeScript |
| --- | --- |
| `mvn package` → JAR | `tsc -p tsconfig.build.json` → `dist/`, or run the `.ts` sources |
| Fat JAR / shading | Bundling with esbuild or tsdown |
| `-g` debug info | `sourceMap` + `--enable-source-maps` |
| Compiler plugin version in `pom.xml` | `"typescript": "7.0.2"` in `devDependencies` |
| `mvn verify` in CI | `npm ci && tsc && eslint . && node --test` |
