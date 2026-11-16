---
from: python
status: draft
concept: "Security as a program the teams trust"
from_way: "bandit run once locally and a requirements.txt nobody audits."
native_way: "bandit or Semgrep, pip-audit and secret scanning in CI, with owners for each finding and SLAs by priority."
sensei_tip: "Semgrep and bandit both support baselines: record today's findings, block only new ones, and burn down the old backlog by priority instead of all at once."
---

| Python | Program practice |
| --- | --- |
| bandit once, locally | SAST in every pull request |
| no dependency audit | pip-audit with a fix SLA |
| a 1000-item backlog | a baseline and new-findings-only gate |
| security training slides | training from your own findings |
| no disclosure policy | security.txt and a response process |
