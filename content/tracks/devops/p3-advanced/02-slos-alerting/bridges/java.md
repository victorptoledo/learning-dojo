---
from: java
status: draft
concept: "Reliability is a target with a budget"
from_way: "Nagios checks that the JVM process is up and the heap is below 90%, and pages for both."
native_way: "An SLO on successful, fast requests is measured from Micrometer's histograms, and only a fast budget burn pages; heap usage is a dashboard."
sensei_tip: "Micrometer's service-level objectives on Timers (serviceLevelObjectives(Duration.ofMillis(300))) add exactly the bucket a latency SLI needs."
---

| Java world | SRE |
| --- | --- |
| "process up" checks | an availability SLI |
| heap above 90% pages | a dashboard panel |
| a Timer with SLO buckets | a latency SLI |
| an SLA in the contract | a looser bound than the SLO |
