---
from: java
status: draft
concept: "Toolchain, build file and dependency lock"
from_way: "SDKMAN or a JDK installer, Maven or Gradle with pom.xml or build.gradle, and dependencies downloaded into ~/.m2, shared by every project."
native_way: "uv installs the interpreter, pyproject.toml is the build file, each project gets its own .venv and uv.lock pins every transitive version."
sensei_tip: "Think of pyproject.toml as pom.xml and uv as Maven plus SDKMAN. The big difference: Python installs dependencies per project (.venv), not into one shared repository, so two projects never fight over versions."
---

| Java | Python with uv |
| --- | --- |
| `sdk install java 25` | `uv python install 3.15` |
| `pom.xml` / `build.gradle` | `pyproject.toml` |
| `<scope>test</scope>` | `[dependency-groups] dev = [...]` (`uv add --dev`) |
| `~/.m2/repository` (shared) | `.venv` per project |
| Gradle lockfile / Maven enforcer | `uv.lock` (always on) |
| `mvn test` | `uv run pytest` |
| `jshell` | `uv run python` (the REPL) |
| `public static void main` | `if __name__ == "__main__":` |
