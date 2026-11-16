---
from: python
status: draft
concept: "Prepared, evidence-first, blameless"
from_way: "SSHing into the box, killing suspicious processes and deleting odd files."
native_way: "Collect process lists, network connections, logs and a disk snapshot first, isolate, then rebuild from the image and rotate secrets."
sensei_tip: "If you script evidence collection in Python, write the output to a separate mounted volume or remote store with hashes (`hashlib.sha256`) of each file, never to the compromised disk."
---

| Python | Incident-ready practice |
| --- | --- |
| kill -9 the odd process | record ps, ss, lsof output first |
| delete the dropped files | hash and preserve them |
| pip reinstall on the host | rebuild from a clean image |
| timeline in someone's head | a scribe with UTC times |
| tell nobody until it is fixed | early legal and privacy involvement |
