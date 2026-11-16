# Catalog Resources

`LegacyCatalogController` is the catalog API as it was first written: one `POST` per action, a
verb in every URL, and always `200 OK` with `{"ok": false, "error": ...}` when something goes
wrong. Clients cannot cache it, retry it safely or tell a missing product from a server bug
without parsing the body.

`Product`, `ProductRequest` (with its validation constraints) and `ProductStore` (in memory,
seeded with products 1 to 3) are written. Write `ProductController` and **delete**
`LegacyCatalogController` (or leave its file with only `package dojo;`).

## The resources

| Request | Answer |
| --- | --- |
| `GET /products` | `200`, every product as a JSON array ordered by id |
| `GET /products?category=kitchen` | `200`, only that category (an empty array if none) |
| `GET /products/{id}` | `200` and the product, or `404 Not Found` |
| `POST /products` with a `ProductRequest` | `201 Created`, a `Location` header with the **absolute** URL of the new product, and the product as body |
| `PUT /products/{id}` with a `ProductRequest` | `200` and the replaced product; `404` if it does not exist |
| `DELETE /products/{id}` | `204 No Content` with an empty body; `404` if it does not exist |
| a method a URL does not support (`DELETE /products`) | `405 Method Not Allowed` |
| invalid input (blank name or category, missing or negative price) | `400 Bad Request`, nothing stored |

`PUT` replaces the **whole** product, so every field is required: a `PUT` with only a name is a
`400`, not a partial update. The old RPC URLs must not answer any more.

```text
POST /products  {"name": "Teapot", "category": "kitchen", "priceCents": 1599}
→ 201 Created
  Location: http://localhost/products/4
  {"id": 4, "name": "Teapot", "category": "kitchen", "priceCents": 1599}
```

Error bodies are not graded here; the next lessons and the kata after this one give them a
standard shape (problem details).

> Tip: `ResponseEntity.of(optional)`, `ResponseEntity.created(uri)`,
> `ResponseEntity.noContent().build()`, `ServletUriComponentsBuilder.fromCurrentRequest()` and
> `@Valid @RequestBody`.
