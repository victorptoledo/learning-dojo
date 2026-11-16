---
from: python
status: draft
concept: "Small, reversible changes; practised incidents; blameless learning"
from_way: "SSH into the server and fix it live."
native_way: "No SSH: changes go through pipelines, operations through Systems Manager runbooks, incidents through a defined process."
sensei_tip: "Write operational scripts as SSM Automation runbooks (they can run Python steps), so fixes are reviewed, repeatable and logged instead of typed into a shell."
---

| Python world | AWS |
| --- | --- |
| SSH and `vim` on the server | Session Manager, then a pipeline fix |
| a fabfile of ops tasks | SSM Automation documents |
| feature flags in settings | AppConfig feature flags |
| a post-mortem email | a blameless review with tracked actions |
