# Networking Fundamentals: Layers, TCP/IP and DNS

Calling `fetch("https://api.example.com/users")` looks like a function call. Underneath, a name is
resolved to an address, packets are routed through a dozen machines owned by different companies,
some of them are lost, reordered or duplicated, and two programs on opposite sides of the planet
still end up with an exact, ordered copy of each other's bytes. This lesson explains how.

## Layers and encapsulation

Networking is built in layers; each uses the one below and offers a service to the one above.
The **TCP/IP model** (the OSI model splits it into seven, but the internet uses these four):

| Layer | Unit | Job | Examples |
| --- | --- | --- | --- |
| Application | message | what the programs say | HTTP, DNS, TLS, SMTP, gRPC |
| Transport | segment / datagram | process to process, ports, reliability | TCP, UDP, QUIC |
| Internet | packet | host to host across networks, routing | IPv4, IPv6, ICMP |
| Link | frame | one hop on one physical network | Ethernet, Wi-Fi |

Sending wraps the data in one header per layer (**encapsulation**); receiving unwraps it:

```text
[ Ethernet | IP | TCP | HTTP request ............ ] [ FCS ]
   14 B      20 B  20 B
```

A **router** looks only up to the IP header; a **switch** only at Ethernet; the two **end hosts**
handle every layer. This is the **end-to-end principle**: keep the network simple and put
reliability in the endpoints.

## The link layer

Ethernet and Wi-Fi deliver **frames** between machines on the same local network, addressed by
48-bit **MAC addresses** (`3c:22:fb:12:9a:01`). A frame carries at most about 1,500 bytes of
payload (the **MTU**). To find the MAC address of a local IP address, hosts use **ARP** (IPv4) or
neighbour discovery (IPv6).

## IP: addresses and routing

**IPv4** addresses have 32 bits (`192.0.2.17`); **IPv6** addresses have 128
(`2001:db8::17`). An address splits into a **network prefix** and a host part, written in **CIDR**
notation: `10.0.0.0/8` means the first 8 bits are the network.

- Each host has a **routing table**: for a destination, which next hop and interface to use. The
  longest matching prefix wins; the **default route** (`0.0.0.0/0`) points to the gateway.
- Routers forward packets hop by hop. Between organisations, **BGP** announces which prefixes are
  reachable through whom.
- IP is **best effort**: packets may be lost, duplicated, reordered or delayed. The **TTL** field
  is decremented at each hop so loops do not run forever; `traceroute` uses it to discover the
  path.
- **Private ranges** (`10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`) are not routed on the
  internet; **NAT** on your router rewrites them to one public address, tracking connections by
  port.

## UDP and TCP

Both add **ports** (16-bit numbers that name a process's endpoint: 443 for HTTPS, 53 for DNS), so
a connection is identified by the 4-tuple (source IP, source port, destination IP, destination
port).

**UDP** adds almost nothing else: independent datagrams, no connection, no ordering, no
retransmission. Used by DNS, video calls, games and QUIC, which builds its own reliability on top.

**TCP** gives a reliable, ordered **byte stream**:

- **Three-way handshake**: `SYN` → `SYN-ACK` → `ACK`. One round trip before any data. Each side
  picks a random initial **sequence number**.
- Every byte has a sequence number; the receiver **acknowledges** what it has received. Missing
  data is **retransmitted** after a timeout or after three duplicate ACKs (fast retransmit). The
  receiver reorders and removes duplicates.
- **Flow control**: the receiver advertises a **window**, how much it can buffer; the sender never
  has more unacknowledged data in flight.
- **Congestion control**: the sender also limits itself to a **congestion window** that probes the
  network. **Slow start** begins at about 10 segments (~14 KB) and doubles every round trip;
  packet loss (Reno, CUBIC) or rising delay (BBR) makes it back off. A fresh connection therefore
  cannot use a fast link fully for several round trips.
- **Closing**: `FIN` and `ACK` in both directions; the side that closes first keeps the 4-tuple in
  `TIME_WAIT` for a while so late packets do not confuse a new connection.
- **Head-of-line blocking**: one lost segment holds back all later bytes, even those for
  unrelated HTTP/2 streams. HTTP/3 runs over **QUIC** (on UDP) to avoid it.

## Sockets

Programs use TCP and UDP through the **socket** API: a server calls `socket`, `bind` (to a port),
`listen` and `accept`; a client calls `socket` and `connect`; both `read` and `write` on the
resulting file descriptor. Every HTTP client and server library is built on this.

## DNS

Humans use names; IP needs addresses. The **Domain Name System** is a distributed, hierarchical,
cached database:

```text
your app → stub resolver (OS) → recursive resolver (ISP, 1.1.1.1, 8.8.8.8)
            → root servers ("ask .com")
            → .com TLD servers ("ask example.com's name servers")
            → example.com's authoritative servers ("api.example.com is 203.0.113.7")
```

- Record types: `A` (IPv4), `AAAA` (IPv6), `CNAME` (an alias), `MX` (mail), `TXT`, `NS` (the name
  servers of a zone).
- Every answer has a **TTL**; resolvers, operating systems, and runtimes cache it. A long TTL makes
  changes slow to propagate; a short one adds lookups. The JVM caches lookups itself
  (`networkaddress.cache.ttl`).
- DNS mostly runs over UDP port 53; DNS over HTTPS or TLS encrypts it.

## One HTTPS request, end to end

On a fresh connection, before the first byte of the response:

1. **DNS** lookup: 0 round trips if cached, one or more otherwise.
2. **TCP** handshake: 1 RTT.
3. **TLS 1.3** handshake: 1 RTT (TLS 1.2 needed 2); certificates are verified on the way.
4. **HTTP** request and response: 1 RTT, plus the server's processing time.

With a 100 ms round trip that is 300–400 ms of waiting, mostly **latency**, not bandwidth. Hence
the standard fixes: reuse connections (keep-alive, HTTP/2 multiplexing, pools), move servers
closer (CDNs and edge locations), resume TLS sessions or use 0-RTT, and use HTTP/3, which merges
the transport and TLS handshakes.

## Summary

- Networks are layered: application, transport, internet, link; each adds a header.
- IP routes best-effort packets by longest prefix match; NAT and private ranges hide hosts.
- UDP is datagrams with ports; TCP is a reliable byte stream with a handshake, ACKs,
  retransmission, flow control and congestion control.
- DNS resolves names through a hierarchy of cached servers with TTLs.
- Latency is counted in round trips; a fresh HTTPS request costs about three or four.

## Further reading

- Kurose and Ross, *Computer Networking: A Top-Down Approach*, 8th ed., chapters 1–5
- Peterson and Davie, *Computer Networks: A Systems Approach* (open edition): <https://book.systemsapproach.org/>
- Ilya Grigorik, *High Performance Browser Networking*, chapters 1–4 (latency, TCP, UDP, TLS): <https://hpbn.co/>
- Julia Evans, "How DNS works" and related zines: <https://jvns.ca/categories/dns/>
- RFC 9293, Transmission Control Protocol (TCP): <https://www.rfc-editor.org/rfc/rfc9293>
- Stanford CS144 Introduction to Computer Networking: <https://cs144.github.io/>
