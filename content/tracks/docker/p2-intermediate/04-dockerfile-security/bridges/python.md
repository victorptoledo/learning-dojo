---
from: python
status: draft
concept: "Least privilege and a minimal attack surface"
from_way: "Running gunicorn under a dedicated system user behind nginx, with the code owned by root."
native_way: "Do the same in the image: create a system user, COPY the code as root, chown only the writable directories, and set USER before the CMD."
sensei_tip: "Python images include pip; delete nothing by hand, but use a distroless python image or a slim runtime stage where pip and compilers are absent if you want a smaller attack surface."
---

| Python | Image hardening |
| --- | --- |
| `useradd -r app` on the server | `RUN useradd --system --uid 10001 app` then `USER 10001` |
| code owned by root, data by app | `COPY` as root, `chown` only the data directory |
| `/healthz` endpoint | `HEALTHCHECK CMD ["python", "-c", "..."]` without curl |
| `pip-audit` | image scanning (Phase 3) |
