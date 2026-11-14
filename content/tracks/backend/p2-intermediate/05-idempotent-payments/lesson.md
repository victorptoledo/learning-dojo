# Idempotent Payments

`POST` is not idempotent, so a client that times out and retries a payment can be charged twice.
The industry answer (used by Stripe and most payment APIs) is an **Idempotency-Key** header: the
server remembers the outcome of the first request with that key and replays it. Here you build it,
including the hard part: two identical requests arriving at the same moment. Press **Enter** to
create the kata folder and open it.
