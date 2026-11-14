# URL Shortener

The entity `ShortLink` (with a **unique** `code` column), the records and the error handling are
written. You write the repository, `LinkService` and `LinkController`.

## Endpoints

| Request | Answer |
| --- | --- |
| `POST /links` `{"url": "...", "code": "optional"}` | `201`, `Location: .../links/{code}`, `{"code", "url", "clicks": 0}` |
| `GET /{code}` | **`302 Found`** with `Location:` the target url, and one more click |
| `GET /links/{code}` | `200`, the link with its click count |
| `GET /links/top?limit=10` | `200`, the most clicked links first; ties by code |

## Rules

- The url must be an **absolute `http` or `https` url** with a host. Anything else
  (`ftp:`, `javascript:`, a relative path, garbage, missing) is `LinkException.invalidUrl` (400).
  A redirect to `javascript:` is how short links get abused.
- A custom code is 3 to 20 letters, digits or dashes (`invalidCode`, 400) and must be free
  (`codeTaken`, 409). Without one, generate **7 random letters or digits** with a `SecureRandom`,
  trying again in the unlikely case it is taken.
- Unknown codes are `notFound` (404).
- **Clicks must not be lost.** Forty concurrent redirects must count forty. Reading the entity,
  adding one and saving it loses updates when two requests interleave; increment **in the
  database** with one statement instead:
  `update ShortLink l set l.clicks = l.clicks + 1 where l.code = :code`, which also tells you (0
  rows) when the code does not exist.

> Tip: `URI.create(url)` with `getScheme()` and `getHost()` (it throws `IllegalArgumentException`
> on garbage), `@Modifying @Query(...) int addClick(@Param("code") String code)` inside a
> `@Transactional` method, `ResponseEntity.status(HttpStatus.FOUND).location(uri).build()`, and a
> `Pageable` (`PageRequest.of(0, limit)`) on a `@Query` for the top list. Map `/links/top` so it is
> not taken for a code.
