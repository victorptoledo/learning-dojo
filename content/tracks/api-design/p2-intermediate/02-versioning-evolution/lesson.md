# Versioning and Evolving an API Without Breaking Clients

The best versioning strategy is not needing a new version. Most changes can be made
**compatibly**; a new major version is an expensive event that every client has to migrate
through, and you will run both versions for months or years. This lesson is about telling
compatible from breaking changes, making them compatible when you can, and retiring old
versions politely when you cannot.

## Compatible and breaking changes

| Change | Compatible? | Why |
| --- | --- | --- |
| Add an optional request field or query parameter | ✅ | old clients do not send it |
| Add a response field | ✅ *if* clients are tolerant readers | strict deserialisers (`FAIL_ON_UNKNOWN_PROPERTIES`) break; say in the docs that clients must ignore unknown fields |
| Add a new endpoint or resource | ✅ | |
| Add an enum value in a response | ⚠️ | breaks clients with closed enums (`switch` without default). Document enums as open from day one |
| Add a new problem type | ⚠️ | fine if clients fall back on the status code |
| Make an optional request field required | ❌ | old clients do not send it |
| Remove or rename a field, change its type or meaning | ❌ | |
| Change a default (page size, sort order) | ❌ | Hyrum's law |
| Change status codes or URL structure | ❌ | |
| Tighten validation (shorter max length) | ❌ | requests that worked now fail |

The **robustness principle** for APIs: be strict in what you send (only documented values),
tolerant in what you accept (ignore unknown fields), and design clients the same way.

## Techniques that avoid a new version

- **Expand, then contract.** To rename `phone` to `phoneNumber`: add `phoneNumber`, keep
  `phone` filled, mark it deprecated in the OpenAPI document, measure who still reads it,
  remove it only after the deprecation period, or never.
- **Defaults for new required data.** A new mandatory field (country) gets a server-side default
  or "unknown" for old clients; only new clients must send it.
- **Mapping for old clients.** A new enum value (`SUSPENDED`) is shown to old versions as the
  closest old value (`INACTIVE`); see the checkpoint kata.
- **New resources instead of changed ones.** A different representation of payments can be a
  new resource (`/payment-intents`) living next to the old one.

## When you need a version: where to put it

| Where | Example | For | Against |
| --- | --- | --- | --- |
| URL path | `/v2/customers/42` | visible, easy to route, cache and test in a browser | a "version" of the whole API for a change in one resource |
| Header / media type | `Accept: application/vnd.shop.customer+json; version=2` | the URL names the resource, not the version | invisible in logs and links; caches need `Vary` |
| Date-based version header | `Stripe-Version: 2026-03-01` | clients pin a date; the server transforms responses step by step | needs a transformation layer per change |
| Query parameter | `?api-version=2026-03-01` (Azure) | easy to send | mixes versioning with filtering |

Path versions with a **major number only** are the most common for public APIs. Whatever you
choose, version the **contract**, not the implementation, and never release `v1.3` paths:
compatible changes do not need a new number.

## Retiring a version

1. Announce: changelog, emails to key consumers, docs.
2. Mark responses: `Deprecation: @1793491200` (RFC 9745, a timestamp) and
   `Sunset: Tue, 01 Jun 2027 00:00:00 GMT` (RFC 8594), with
   `Link: <.../v2/customers/42>; rel="successor-version"` and a `rel="deprecation"` link to a
   migration guide.
3. Measure: log and dashboard which clients (API keys) still call the old version.
4. Remind, then do short "brownouts" (planned minutes of `410`) so forgotten clients surface.
5. After the sunset: `410 Gone` with a problem detail pointing to the migration guide.

## Contract tests

The only reliable way to know a change is compatible is to test it against what clients
actually use. **Consumer-driven contract tests** (Pact, Spring Cloud Contract) record each
client's expectations and run them in the provider's build; an OpenAPI diff tool
(`oasdiff`, `openapi-diff`) flags breaking changes between two versions of the document in CI.
The checkpoint kata of this phase starts from such a consumer contract.

## Further reading

- RFC 9745, "The Deprecation HTTP Response Header Field": <https://www.rfc-editor.org/rfc/rfc9745>
- RFC 8594, "The Sunset HTTP Header Field": <https://www.rfc-editor.org/rfc/rfc8594>
- Google AIP-180, "Backwards compatibility", and AIP-185, "API Versioning": <https://google.aip.dev/180>, <https://google.aip.dev/185>
- Stripe engineering blog, "APIs as infrastructure: future-proofing Stripe with versioning": <https://stripe.com/blog/api-versioning>
- Zalando RESTful API Guidelines, "Compatibility": <https://opensource.zalando.com/restful-api-guidelines/#compatibility>
- Pact documentation, "Consumer-driven contract testing": <https://docs.pact.io/>
- Martin Fowler, "TolerantReader": <https://martinfowler.com/bliki/TolerantReader.html>
