# Images, Layers, Tags and Digests

An image looks like one big file, but it is a small set of JSON documents pointing at a
stack of compressed tarballs. Understanding that stack is what makes images fast to share,
cheap to store and, when you get it wrong, surprisingly large.

## What an image is made of

The **OCI image specification** defines three kinds of objects, each stored and fetched by
its **digest** (the SHA-256 of its bytes):

| Object | Contains |
| --- | --- |
| **Layers** | tar archives of file system changes: files added, changed or removed |
| **Config** | JSON: the start command (`Entrypoint`, `Cmd`), `Env`, `User`, `WorkingDir`, `ExposedPorts`, labels, the list of layer digests, the build history |
| **Manifest** | JSON: the digest of the config and of every layer, in order |

For images built for several platforms there is one more level, the **index**: a list of
manifests, one per OS and CPU architecture. `docker pull` picks the one for your machine.

Because everything is content-addressed, two images that share a base share its layers on
disk and over the network: pulling `myapp:2` after `myapp:1` downloads only the layers that
changed.

## Layers and copy-on-write

Each instruction in a Dockerfile that changes files (`RUN`, `COPY`, `ADD`) produces one
layer. When a container starts, the storage driver (OverlayFS) stacks them read-only and
adds one **writable layer** for the container:

```text
container layer   (read-write, deleted with the container)
layer 3  COPY app /app
layer 2  RUN apk add curl
layer 1  alpine base
```

Reading a file finds it in the topmost layer that has it. Changing a file **copies it up**
into the writable layer first (copy-on-write). Deleting a file from a lower layer writes a
**whiteout** entry that hides it; the bytes stay in the lower layer.

That has a direct consequence:

```dockerfile
RUN wget https://example.com/big.tar.gz      # layer: +400 MB
RUN tar xzf big.tar.gz                        # layer: +500 MB
RUN rm big.tar.gz                             # layer: a whiteout, the 400 MB remain
```

The image ships all 900 MB. Download, unpack and delete in **one** `RUN`, or build in one
stage and copy only the result into another (Phase 2).

Instructions such as `ENV`, `EXPOSE`, `LABEL`, `CMD` and `USER` change only the config, so
they add an empty history entry, not a layer.

## Looking inside

```powershell
docker image inspect alpine:3.24 --format '{{json .Config}}'   # the config
docker image inspect alpine:3.24 --format '{{json .RootFS.Layers}}'
docker history --no-trunc myapp:1.0                             # one line per step, with size
docker image ls --digests
```

`docker history` shows which step produced how many bytes, which is the first tool to reach
for when an image is too big. Tools such as `dive` show what each layer adds file by file.

## Tags and digests

A full image reference is `registry/repository:tag@digest`:

```text
docker.io/library/postgres:18-alpine
ghcr.io/acme/orders-api:1.4.0
ghcr.io/acme/orders-api@sha256:9b2f...e1
```

- A **tag** is a human-friendly **pointer** that the publisher can move. `postgres:18-alpine`
  today is not the same bytes as next month, after a patch release. `latest` is just the
  default tag name, not "the newest".
- A **digest** identifies exact content and can never change. Pulling by digest gives you
  the same bytes every time.

Use tags to choose a version and digests when you need to be sure: pin base images by digest
in critical builds, and record the digest of every image you deploy.

## Where images live locally

The engine keeps images and layers in its own store (inside Docker Desktop's VM), not as
files you manage. `docker save` and `docker load` move images as tar files, `docker image
prune` removes dangling images (untagged leftovers of rebuilds), and `docker system df`
shows the disk used by images, containers, volumes and the build cache.

## Summary

- An image is a manifest pointing at a config and an ordered list of layers, all by digest.
- Every file-changing instruction adds a layer; later deletions only hide files.
- Containers add a writable copy-on-write layer that disappears with them.
- A tag is a movable name; a digest is the exact content.
- `docker history` and `docker image inspect` show how an image was built and configured.

## Further reading

- Understanding image layers (Docker docs): <https://docs.docker.com/get-started/docker-concepts/building-images/understanding-image-layers/>
- OCI image specification: <https://github.com/opencontainers/image-spec>
- Storage drivers and OverlayFS: <https://docs.docker.com/engine/storage/drivers/>
- `docker image tag` reference: <https://docs.docker.com/reference/cli/docker/image/tag/>
