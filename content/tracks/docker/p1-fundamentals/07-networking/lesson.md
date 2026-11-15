# Networks, DNS and Published Ports

Every container gets its own network namespace: its own interfaces, IP address, routing table
and, importantly, its own **`localhost`**. Networking in Docker is about deciding which of
those namespaces can reach which, and what the outside world can reach.

## Network drivers

| Driver | What the container gets | Use it for |
| --- | --- | --- |
| `bridge` (default) | a private IP on a virtual switch on the host, outbound access through NAT | almost everything on one host |
| `host` | no isolation: the host's own interfaces and ports | special cases needing raw network performance (Linux hosts) |
| `none` | only a loopback interface | jobs that must have no network (the dojo's checks) |
| `overlay` | a network spanning several hosts | Swarm clusters |
| `macvlan`, `ipvlan` | an address on the physical LAN | legacy integrations |

## User-defined bridge networks and DNS

Containers started without `--network` join the **default bridge**, where they can reach
each other only by IP address. Create your own network instead:

```powershell
docker network create shop
docker run -d --name db  --network shop -e POSTGRES_PASSWORD=dev postgres:18-alpine
docker run -d --name api --network shop -e DATABASE_HOST=db -p 8080:8080 shop-api:1.0
docker run --rm --network shop alpine:3.24 nslookup db
```

On a user-defined network Docker runs an **embedded DNS server** (at `127.0.0.11` inside
each container) that resolves **container names** and **aliases** (`--network-alias`) to
their current IP addresses. That is why `DATABASE_HOST=db` works, and keeps working when the
database container is recreated with a new IP. Containers on different networks cannot talk
to each other unless one is connected to both (`docker network connect`), which makes
networks a simple security boundary. A network created with `--internal` has no route to
the outside world.

## localhost is the container

A classic mistake: the API uses `localhost:5432` for its database. Inside the API's container,
`localhost` is the **API container itself**. Use the database's name on a shared network.

From a container to a service on your **host** machine, Docker Desktop provides the name
`host.docker.internal`.

## Publishing ports

Nothing inside a bridge network is reachable from outside the host until you **publish** a
port:

```powershell
docker run -d -p 8080:80 nginx:1.29-alpine            # host 0.0.0.0:8080 -> container 80
docker run -d -p 127.0.0.1:8080:80 nginx:1.29-alpine  # only from this machine
docker run -d -P nginx:1.29-alpine                     # every EXPOSEd port to a random host port
docker port <container>                                # what is published where
```

- `EXPOSE` in a Dockerfile is documentation (and what `-P` uses); it publishes nothing.
- `-p 8080:80` listens on **every** interface of the host by default. For a database or an
  admin port, bind to `127.0.0.1` or do not publish at all: containers on the same network
  reach it without `-p`. On Linux hosts, published ports also bypass many host firewall
  rules, because Docker manages its own iptables or nftables rules.
- The program inside must listen on `0.0.0.0` (all interfaces of the container). A server
  bound to `127.0.0.1` answers `docker exec ... curl localhost` but not a published port,
  because published traffic arrives on the container's `eth0`.

## Debugging connectivity

```powershell
docker network ls
docker network inspect shop                       # subnet and connected containers
docker inspect api --format '{{json .NetworkSettings.Networks}}'
docker run --rm -it --network shop nicolaka/netshoot   # curl, dig, nc, tcpdump, ss ...
docker run --rm -it --network container:api nicolaka/netshoot ss -ltn   # inside api's namespace
```

Joining another container's network namespace (`--network container:NAME`) lets a tool
container see exactly what the app sees, without installing tools in the app image.

## Summary

- Each container has its own network namespace; `localhost` is the container itself.
- Use user-defined bridge networks: they have DNS by container name and isolate groups.
- `--internal` networks have no outbound route; `none` has no network at all.
- `EXPOSE` documents; `-p` publishes, on all interfaces unless you give an address.
- Servers must listen on `0.0.0.0` inside the container; databases rarely need `-p`.
- Debug with `network inspect` and a tool container on the same network or namespace.

## Further reading

- Networking overview: <https://docs.docker.com/engine/network/>
- Bridge network driver: <https://docs.docker.com/engine/network/drivers/bridge/>
- Networking with standalone containers (tutorial): <https://docs.docker.com/engine/network/tutorials/standalone/>
