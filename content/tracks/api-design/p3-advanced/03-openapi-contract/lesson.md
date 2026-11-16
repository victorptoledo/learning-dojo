# OpenAPI as a Contract

The **OpenAPI Specification** (OAS) describes an HTTP API in a machine-readable YAML or JSON
document: its paths, operations, parameters, request and response bodies, errors and security.
Written well, that one file becomes the documentation, the source of client SDKs and server
stubs, the input of mock servers, linters and breaking-change detectors, and the thing
reviewers approve. Written badly (generated after the fact, never read), it is a decoration.

## Versions

| Version | Notable |
| --- | --- |
| 2.0 ("Swagger") | still common in old tools |
| 3.0 (2017) | `components`, `requestBody`, `oneOf`; its own JSON Schema dialect |
| 3.1 (2021) | **full JSON Schema 2020-12**; `webhooks`; `type: [string, "null"]` instead of `nullable` |
| 3.2 (2025) | tags with hierarchy, streaming and sequential media types (`text/event-stream`, JSON Lines), the `QUERY` method, better `security` and examples |

Write new documents in 3.1 or 3.2.

## Anatomy

```yaml
openapi: 3.1.1
info:
  title: Orders API
  version: 2.3.0                      # the version of the document, not a URL segment
  contact: { email: orders-team@shop.example }
servers:
  - url: https://api.shop.example/v2
security:
  - oauth: [orders:read]             # default for every operation
paths:
  /orders/{orderId}:
    parameters:
      - { name: orderId, in: path, required: true, schema: { type: string } }
    get:
      operationId: getOrder          # becomes the SDK method name
      summary: Read one order
      responses:
        '200':
          description: The order
          headers:
            ETag: { schema: { type: string } }
          content:
            application/json:
              schema: { $ref: '#/components/schemas/Order' }
        '404': { $ref: '#/components/responses/NotFound' }
components:
  schemas:
    Order:
      type: object
      required: [id, status, totalCents]
      properties:
        id: { type: string, examples: [ord_8f2kq] }
        status: { type: string, enum: [PENDING, PAID, SHIPPED, CANCELLED] }
        totalCents: { type: integer, minimum: 0 }
        createdAt: { type: string, format: date-time }
  responses:
    NotFound:
      description: No such resource
      content:
        application/problem+json:
          schema: { $ref: '#/components/schemas/Problem' }
  securitySchemes:
    oauth:
      type: oauth2
      flows:
        authorizationCode:
          authorizationUrl: https://auth.shop.example/authorize
          tokenUrl: https://auth.shop.example/token
          scopes: { 'orders:read': Read orders, 'orders:write': Create and change orders }
```

Reuse through `components` and `$ref`: one `Problem` schema, one `NotFound` response, one
`PageCursor` parameter for every collection. Consistency is easier when it is copy-free.

## Design-first workflow

1. Write or change the document in a pull request: the API change **is** the diff.
2. CI runs a **linter** (Spectral, Redocly, Vacuum) with the team's style guide as rules, and a
   **breaking-change check** (`oasdiff breaking old.yaml new.yaml`).
3. Reviewers read the contract, not the code (phase 4: design review).
4. Mock servers (Prism, Microcks) let clients start before the server exists.
5. The implementation is generated from or checked against the document: generated server
   interfaces, or contract tests that validate real responses against the schemas.

Code-first (annotations such as springdoc-openapi, FastAPI, NestJS decorators) is fine when the
generated document is **reviewed and diffed** like a hand-written one; the danger is never
looking at it.

## Making the contract precise

- `required` lists, `minimum`/`maxLength`/`pattern`, `format`, `enum`: every constraint you
  enforce should be in the schema, so SDKs and docs show it.
- `examples` on schemas and operations; they become the docs that developers actually read.
- Every operation documents its error responses (as problem details) and its security.
- `deprecated: true` on operations, parameters and schema properties during expand-then-contract.
- `operationId` on every operation, unique and stable: SDK method names depend on it.

The kata of phase 4 turns rules like these into a linter you write yourself.

## Further reading

- OpenAPI Specification 3.1.1: <https://spec.openapis.org/oas/v3.1.1.html>
- OpenAPI Specification 3.2.0: <https://spec.openapis.org/oas/v3.2.0.html>
- OpenAPI Initiative, "OpenAPI Learn" guides: <https://learn.openapis.org/>
- JSON Schema 2020-12: <https://json-schema.org/specification>
- Stoplight Spectral (linter) documentation: <https://docs.stoplight.io/docs/spectral/>
- oasdiff (breaking-change detection): <https://github.com/oasdiff/oasdiff>
- springdoc-openapi (Spring Boot): <https://springdoc.org/>
