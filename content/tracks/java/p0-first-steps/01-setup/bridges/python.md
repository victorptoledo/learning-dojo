---
from: python
status: draft
concept: "Getting started with Java 25"
from_way: "An interpreter you run directly (python app.py), a REPL, and pip with venv for libraries."
native_way: "A compiler and a JVM: java App.java runs a single file, jshell is the REPL, and Maven or Gradle manage builds and libraries."
sensei_tip: "Since Java 25 a compact source file with void main() { ... } runs with java Hello.java, as close to a Python script as Java gets. Use jshell to try things like you use the Python REPL."
---

| Python | Java 25 |
| --- | --- |
| python app.py | java App.java |
| python (REPL) | jshell |
| pip + venv | Maven or Gradle |
| requirements.txt | pom.xml dependencies |
| pyenv | SDKMAN! or the JDK installer |
