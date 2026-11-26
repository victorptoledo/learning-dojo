# Checkpoint 3: Clinic Network

A telehealth network has clinics in three regions: `eu` (clinics 1 and 2), `us` (3 and 4) and
`br` (5 and 6). `fixture/01_clinics.sql` creates:

```text
clinics         (id, name UNIQUE, region)
patients        (id, clinic_id → clinics, full_name, email, diagnosis)
visits_br       (id, region, clinic_id, patient_id, starts_at, status, notes)   -- last week's import
clinic_networks (id, clinic_id → clinics, net cidr)                             -- 20,000 networks
prescriptions   (id, patient_id → patients, drug, active)
index_usage     (index_name, idx_scan)        -- this morning's pg_stat_user_indexes
```

`visits_br` holds the 900 Brazilian visits, with the ids (1 to 900) they had in the old system.
Patient 1 is Ana Silva, `Ana.Silva@example.com`, of clinic 1. A visit is `scheduled`, `done` or
`cancelled`.

| File | Kind | Task |
| --- | --- | --- |
| `schema.sql` | statements | Create `visits` with the columns of `visits_br` (all `NOT NULL` except `notes`, `status` defaulting to `scheduled`, `id` a `bigint` identity), references to `clinics` and `patients`, **partitioned by list of `region`**: `visits_eu`, `visits_us`, and `visits_br` **itself**, keeping its rows where they are. There is **no default partition**: a visit of an unknown region is an error. Add a primary key, and an index on `(patient_id, starts_at)` that every partition has. New visits, in any region, get ids after the imported ones. |
| `indexes.sql` | statements | (1) The VPN asks "which clinic owns address `10.20.30.40`?" as `net >>= inet '10.20.30.40'`: make that an index lookup. (2) A patient may have at most **one active** prescription of a drug, whatever the case of its name (`Ibuprofen` and `ibuprofen` are the same drug); inactive ones are history and can repeat. |
| `index_report.sql` | one query | The indexes of the `public` schema to drop: `index_name`, `reason`, by name. The reason is the first that applies: **`invalid`** (a failed build); **`redundant`**, a plain (no `WHERE`, no expression) non-unique B-tree whose columns are the **leading** columns of another valid plain B-tree of the same table (of two with exactly the same columns, the one whose name sorts last is the redundant one); **`unused`**, a non-unique index with `idx_scan = 0` in `index_usage`. Indexes that enforce a primary key or a unique constraint are never dropped. Judge from the catalog, not from the names. |
| `security.sql` | statements | Three roles, no login needed here. **`reception`** reads patients except their `diagnosis`, reads visits except their `notes`, and books visits. **`clinician`** reads patients and visits and records the outcome of a visit: its `status` and `notes`, nothing else. Both see and write only the visits of the clinic of the request (`SET LOCAL app.clinic_id = '<id>'`); no clinic means no visits. **`web`**, the public booking page, reads no table at all: it may only call `next_visit(p_email text, p_after timestamptz)`, which returns the start of the patient's first `scheduled` visit after `p_after` (email compared without case), and only `web` may call it. |

Details that matter:

- `ATTACH PARTITION` takes an existing table as it is, if its columns match. In production you
  would first add a `CHECK (region = 'br')` to it, so the attach does not have to scan it.
- A primary key of a partitioned table must include the partition key.
- The tests read your files as the test user (a superuser, which bypasses row-level security)
  and switch roles with `SET LOCAL ROLE` before checking.
- A policy on `visits` applies to queries on `visits`, not to queries on one of its partitions.
- `next_visit` runs with its owner's rights, so it must not be fooled by a caller: one test
  creates a temporary table called `patients` before calling it.

Try it with `docker run -it --rm --network none -v "${PWD}:/work" dojo-sql play`, then
`BEGIN; \i schema.sql`, `\i security.sql`, `SET LOCAL ROLE reception;`,
`SET LOCAL app.clinic_id = '5';` and query.

There are 30 tests: 1 in `tests/visible.sql` and 29 hidden ones.
