---
from: java
status: draft
concept: "Automated regression hunting"
from_way: "Reading the release notes and guessing, or attaching a debugger to the new version only."
native_way: "git bisect run with a script that builds with Maven and runs the one test that reproduces the bug."
sensei_tip: "A good bisect script for Maven: mvn -q -o -DskipTests package || exit 125, then mvn -q -o test -Dtest=CheckoutTotalsTest. Offline (-o) keeps every step fast and independent of the network."
---

| Java habit | With bisect |
| --- | --- |
| "it broke somewhere in 2.4" | `git bisect start v2.4.0 v2.3.0` |
| a JUnit test reproducing the bug | the body of the `bisect run` script |
| the module does not compile at some commit | `exit 125` |
| a new regression test | written after bisect names the culprit |
