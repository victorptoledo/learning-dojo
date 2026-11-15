# Containers, Docker Desktop and Your First Container

"It works on my machine" is the oldest excuse in software. A service needs a language
runtime of the right version, system libraries, configuration files and environment
variables, and every machine it runs on has a slightly different set. A **container**
packages the program together with everything it needs, so it runs the same on a laptop, in
CI and on a server.

## Containers and virtual machines

A **virtual machine** emulates hardware: each VM boots its own kernel and operating system,
takes gigabytes of disk and starts in tens of seconds.

A **container** is an ordinary process on the host, started with a restricted view of the
system: its own file system, process list, network interfaces and resource limits. All
containers on a host **share the host's kernel**.

| | Virtual machine | Container |
| --- | --- | --- |
| Isolates with | a hypervisor and virtual hardware | kernel features (namespaces, cgroups) |
| Contains | a full OS with its own kernel | the program and its user-space files |
| Size | gigabytes | megabytes |
| Starts in | tens of seconds | well under a second |
| Isolation | strong (separate kernel) | good, but a kernel bug affects every container |

Phase 1 opens the box and shows the kernel features a container is made of.

## The parts of Docker

- **Image**: a read-only template, a stack of file system layers plus configuration (which
  command to run, environment variables, the user). Images have names and tags such as
  `alpine:3.24`.
- **Container**: a running (or stopped) instance of an image, with a thin writable layer on
  top. Many containers can run from one image.
- **Registry**: a server that stores images. Docker Hub is the default; GitHub (`ghcr.io`),
  cloud providers and companies run others.
- **Docker Engine** (`dockerd`): the daemon that builds images and runs containers. It uses
  `containerd` and `runc` underneath (Phase 4).
- **Docker CLI** (`docker`): the client you type commands into. It talks to the engine over
  an API, locally through a socket.

The image and container formats are open standards from the **Open Container Initiative**
(OCI), so Podman, containerd and Kubernetes run the same images.

## Docker Desktop on Windows

Containers need a Linux kernel. **Docker Desktop** runs one for you in a small virtual
machine on **WSL 2** and puts the `docker` CLI on your Windows `PATH`:

```powershell
winget install Docker.DockerDesktop   # then start Docker Desktop and accept the WSL 2 backend
docker version                        # shows both Client and Server: the engine is up
docker run --rm hello-world           # pulls a tiny image and prints a greeting
```

If `docker version` shows only the client and an error such as *cannot connect to the
Docker daemon*, Docker Desktop is not running yet.

## Your first containers

```powershell
docker run -it --rm alpine:3.24 sh     # an interactive shell in a fresh Alpine container
/ # cat /etc/os-release                 # Alpine, whatever your host is
/ # exit

docker run -d --name web -p 8080:80 nginx:1.29-alpine   # a web server in the background
docker ps                               # running containers
docker logs web                         # what it printed
docker stop web; docker rm web          # stop it, then delete it
docker images                           # images stored locally
```

| Flag | Meaning |
| --- | --- |
| `-it` | interactive with a terminal: keep stdin open (`-i`) and allocate a TTY (`-t`) |
| `--rm` | delete the container when its main process exits |
| `-d` | detached: run in the background |
| `--name web` | a name instead of a random one |
| `-p 8080:80` | publish container port 80 on port 8080 of the host |

What happens on `docker run alpine:3.24 sh`: the CLI asks the engine to run it; the engine
looks for `alpine:3.24` locally and **pulls** it from the registry if missing; it
**creates** a container (a writable layer and a configuration), then **starts** it, which
runs `sh` as the container's first process. When that process exits, the container stops.

## How the dojo checks Docker katas

In Docker katas you write a `Dockerfile` (and sometimes a `compose.yaml`). When you save, the
dojo:

1. builds your image on your own Docker engine with `docker build --network none` (base
   images are pulled once; build steps cannot download anything), under a throwaway tag;
2. inspects it and starts it once, with no network, no Linux capabilities and small limits;
3. runs **hadolint** (a Dockerfile linter) and **bats** tests in the `dojo-docker` image over
   what it saw: the image configuration, the files inside, what the container printed, and
   HTTP answers when it is a server;
4. removes the container and the image. The build cache stays, so re-checks are fast.

Nothing gets the Docker socket and nothing runs privileged. Run `dojo runner build` once to
build `dojo-docker`.

> [!TIP]
> Images accumulate. `docker system df` shows how much disk they use and
> `docker system prune` removes stopped containers, unused networks and dangling images.

## Summary

- A container is an isolated process sharing the host kernel; a VM has its own kernel.
- An image is a read-only template; a container is an instance with a writable layer.
- Registries store images; the CLI talks to the engine, which pulls, creates and starts.
- On Windows, Docker Desktop runs the engine in a WSL 2 virtual machine.
- `docker run -it --rm IMAGE sh` for experiments; `-d`, `-p`, `logs`, `stop`, `rm` for services.

## Further reading

- Docker overview (architecture, objects): <https://docs.docker.com/get-started/docker-overview/>
- What is a container?: <https://docs.docker.com/get-started/docker-concepts/the-basics/what-is-a-container/>
- Install Docker Desktop on Windows: <https://docs.docker.com/desktop/setup/install/windows-install/>
- Docker Desktop and WSL 2: <https://docs.docker.com/desktop/features/wsl/>
- The Docker roadmap on roadmap.sh (topic map): <https://roadmap.sh/docker>
