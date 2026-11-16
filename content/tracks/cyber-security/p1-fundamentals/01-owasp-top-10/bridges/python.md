---
from: python
status: draft
concept: "Authorise every object, not just every user"
from_way: "Django or Flask views decorated with @login_required that fetch objects with get(pk=pk)."
native_way: "Scope every query to the user (`Invoice.objects.get(pk=pk, owner=request.user)`) or use a permission layer, and return 404 for others' objects."
sensei_tip: "For SSRF, resolve the host with `socket.getaddrinfo`, check the address with `ipaddress.ip_address(...).is_global`, and connect to that address; `requests` follows redirects unless you pass `allow_redirects=False`."
---

| Python | OWASP Top 10 practice |
| --- | --- |
| Invoice.objects.get(pk=pk) | get(pk=pk, owner=request.user) |
| @login_required only | login plus an object permission |
| requests.get(user_url) | allow-list, IP check, no redirects |
| yaml.load(data) | yaml.safe_load(data) |
| DEBUG pages in production | generic errors, logged details |
