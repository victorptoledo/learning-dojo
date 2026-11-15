---
from: javascript
status: draft
concept: "Getting started with Java 25"
from_way: "Node runs your files directly, npm manages packages, and TypeScript adds types through a compile step."
native_way: "javac compiles, the JVM runs, and Maven or Gradle play npm's role."
sensei_tip: "Java's type checks are like TypeScript in strict mode, but they are kept at runtime too, and the code is compiled to bytecode that the JIT optimises while it runs."
---

| JavaScript / TypeScript | Java 25 |
| --- | --- |
| node app.js | java App.java |
| tsc | javac |
| node REPL | jshell |
| package.json | pom.xml |
| npm install | mvn dependency:resolve, or the build does it |
