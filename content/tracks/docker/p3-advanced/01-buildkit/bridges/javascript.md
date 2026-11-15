---
from: javascript
status: draft
concept: "Build caches, build secrets and target platforms"
from_way: ".npmrc holds an auth token for a private registry, and the npm cache speeds up installs."
native_way: "Mount the npm cache with type=cache and the .npmrc (or the token) with type=secret, so neither ends up in a layer."
sensei_tip: "Never COPY .npmrc into an image, not even in a build stage you think is temporary: put it in .dockerignore and use a secret mount."
---

| JavaScript | BuildKit |
| --- | --- |
| `~/.npm` cache | `--mount=type=cache,target=/root/.npm` |
| `.npmrc` with `_authToken` | `--mount=type=secret,id=npmrc,target=/root/.npmrc` |
| native modules (sharp, bcrypt) | built per platform with `buildx --platform` |
| `process.env.npm_package_version` | `ARG VERSION` and a label |
