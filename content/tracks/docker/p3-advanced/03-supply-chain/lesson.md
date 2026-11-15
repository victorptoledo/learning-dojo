# The Image Supply Chain: Size, Scanning, SBOMs, Registries, Tags and Signing

An image you deploy contains your code plus hundreds of packages written by other people,
built by a machine you hope was not tampered with, stored in a registry and pulled by name.
Each of those steps is part of your **supply chain**, and each can go wrong.

## Size: find what is in there

Large images pull slowly, start slowly on new nodes, cost storage and, above all, carry more
packages to patch. To see where the bytes are:

```powershell
docker image ls shop                     # total size
docker history --no-trunc shop:1.4.0     # bytes per build step
docker scout quickview shop:1.4.0        # base image, packages and vulnerabilities
```

Tools such as `dive` show what each layer adds, file by file. The usual culprits: a full base
image instead of `-slim` or distroless, build tools in the final stage, package caches,
files deleted in a later layer, and a context copied with `COPY . .` without `.dockerignore`.

## Vulnerability scanning

A scanner reads the packages in an image (OS packages and language dependencies) and matches
their versions against vulnerability databases (CVE, GitHub advisories, distribution
trackers):

```powershell
docker scout cves shop:1.4.0
trivy image --severity HIGH,CRITICAL --exit-code 1 shop:1.4.0
grype shop:1.4.0
```

How to use the results:

- Scan **in CI** and fail on a threshold you can live with (for example, fixable HIGH and
  CRITICAL).
- **Rescan** images already deployed: new CVEs are published every day against packages
  that did not change.
- Most findings come from the **base image**. Rebuilding on an updated base, or moving to a
  smaller one, fixes more than any code change. Automate base updates (Renovate, Dependabot)
  and rebuild regularly even without code changes.
- Not every CVE is exploitable in your context; record accepted risks (for example in a
  `.trivyignore` with a reason and an expiry) instead of ignoring the report.

Scanners also find **secrets** and **misconfigurations** (`trivy config` on Dockerfiles and
Compose files), complementing hadolint.

## SBOMs and provenance

- A **Software Bill of Materials** (SBOM) lists every component in the image with its
  version and licence, in a standard format (SPDX or CycloneDX). With it you can answer "are
  we affected?" in minutes when the next big vulnerability lands. Generate one with
  `docker buildx build --sbom=true`, `docker scout sbom` or `syft`.
- **Provenance** attestations (SLSA) record how an image was built: from which source
  commit, by which builder, with which parameters. `docker buildx build --provenance=mode=max`
  attaches one to the image in the registry.

## Registries

A **registry** stores images under repositories: Docker Hub, GitHub Container Registry
(`ghcr.io`), cloud registries (ECR, Artifact Registry, ACR), or self-hosted (Harbor). They
all implement the OCI distribution specification.

```powershell
docker login ghcr.io
docker tag shop:dev ghcr.io/acme/shop:1.4.0
docker push ghcr.io/acme/shop:1.4.0
docker pull ghcr.io/acme/shop@sha256:4c1e...
```

Things to configure on a registry: who may push (CI only), retention rules for old tags,
immutable tags where the registry supports them, vulnerability scanning on push, and a pull
through cache or mirror for public images, so builds survive rate limits and outages.

## A tagging strategy

| Tag | Example | Moves? | Use |
| --- | --- | --- | --- |
| exact version | `1.4.0` | never (enforce it) | releases |
| git commit | `sha-3f9c2a1` | never | traceability from any build |
| minor / major | `1.4`, `1` | yes | consumers who accept patches |
| branch / `latest` | `main`, `latest` | yes | development convenience only |

Deploy with an immutable reference. Best is the **digest**: `ghcr.io/acme/shop@sha256:...`
(or `1.4.0@sha256:...`, which keeps the readable tag and pins the bytes). Then a rollback is
"deploy the previous digest", and what runs is exactly what was tested.

## Signing and verifying

A signature proves an image was produced by someone holding a key (or, with keyless
signing, by a specific CI identity), and that it was not changed since:

```powershell
cosign sign ghcr.io/acme/shop@sha256:4c1e...
cosign verify --certificate-identity-regexp 'https://github.com/acme/.*' `
  --certificate-oidc-issuer https://token.actions.githubusercontent.com ghcr.io/acme/shop@sha256:4c1e...
```

Sigstore's `cosign` and Notary's `notation` are the common tools; signatures and SBOMs are
stored next to the image in the registry. The value comes from **verifying**: an admission
controller in Kubernetes (Kyverno, Sigstore policy-controller) refuses images that are not
signed by your pipeline.

## Summary

- Measure size with `docker history` and Scout; base images and build tools are the usual
  causes.
- Scan in CI with a threshold, rescan what is deployed, and rebuild on fresh bases.
- SBOMs list what is inside; provenance records how it was built.
- Push immutable version and commit tags; deploy by digest.
- Sign in CI and verify at deployment time.

## Further reading

- Docker Scout: <https://docs.docker.com/scout/>
- Trivy: <https://trivy.dev/>
- Grype and Syft (Anchore): <https://github.com/anchore/grype>, <https://github.com/anchore/syft>
- SBOM attestations with BuildKit: <https://docs.docker.com/build/attestations/sbom/>
- OCI distribution specification: <https://github.com/opencontainers/distribution-spec>
- Sigstore cosign: <https://github.com/sigstore/cosign>
- Semantic Versioning: <https://semver.org/>
