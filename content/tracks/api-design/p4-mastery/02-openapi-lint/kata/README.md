# OpenAPI Linter

The company's API style guide is a wiki page nobody reads. Turn it into code: a linter that
reads an OpenAPI document (YAML) and reports every violation, so that design review can start
from a green build instead of from a checklist. This is what tools like Spectral or Redocly
do with rule sets; you write the rules yourself.

`Finding` is written. Write `Linter.lint(String yaml)`. SnakeYAML is on the classpath:
`new Yaml().load(text)` gives nested `Map`s and `List`s.

## The rules

| Rule id | Where (`pointer`) | Reported when | `message` |
| --- | --- | --- | --- |
| `openapi-version` | `/openapi` | `openapi` is missing or not `3.1.x` / `3.2.x` | `openapi must be 3.1.x or 3.2.x, got 3.0.3` (`got nothing` when missing) |
| `info-contact` | `/info` | `info.contact.email` is missing or blank | `info.contact.email is required` |
| `path-kebab-case` | the path | a literal segment is not lowercase kebab-case (`[a-z0-9]+(-[a-z0-9]+)*`; `{templates}` are skipped) | `segment lineItems is not lowercase kebab-case` |
| `path-kebab-case` | the path | the path (other than `/`) ends with `/` | `path ends with a slash` |
| `path-no-verbs` | the path | a literal segment starts (its first run of lowercase letters) with `get`, `list`, `fetch`, `create`, `add`, `update`, `delete` or `remove` | `segment get-orders starts with the verb get: let the HTTP method say what happens` |
| `path-params-defined` | the operation | a `{name}` of the path has no parameter with that `name`, `in: path` and `required: true`, in the path item's or the operation's `parameters` | `path parameter lineId is not declared as a required path parameter` |
| `operation-id` | the operation | no `operationId` | `missing operationId` |
| `operation-id` | the operation | an `operationId` already used earlier in the document | `duplicate operationId listOrders` |
| `no-get-body` | the operation + `/requestBody` | a `get` has a `requestBody` | `a GET must not have a request body` |
| `security-defined` | the operation | the operation has no `security` key and the document has no (or an empty) root `security` | `no security requirement: declare one, or security: [] for a public operation` |
| `success-response` | the operation + `/responses` | no response code `2XX` or `200`–`299` | `no 2xx response` |
| `problem-json` | the operation + `/responses/{code}` | a `4xx`, `5xx` or `default` response has a non-empty `content` without `application/problem+json` | `error responses must use application/problem+json` |
| `schema-camel-case` | `/components/schemas/{name}/properties/{property}` | a property name is not camelCase (`[a-z][a-zA-Z0-9]*`) | `property created_at is not camelCase` |

Operations are the path item keys `get`, `put`, `post`, `delete`, `options`, `head`, `patch`
and `trace`. An operation that says `security: []` is public on purpose: that is fine.

## Pointers and order

`pointer` is an RFC 6901 JSON Pointer into the document: each key is a reference token, with
`~` written `~0` and `/` written `~1`. The `get` of the path `/orders/{orderId}` is
`/paths/~1orders~1{orderId}/get`.

Return every finding, sorted by `pointer`, then `rule`, then `message` (plain `String`
order). One finding per problem: a path with two bad segments has two findings.

> Watch out: YAML reads an unquoted `200:` or `404:` as a **number**, while `'200':` and `2XX:`
> are strings. Turn keys into strings (`String.valueOf`) before you compare them.
