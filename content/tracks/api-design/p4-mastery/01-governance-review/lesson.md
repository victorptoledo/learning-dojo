# API Governance, Style Guides and Design Review

One well-designed API is craft. Two hundred APIs built by forty teams that feel like **one
product** is governance. Without it, every team re-decides pagination, errors and naming, and
consumers pay for every difference. With too much of it, teams wait weeks for a committee.
Good governance makes the right thing the easy thing: written rules, automated checks, a light
review for what tools cannot judge, and a catalogue so APIs can be found and reused.

## The style guide

Start from a published guide (Google AIP, Microsoft, Zalando, the Adidas or PayPal guides) and
write down only your decisions and deviations. A useful guide is short, has examples of good
and bad, and says **MUST**, **SHOULD** or **MAY** (RFC 2119 keywords) for every rule:

| Area | Decisions to write down |
| --- | --- |
| URLs and names | case of segments and fields, plural nouns, id format, glossary |
| Methods and status codes | PUT vs PATCH, `400` vs `422`, `404` for unauthorised objects |
| Errors | problem details, the type URI base, the catalogue of shared types |
| Collections | cursor parameters, default and maximum sizes, sort syntax |
| Evolution | compatibility rules, versioning scheme, deprecation periods and headers |
| Reliability | which operations require `Idempotency-Key`; ETags on mutable resources |
| Security | auth schemes, scope naming, data that must never appear in URLs |
| Documentation | OpenAPI version, required descriptions, examples, `operationId`s |

## Automate what can be automated

Every rule a machine can check belongs in a **linter** in CI (Spectral, Redocly CLI, Vacuum,
or your own, as in the next kata), plus a **breaking-change diff** against the released
contract. Humans then review what only humans can: is the model right, are the names
meaningful to consumers, is this the right resource boundary, is the change worth its cost.

## Design review

A lightweight, asynchronous review of the **contract** before implementation:

1. The team opens a pull request with the OpenAPI change and a short description: who the
   consumers are, the use cases, alternatives considered.
2. CI posts the lint and breaking-change results.
3. One or two reviewers from an API guild (not a central gatekeeper team) review within an
   agreed time, for example two working days.
4. Outcomes: approve, approve with comments, or "needs a conversation". Decisions that set a
   precedent become a new guide rule or an ADR.

A review checklist that catches most real problems:

- Does every resource and field make sense to a consumer who has never seen our database?
- Is the change compatible? If not, is the migration plan in the PR?
- Are errors specific problem types with actionable details? Is every error documented?
- Can every write be retried safely? Can collections grow without breaking?
- Are auth, scopes and object-level checks clear for each operation?
- Would a new developer get their first successful call in ten minutes from the docs?

## The API lifecycle

```text
design → review → build → publish (catalogue, docs, SDKs) → operate (metrics, SLOs)
       → evolve (compatible changes) → deprecate → sunset → retire
```

- An **API catalogue** (Backstage, an API portal) lists every API with owner, contract,
  maturity (experimental, beta, stable, deprecated) and consumers.
- **Ownership** is explicit: a team and a contact (`info.contact` in OpenAPI).
- **Maturity labels** set expectations: an experimental API may change without notice; a stable
  one follows the compatibility rules.
- **Usage data** (calls per consumer, per operation, per field for GraphQL) drives
  deprecations and tells you who to warn.

## Measuring success

Time to first successful call, number of guide violations per new API, support tickets per
integration, percentage of APIs with contracts in the catalogue, breaking changes shipped by
accident (target: zero).

## Further reading

- Google AIP-1, "AIP Purpose and Guidelines", and AIP-100, "API Design Review FAQ": <https://google.aip.dev/1>, <https://google.aip.dev/100>
- Zalando RESTful API Guidelines, "API design review" and "API first": <https://opensource.zalando.com/restful-api-guidelines/#api-design-principles>
- Microsoft REST API Guidelines: <https://github.com/microsoft/api-guidelines>
- RFC 2119, "Key words for use in RFCs to Indicate Requirement Levels": <https://www.rfc-editor.org/rfc/rfc2119>
- Stoplight Spectral, "Rulesets": <https://docs.stoplight.io/docs/spectral/>
- Redocly CLI, "Lint": <https://redocly.com/docs/cli/commands/lint>
- Backstage, "API catalogue (software catalog)": <https://backstage.io/docs/features/software-catalog/>
