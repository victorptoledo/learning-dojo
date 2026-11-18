Before the image goes to the registry, the security team runs its release audit. A scanner
answers "which packages are in it and which of them have known holes"; with no shell, no
package manager and one static binary, there is almost nothing left to find. The audit also
checks what a scanner cannot see. This stage added `.env.release` (the registry token) and
`certs/dev-tls.key` to the folder.

1. **Pinned bases.** Every `FROM` that pulls an image names it by digest, so a rebuild next
   month uses the same bytes: `FROM golang:1.27-alpine@sha256:<digest> AS build`. Get the
   digest with `docker buildx imagetools inspect golang:1.27-alpine` (the `Digest:` line).
   `scratch` and references to an earlier stage need none. Keep the tag: it tells people
   what the digest is.
2. **Labels.** In the final stage: `org.opencontainers.image.title="inventory"`,
   `version` from the same `VERSION` argument as the binary, `revision` from a new
   `ARG REVISION` (a commit id; any default, such as `unknown`), `source` (an `https://` URL)
   and `licenses` (an SPDX id such as `MIT`).
3. **No secrets.** The registry token belongs to the release script, never to the build: no
   `ARG` or `ENV` with a token or password, and nothing of `.env.release` in a build step.
4. **Context.** The `.dockerignore` also keeps `.env*` and `certs/` (or `*.key`) out.

The dojo reads every `FROM` line, the labels and the configuration, looks for the token in
the history and for key files in the image, and runs every check of the earlier stages.

> [!TIP]
> To scan the image yourself: `docker scout cves <image>` (Docker Desktop) or
> `trivy image <image>`, and `docker buildx build --sbom=true --provenance=true` to attach
> an SBOM and the build provenance when you push.
