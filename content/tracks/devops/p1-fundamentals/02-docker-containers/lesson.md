# Containers for Operations: Images, Registries and Runtime

Containers became the unit of deployment because they answer the oldest operations question,
"what exactly is running?", with one immutable artefact. This lesson covers what an operator
needs: what a container is, how good images are built, how images move between environments,
and how to look inside a running one. The **Docker & Containers** track goes much deeper.

## A container is a process

There is no "container" object in the Linux kernel. A container is an ordinary process that
the runtime starts with:

- **namespaces**, which give it its own view of process IDs, network interfaces, mounts,
  hostname and users;
- **cgroups**, which limit and account its CPU, memory, I/O and number of processes;
- a **root file system** unpacked from an image, plus dropped capabilities and a seccomp
  filter.

So `ps` on the host shows container processes, a memory limit is a cgroup setting, and a
container "VM-style" breakout is a kernel bug, not a hypervisor one. Containers share the
host kernel: that is why they start in milliseconds, and why isolation is weaker than a VM's.

## Images and layers

An image is a stack of read-only **layers** plus metadata (entrypoint, user, environment,
exposed ports). Each Dockerfile instruction that changes files adds a layer; layers are
content-addressed by digest and shared between images. A good image:

```dockerfile
# syntax=docker/dockerfile:1
FROM golang:1.27-alpine AS build
WORKDIR /src
COPY go.mod go.sum ./
RUN go mod download                      # cached until dependencies change
COPY . .
RUN CGO_ENABLED=0 go build -trimpath -ldflags="-s -w" -o /out/api ./cmd/api

FROM gcr.io/distroless/static-debian13:nonroot
COPY --from=build /out/api /api
USER nonroot:nonroot
EXPOSE 8080
ENTRYPOINT ["/api"]
```

- **multi-stage**: compilers stay in the build stage, the runtime image holds only the binary;
- **cache-friendly order**: what changes least (dependencies) is copied before what changes
  most (source);
- **pinned base images**: an exact tag or, better, a digest (`@sha256:...`), so a rebuild
  gives the same result;
- **non-root user** and a `.dockerignore` that keeps `.git`, secrets and build output out of
  the context.

## Tags, digests and registries

A **tag** (`orders:1.4.2`) is a movable name; a **digest** (`orders@sha256:9f...`) is the
immutable identity of the content. Registries (Docker Hub, GHCR, ECR, Artifact Registry,
Harbor) store images by digest and map tags to them.

Rules that keep operations sane:

- tag every build with the **commit SHA** (and a semver for releases); never deploy `:latest`;
- **build once, promote many**: the image tested in staging is the same digest that goes to
  production, only its configuration changes (environment variables, config maps, secrets);
- keep a retention policy so old tags are cleaned up, but never delete what is deployed;
- scan images for known vulnerabilities and sign them (Cosign) so the cluster can verify
  where they came from (Phase 3).

## Running and debugging

```bash
docker run -d --name api -p 8080:8080 --memory 256m --cpus 1 \
  --read-only --cap-drop ALL ghcr.io/acme/api:3f2c9e1
docker logs -f --since 10m api          # stdout/stderr: containers log to streams, not files
docker exec -it api sh                  # a shell, if the image has one
docker inspect api | jq '.[0].State'    # exit code, OOMKilled, health
docker stats                            # live CPU and memory against limits
docker events --since 1h                # starts, stops, OOM kills
```

A distroless image has no shell: debug it with a sidecar that shares its namespaces, such as
`docker run -it --pid container:api --network container:api busybox`, or `kubectl debug` in
Kubernetes. Configuration comes from the environment and mounted files (twelve-factor), so
the same image runs everywhere.

For local multi-service setups, **Compose** describes services, networks and volumes in one
file; in production, an orchestrator (Kubernetes, ECS, Nomad) does the same across machines.

## Summary

- A container is a process with namespaces, cgroups and an image's file system; it shares the
  host kernel.
- Good images are multi-stage, cache-friendly, pinned, non-root and small.
- Tags move, digests do not: deploy commit-tagged images and promote the same digest through
  environments.
- Debug with logs, exec or a sidecar sharing namespaces, inspect and stats.

## Further reading

- Docker build best practices: <https://docs.docker.com/build/building/best-practices/>
- Multi-stage builds: <https://docs.docker.com/build/building/multi-stage/>
- OCI image specification: <https://github.com/opencontainers/image-spec>
- The Twelve-Factor App: <https://12factor.net/>
- *Container Security*, Liz Rice (O'Reilly, 2020)
