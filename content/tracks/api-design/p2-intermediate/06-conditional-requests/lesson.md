# Conditional Requests

Validators make HTTP cheaper and safer at the same time. In this kata a documents API learns
to send a strong `ETag` and `Last-Modified` with every representation, to answer `304 Not
Modified` when the client already has the current version, and to refuse writes that do not
prove which version they change: `428` without `If-Match`, `412` when it no longer matches.
That last rule is optimistic concurrency control on the wire, and it ends lost updates.
Press **Enter** to create the kata folder and open it.
