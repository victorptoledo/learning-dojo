---
from: go
status: draft
concept: "One immutable artefact for every environment"
from_way: "A static binary copied to servers with scp."
native_way: "The same static binary sits alone in a distroless or scratch image, tagged with the commit and promoted through environments."
sensei_tip: "CGO_ENABLED=0 and -trimpath make the binary static and reproducible; add ca-certificates and tzdata if you use scratch."
---

| Go world | Containers |
| --- | --- |
| `scp api server:` | `docker push` and a deploy |
| `GOOS=linux` | the image's platform |
| a static binary | a distroless image |
| `-ldflags -X main.version` | an image label and tag |
