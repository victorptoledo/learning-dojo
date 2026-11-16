---
from: java
status: draft
concept: "Security as a program the teams trust"
from_way: "An annual penetration test and an OWASP Dependency-Check report nobody reads."
native_way: "SCA, SAST (CodeQL, Semgrep, SpotBugs with Find Security Bugs) and secret scanning in every pull request, findings triaged by exploitability, and fix SLAs."
sensei_tip: "Start CI security checks in warning mode, measure their false-positive rate for a few weeks, and only then make the reliable ones (leaked secrets, critical reachable CVEs) block the build."
---

| Java | Program practice |
| --- | --- |
| yearly pen test as the program | continuous checks plus targeted tests |
| all findings are urgent | KEV, EPSS, reachability, exposure |
| security team fixes everything | owners and champions in each team |
| reports with raw counts | time to fix and coverage |
| compliance as the goal | controls first, evidence from them |
