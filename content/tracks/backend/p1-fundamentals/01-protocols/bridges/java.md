---
from: java
status: draft
concept: "Client-server communication"
from_way: "Writing framework route handlers without understanding the TCP sockets and HTTP messages underneath."
native_way: "HTTP/2 multiplexes requests over one TCP connection; HTTP/3 runs on QUIC over UDP and removes TCP head-of-line blocking."
sensei_tip: "Use WebSockets only for true two-way real time; for one-way server notifications, Server-Sent Events are simpler and more reliable."
---
