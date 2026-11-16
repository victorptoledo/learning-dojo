---
from: java
status: draft
concept: "Build pipelines"
from_way: "Maven or Gradle: dependency resolution, compilation, the shade plugin for fat JARs, and ProGuard to shrink."
native_way: "npm installs dependencies; a bundler resolves imports, transforms TypeScript, tree-shakes (like ProGuard), splits and minifies."
sensei_tip: "A fat JAR is the single bundle.js of the 2010s; code splitting is lazy class loading that you control with dynamic import()."
---

| Java | In the browser |
| --- | --- |
| `pom.xml` dependencies | `package.json` dependencies |
| Maven shade plugin | Bundling |
| ProGuard / R8 shrinking | Tree shaking and minification |
| Lazy class loading | Dynamic `import()` chunks |
