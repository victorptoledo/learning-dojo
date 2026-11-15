---
from: javascript
status: draft
concept: "Persistent state outside the process"
from_way: "A dev server watches your source folder, and uploads land in a local public/uploads folder."
native_way: "Bind-mount the source only for development; in production, state goes to a volume or object storage and the image holds the built code."
sensei_tip: "When bind-mounting a project, add an anonymous volume for /app/node_modules so the container keeps its Linux modules and the host folder does not hide them."
---

| JavaScript | Container storage |
| --- | --- |
| `vite` or `nodemon` watching files | a bind mount of the source in development |
| `fs.writeFile("uploads/...")` | a named volume, or S3-style storage |
| `node_modules` | inside the image, never bind-mounted from Windows |
| `os.tmpdir()` | a tmpfs mount |
