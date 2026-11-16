---
from: python
status: draft
concept: "One immutable artefact for every environment"
from_way: "A virtualenv on each server, created with pip install -r requirements.txt on deploy day."
native_way: "Dependencies are installed once, in the image build, from a lock file; every environment runs that image."
sensei_tip: "Install wheels in a builder stage and copy the virtualenv into a python:3.x-slim stage; never ship gcc to production."
---

| Python world | Containers |
| --- | --- |
| a virtualenv per server | a layer in the image |
| `requirements.txt` | a lock file installed at build time |
| `python app.py` | ENTRYPOINT with gunicorn or uvicorn |
| PyPI | a registry for images (and a mirror for wheels) |
