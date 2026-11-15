---
from: java
status: draft
concept: "Persistent state outside the process"
from_way: "An application server writes logs, uploads and an embedded H2 file to the local disk, which survives restarts because the server is long-lived."
native_way: "A container is disposable, so state goes to a named volume (or a database), configuration comes in read-only, and logs go to stdout."
sensei_tip: "Spring Boot's spring.servlet.multipart.location and java.io.tmpdir default to /tmp: on a read-only container mount a tmpfs there."
---

| Java | Container storage |
| --- | --- |
| `Files.write(Path.of("/data/..."))` | a named volume mounted at `/data` |
| `application.yml` next to the JAR | a read-only bind mount or a config file in the image |
| `java.io.tmpdir` | a tmpfs mount |
| H2 file database | a volume, or better a Postgres container with one |
