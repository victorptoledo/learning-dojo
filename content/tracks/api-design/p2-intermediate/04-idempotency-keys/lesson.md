# Idempotency Keys

`POST` is not idempotent, and money endpoints are exactly the ones clients retry most. In
this kata you write a reusable idempotency layer for two endpoints: keys scoped to a client,
a fingerprint that recognises the same request, replays marked with `Idempotent-Replayed`,
`422` for a reused key and `409` while the first request runs, keys released when the failure
means nothing happened, and a 24-hour memory. (The Backend track's kata "Idempotent Payments",
`backend.p2.idempotent-payments`, stores the same idea in a database with JPA; here the focus
is the contract.) Press **Enter** to create the kata folder and open it.
