---
from: java
status: draft
concept: "Modules, dependencies and a lockfile"
from_way: "Packages and the module system, dependencies declared in pom.xml or build.gradle with exact versions, resolved from Maven Central into ~/.m2."
native_way: "Files are modules with import/export, dependencies declared in package.json with version ranges, installed from the npm registry into the project's node_modules and pinned by package-lock.json."
sensei_tip: "Maven versions are exact by default; npm's default ^ range is not. The lockfile is what gives you Maven-like reproducibility, so commit it and use npm ci."
---

| Java | Node.js |
| --- | --- |
| `import com.acme.util.Strings;` | `import { slug } from "./strings.ts"` |
| `module-info.java` exports | `exports` field of package.json |
| `pom.xml` | `package.json` |
| `<scope>test</scope>` | `devDependencies` |
| Maven Central, `~/.m2` | npm registry, `node_modules` per project |
| `mvn verify` | `npm ci && npm test` |
