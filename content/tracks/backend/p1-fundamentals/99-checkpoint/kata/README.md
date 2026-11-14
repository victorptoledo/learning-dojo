# Checkpoint 1: Issue Tracker

Projects (`DOJO`) hold issues numbered from 1 inside each project (`DOJO-1`, `DOJO-2`). The
entities, records and error handling are written. `Issue` has a `@Version` column; its value is
the issue's **ETag** (`"0"`, `"1"`, ...). You write the repositories, `TrackerService` and
`TrackerController`.

## Endpoints

| Request | Answer |
| --- | --- |
| `POST /projects` `{"key": "DOJO", "name": "..."}` (validated) | `201`; duplicate key `409` |
| `POST /projects/{key}/issues` `{"title", "assignee"?}` (validated) | `201`, `Location: .../projects/{key}/issues/{number}`, `ETag`, the issue |
| `GET /projects/{key}/issues/{number}` | `200`, the issue, `ETag` |
| `GET /projects/{key}/issues?status=&assignee=&page=0&size=20` | `200`, an `IssuePage` sorted by number |
| `PATCH /projects/{key}/issues/{number}` with `If-Match` and a merge-patch body | `200`, the issue and its new `ETag` |

## Rules

- The next issue number is the project's highest number plus one.
- Filters are optional and combine; `status` must be a known status (else `400`, Spring does this
  when the parameter is the enum). `page` ≥ 0 and `size` 1 to 50, else `TrackerException.badRequest`.
- `PATCH` (`application/merge-patch+json`) changes only the fields present: `title` (not blank),
  `status` (a known status), `assignee` (`null` clears it). Any other field is `badRequest`.
- **Optimistic concurrency:** no `If-Match` → `ifMatchRequired()` (**428**); an `If-Match` that is
  not the current version → `stale(current)` (**412**); nothing changes in either case.
- Unknown projects and issues are `404`.

> Tip: `Optional<Project> findByKey(String key)`, a `@Query` with
> `coalesce(max(i.number), 0)` for the next number, a `@Query` with
> `(:status is null or i.status = :status)` and a `Pageable` for the filters, `@RequestBody
> Map<String, Object>` for the patch, and `saveAndFlush` so the incremented version is visible in
> the response.
