---
from: java
status: draft
concept: "Least privilege"
from_way: "Running the application inside the container as the default `root` user."
native_way: "Create a system user (`USER nonroot`) and remove shells (`/bin/sh`) and package managers from the final image."
sensei_tip: "If an attacker gets remote code execution in a distroless, non-root container, there isn't even a shell to run commands in."
---
