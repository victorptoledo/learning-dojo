# Webhooks and Asynchronous APIs

Request/response only covers half of an integration. Clients also need to know when something
**happens**: a payment succeeded, a build finished, a document was signed. Polling
(`GET /payments/42` every few seconds) wastes everybody's capacity and still reacts late.
This lesson covers the two asynchronous patterns an HTTP API designer meets most:
**long-running operations** and **webhooks**.

## Long-running operations

When work takes longer than a request should (exports, video processing, provisioning):

```http
POST /exports            {"format": "csv", "from": "2026-01-01"}
→ 202 Accepted
  Location: /exports/exp_31
  {"id": "exp_31", "status": "RUNNING", "progress": 0}

GET /exports/exp_31
→ 200 {"id": "exp_31", "status": "SUCCEEDED", "resultUrl": "https://files.example/exp_31.csv"}
```

- The operation is a **resource** with a status (`RUNNING`, `SUCCEEDED`, `FAILED`,
  `CANCELLED`), progress, and the result or a problem detail when it fails.
- Clients poll with backoff (`Retry-After` hints the interval), or get a webhook when it ends.
- `DELETE /exports/exp_31` (or `POST .../cancellation`) cancels it. Google AIP-151 and the
  Azure guidelines describe this pattern in detail.

## Webhooks: the provider calls the client

The client registers a URL; the provider sends an HTTP `POST` for each event.

```http
POST https://shop.client.example/hooks/payments
Content-Type: application/json
webhook-id: msg_2KWPBgLlAfxdpx2AI54pPJ85f4W
webhook-timestamp: 1793491200
webhook-signature: v1,K5oZfzN95Z9UVu1EsfQmfVNQhnkZ2pj9o9NDN/H/pI4=

{"type": "payment.succeeded", "id": "evt_9", "createdAt": "2026-11-01T00:00:00Z",
 "data": {"paymentId": "pay_42", "amountCents": 2500}}
```

Design rules (they match the **Standard Webhooks** specification, used by several large
providers):

1. **Sign every delivery.** HMAC-SHA256 over `id.timestamp.body` with a per-endpoint secret;
   receivers verify with a constant-time comparison and **reject old timestamps** (for example
   older than five minutes) to stop replays. Support two secrets during rotation.
2. **At-least-once delivery.** Retry on timeouts and non-`2xx` with exponential backoff for
   hours or days; receivers therefore must be **idempotent**: deduplicate by event id.
3. **No ordering guarantee.** Events can arrive out of order; include a timestamp or version,
   or send *thin* events (`{"type": "payment.updated", "id": "pay_42"}`) that make the receiver
   fetch the current state.
4. **Answer fast.** Receivers acknowledge with `2xx` within seconds and process later from a
   queue; the provider uses short timeouts.
5. **Event types are a contract.** Name them `resource.event` (`invoice.paid`), version their
   payloads, document them (OpenAPI 3.1 has a `webhooks` section; AsyncAPI for brokers).
6. **Operability.** A dashboard of deliveries, manual replay, automatic disabling of endpoints
   that fail for days (with a notice), and an endpoint to send a test event.
7. **Security of the provider**: webhooks are outbound requests to customer-chosen URLs.
   Block internal addresses (SSRF), resolve and pin the IP, require HTTPS.

## Events beyond webhooks

| Mechanism | Fits |
| --- | --- |
| Webhooks | notifying external systems over the public internet |
| Message brokers (Kafka, RabbitMQ, SNS/SQS) | internal, high-volume, many consumers; described with **AsyncAPI** |
| Server-Sent Events / WebSockets | live updates to a connected browser or app |
| CloudEvents | a common envelope (`id`, `source`, `type`, `time`, `data`) across all of the above |

The Backend track's "Messaging You Can Trust" (`backend.p2.messaging-reliability`) covers the
outbox pattern and idempotent consumers that make providers and receivers reliable.

## Further reading

- Standard Webhooks specification: <https://www.standardwebhooks.com/>
- CloudEvents specification (CNCF): <https://cloudevents.io/>
- AsyncAPI specification: <https://www.asyncapi.com/docs/reference/specification/latest>
- OpenAPI Specification 3.1.1, "webhooks" field: <https://spec.openapis.org/oas/v3.1.1.html#openapi-object>
- Google AIP-151, "Long-running operations": <https://google.aip.dev/151>
- Stripe docs, "Receive Stripe events in your webhook endpoint" (signatures and retries): <https://docs.stripe.com/webhooks>
- OWASP, "Server-Side Request Forgery Prevention Cheat Sheet": <https://cheatsheetseries.owasp.org/cheatsheets/Server_Side_Request_Forgery_Prevention_Cheat_Sheet.html>
