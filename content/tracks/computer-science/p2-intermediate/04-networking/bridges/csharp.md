---
from: csharp
status: draft
concept: "Networking in C#"
from_way: "Creating and disposing an HttpClient per request."
native_way: "HttpClient uses SocketsHttpHandler with a connection pool; disposing it per request exhausts ports in TIME_WAIT; Socket is the raw API; Dns.GetHostAddressesAsync resolves names."
sensei_tip: "Use IHttpClientFactory or one long-lived HttpClient with PooledConnectionLifetime, so connections are reused but DNS changes are still picked up."
---

| In C# | Underneath |
| --- | --- |
| `HttpClient` | a pool of TCP/TLS connections |
| `PooledConnectionLifetime` | recycle connections for DNS changes |
| `Socket` | BSD socket |
| `UdpClient` | UDP |
| `Dns.GetHostAddressesAsync` | name resolution |
