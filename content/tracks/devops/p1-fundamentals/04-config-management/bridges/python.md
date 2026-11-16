---
from: python
status: draft
concept: "Declare the state, let a tool converge to it"
from_way: "A Fabric script runs commands over SSH in order, and breaks if run twice."
native_way: "Ansible (itself written in Python) declares resources whose modules check before they change, so runs are repeatable."
sensei_tip: "Custom Ansible modules are Python; return changed=False when nothing was done, or every run will look like a change."
---

| Python world | Configuration management |
| --- | --- |
| Fabric `run(...)` | an idempotent module |
| a `settings.py` per server | templates and variables |
| `if not os.path.exists` | `creates:` on a command task |
| Jinja2 templates | the same Jinja2, in Ansible |
