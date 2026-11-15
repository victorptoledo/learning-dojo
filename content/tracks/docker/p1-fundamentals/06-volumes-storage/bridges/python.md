---
from: python
status: draft
concept: "Persistent state outside the process"
from_way: "Scripts write to a local folder or a SQLite file next to the code, and it is still there tomorrow."
native_way: "Anything a container writes outside a mount is lost on docker rm; put SQLite files and uploads on a named volume and mount the code only in development."
sensei_tip: "With a bind-mounted project in development, keep the virtualenv inside the image (not in the mounted folder) so Windows and Linux packages never mix."
---

| Python | Container storage |
| --- | --- |
| `sqlite3.connect("app.db")` | `app.db` on a volume, such as `/data/app.db` |
| `tempfile.mkdtemp()` | a tmpfs mount on `/tmp` |
| editing code with auto-reload | a bind mount in development only |
| `shutil.make_archive` for backups | a temporary container running `tar` on the volume |
