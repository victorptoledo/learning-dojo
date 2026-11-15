---
from: go
status: draft
concept: "A declarative build recipe"
from_way: "go build reads go.mod and the sources; flags and environment decide the output, and a Makefile often wraps it."
native_way: "The Dockerfile pins the toolchain image, copies go.mod and the sources, runs go build, and declares the binary as the exec-form ENTRYPOINT."
sensei_tip: "flag defaults map naturally to CMD: ENTRYPOINT [\"/app/server\"] and CMD [\"-port\", \"8080\"], so docker run IMAGE -port 9090 just works."
---

| Go | Dockerfile |
| --- | --- |
| `go build -o server .` | `RUN go build -o /out/server .` |
| `flag` defaults | `CMD [...]` |
| `os.Getenv("PORT")` | `ENV PORT=8080` |
| the binary in `main` | `ENTRYPOINT ["/app/server"]` |
