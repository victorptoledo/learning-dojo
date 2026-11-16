---
from: java
status: draft
concept: "The pipeline is a build file for the whole delivery"
from_way: "A Jenkins freestyle job, configured in the web UI, runs mvn clean install on a shared agent."
native_way: "A workflow file in the repository runs mvn -B verify on a fresh runner, with the Maven repository cached by the pom's hash."
sensei_tip: "Use mvn -B -ntp (batch mode, no transfer progress) in CI, and cache ~/.m2/repository with setup-java's cache: maven."
---

| Java world | CI pipeline |
| --- | --- |
| `pom.xml` lifecycle phases | jobs and steps |
| a Jenkins agent | a runner |
| `~/.m2/repository` | a cache keyed by the pom |
| Surefire reports | test reports uploaded as artefacts |
