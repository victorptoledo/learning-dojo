---
from: python
status: draft
concept: "Layered, content-addressed artifacts"
from_way: "A wheel or a requirements lock pins versions, and pip caches what it downloaded, but a deployment usually reinstalls everything."
native_way: "Each Dockerfile step is a cached, content-addressed layer: installed packages in one layer, your code in a later one, and only changed layers are rebuilt and pulled."
sensei_tip: "pip leaves its download cache in the layer unless you pass --no-cache-dir, which is a classic source of extra megabytes."
---

| Python | Image |
| --- | --- |
| `pip install` into site-packages | a layer with the installed packages |
| `requirements.txt` with hashes | a digest pins the exact image bytes |
| deleting files to save space after install | only helps inside the same `RUN` step |
| PyPI | a registry |
