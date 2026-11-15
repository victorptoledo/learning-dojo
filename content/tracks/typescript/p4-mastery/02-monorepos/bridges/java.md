---
from: java
status: draft
concept: "Multi-module builds"
from_way: "A Maven or Gradle multi-module build: modules with their own pom.xml or build.gradle, dependencies between them, a reactor that builds them in order with incremental compilation."
native_way: "Workspaces link packages, each package.json declares its exports, and project references with tsc -b play the role of the reactor: ordered, incremental, parallel builds."
sensei_tip: "Map each Maven module to a package with its own tsconfig.json (composite) and the parent pom to the solution tsconfig. module-info exports become package.json exports."
---

| Java | TypeScript |
| --- | --- |
| Parent `pom.xml` with `<modules>` | Root `package.json` `workspaces` + solution `tsconfig.json` |
| Module dependency | `"@acme/domain": "workspace:*"` + `references` |
| Maven reactor / Gradle build | `tsc -b` |
| `module-info.java` `exports` | `package.json` `exports` |
| Gradle build cache | `.tsbuildinfo`, Turborepo / Nx cache |
