# Networking: IP, Ports, DNS and SSH

Most production problems end with "it cannot connect". Debugging them is mostly a matter of
testing one layer at a time, from your own interface to the remote application.

## The layers you will actually debug

| Layer | Question | Tools |
| --- | --- | --- |
| Link | is the interface up, with a cable or a virtual link? | `ip link` |
| Network (IP) | do I have an address, and a route to the target? | `ip addr`, `ip route`, `ping`, `traceroute` |
| Transport (TCP/UDP) | is something listening on that port, and can I reach it? | `ss`, `nc`, `curl` |
| Names | does the name resolve to the right address? | `getent hosts`, `dig` |
| Application | does it answer correctly (HTTP status, TLS)? | `curl -v`, `openssl s_client` |

## Interfaces, addresses and routes

```text
$ ip -br addr
lo        UNKNOWN  127.0.0.1/8 ::1/128
ens3      UP       10.0.1.25/24 fe80::5054:ff:fe12:3456/64
$ ip route
default via 10.0.1.1 dev ens3 proto dhcp
10.0.1.0/24 dev ens3 proto kernel scope link src 10.0.1.25
```

`10.0.1.25/24` is an address plus a **prefix**: the first 24 bits (`10.0.1`) are the
network, so every address from `10.0.1.0` to `10.0.1.255` is on the same link and reached
directly. Everything else goes to the **default gateway** `10.0.1.1`. A `/16` network has
65,536 addresses, a `/32` exactly one. The private ranges are `10.0.0.0/8`,
`172.16.0.0/12` and `192.168.0.0/16`.

**DHCP** hands out the address, the gateway and the DNS servers when the machine boots.
Servers often use static configuration: on Debian in `/etc/network/interfaces`, with
systemd-networkd, or with NetworkManager; cloud images use cloud-init. `ip` changes are
**temporary** (lost at reboot); write the configuration file to keep them.

`ip neigh` shows the **ARP** table: the hardware (MAC) addresses of neighbours on the link.

## Testing reachability

```bash
ping -c 3 10.0.1.1                 # ICMP echo: is the host there? (often blocked by firewalls)
traceroute -n api.example.com      # each router on the way (mtr: live version)
nc -zv db.internal 5432            # can I open a TCP connection to the port?
curl -v https://api.example.com/health
```

A failed `ping` does not prove the host is down: many networks drop ICMP. A TCP test on the
real port is the better answer. With `curl -v`, learn the three failures apart:

| Symptom | Usually means |
| --- | --- |
| `Connection timed out` | packets dropped: firewall, security group, wrong route |
| `Connection refused` | host reachable, nothing listening on that port (or a REJECT rule) |
| `Could not resolve host` | DNS |
| TLS error | wrong name in the certificate, expired, or a proxy in the middle |
| an HTTP status | the network is fine; it is the application |

## Ports and sockets: ss

```text
$ sudo ss -tlnp
State   Recv-Q  Send-Q  Local Address:Port  Peer Address:Port  Process
LISTEN  0       511     0.0.0.0:80          0.0.0.0:*          users:(("nginx",pid=812,fd=6))
LISTEN  0       4096    127.0.0.1:5432      0.0.0.0:*          users:(("postgres",pid=640,fd=5))
```

`-t` TCP, `-u` UDP, `-l` listening, `-n` numbers (no name lookups), `-p` the process. The
local address matters: `127.0.0.1:5432` accepts connections **only from this machine**,
`0.0.0.0` (or `*`, `[::]`) from any interface. "It works on the server but not from my
laptop" is very often a service bound to `127.0.0.1`.

`ss -tan state established` lists open connections; `ss -s` gives totals. `netstat` is the
old tool with the same ideas. Ports below 1024 need root (or `CAP_NET_BIND_SERVICE`).

## Name resolution

The order is set by `/etc/nsswitch.conf` (`hosts: files dns`): first **`/etc/hosts`**,
then the DNS servers listed in **`/etc/resolv.conf`** (on systems with systemd-resolved it
points to `127.0.0.53`; `resolvectl status` shows the real servers).

```bash
getent hosts api.example.com       # resolves exactly as applications do (hosts file included)
dig +short api.example.com         # asks DNS directly (package bind9-dnsutils)
dig api.example.com @1.1.1.1       # ask a specific server
dig -x 93.184.216.34               # reverse lookup
```

If `getent` and `dig` disagree, look at `/etc/hosts`. Remember that answers are cached for
their **TTL**, so a changed record can take minutes to be seen everywhere.

## Firewalls in one paragraph

Linux filters packets with **netfilter** in the kernel; **nftables** (`nft list ruleset`)
is the modern interface on Debian 13, with `iptables` commands translated to it. Most
people use a front end: `ufw` on Debian and Ubuntu, `firewalld` on Fedora and RHEL. In the
cloud, security groups filter before the packet even reaches the machine. Phase 3's
hardening lesson writes a minimal ruleset.

## SSH: the remote shell

```bash
ssh-keygen -t ed25519 -C "ada@laptop"     # a key pair: ~/.ssh/id_ed25519 (private) and .pub
ssh-copy-id deploy@web1                   # appends the public key to web1:~/.ssh/authorized_keys
ssh deploy@web1                           # log in with the key
ssh web1 'systemctl is-active nginx'      # run one command
```

The private key never leaves your machine; protect it with a passphrase and `ssh-agent`.
`~/.ssh` must be `700` and keys `600`, or ssh refuses them. The first connection asks you to
confirm the server's **host key**; it is then pinned in `~/.ssh/known_hosts`, and a change
later is a warning to take seriously.

`~/.ssh/config` saves typing:

```text
Host web1
    HostName 10.0.1.25
    User deploy
    IdentityFile ~/.ssh/id_ed25519
Host db1
    HostName 10.0.2.10
    ProxyJump web1            # go through web1 (a bastion) to reach db1
```

Copying files and tunnels:

```bash
scp app.tar.gz web1:/tmp/                  # copy one file
rsync -avz --delete site/ web1:/srv/site/  # sync a tree, only the differences
ssh -L 5433:localhost:5432 db1             # local port 5433 -> db1's local PostgreSQL
```

## Summary

- Debug layer by layer: link, IP and routes, TCP port, DNS, application.
- `ip addr`, `ip route`: the prefix decides what is local; the rest goes to the default gateway.
- `ss -tlnp` shows what listens where; `127.0.0.1` means local only.
- `getent hosts` resolves like applications do; `dig` asks DNS directly.
- Timeout means dropped, refused means nothing listening.
- SSH with ed25519 keys, `~/.ssh/config`, `ProxyJump`, `rsync` and `-L` tunnels.

## Further reading

- `ip(8)`: <https://man7.org/linux/man-pages/man8/ip.8.html> and `ss(8)`: <https://man7.org/linux/man-pages/man8/ss.8.html>
- Arch Wiki, "Network configuration": <https://wiki.archlinux.org/title/Network_configuration>
- Arch Wiki, "OpenSSH": <https://wiki.archlinux.org/title/OpenSSH>
- Debian Reference, "Network setup": <https://www.debian.org/doc/manuals/debian-reference/>
