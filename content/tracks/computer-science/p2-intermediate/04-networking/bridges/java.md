---
from: java
status: draft
concept: "Networking in Java"
from_way: "Opening a new HTTP connection for every request."
native_way: "Socket and SocketChannel wrap TCP sockets; java.net.http.HttpClient pools connections and speaks HTTP/2; InetAddress caches DNS lookups according to networkaddress.cache.ttl."
sensei_tip: "Reuse one HttpClient per application, set connect and request timeouts, and lower the DNS cache TTL if your endpoints move."
---

| In Java | Underneath |
| --- | --- |
| `new Socket(host, port)` | DNS lookup + TCP handshake |
| `HttpClient` | a connection pool with HTTP/2 |
| `InetAddress.getByName` | DNS through the resolver, cached |
| `socket.setTcpNoDelay(true)` | disables Nagle's algorithm |
| `DatagramSocket` | UDP |
