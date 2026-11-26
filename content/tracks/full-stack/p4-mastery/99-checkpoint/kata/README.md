# Final Exam: Split the Name Without Downtime

The product must split `users.full_name` into `first_name` and `last_name`. The web app is
cached in browsers, the mobile app updates slowly, and the API runs several instances that are
replaced one by one, so for a while **the old release and the new one run side by side**. You
write the **expand release**: the step after which nothing breaks, whatever is still running.

- a migration that only **adds** (and a runner that applies it once, in a transaction);
- a repository that **writes both shapes** and a **backfill** in small batches;
- the read of the new columns behind a **feature flag** with a kill switch and a stable rollout;
- a **consumer contract** check that proves old clients still get what they read.

The contract step (making the columns `NOT NULL`, dropping `full_name`) is a later release, once
the metrics show no client reads `fullName`: it is not part of this kata.

## `migrations.ts`

`MIGRATIONS` already has `001_create_users` (`id`, `full_name`, `email`). Add
**`002_split_name_expand`**: the columns `first_name` and `last_name`, `TEXT`, **nullable and
without a default** (the old release keeps inserting rows with `full_name` only, and `NULL` is
how the backfill knows a row is not done). `full_name` stays as it is.

`migrate(db, migrations = MIGRATIONS)` creates `schema_migrations (name TEXT PRIMARY KEY,
applied_at TEXT NOT NULL)` when needed, applies in order each migration whose name is not
recorded, **each in its own transaction** together with its row in `schema_migrations`, and
returns the names it applied. When a migration fails, roll it back whole (none of its
statements stay), do not record it and rethrow; the ones before it stay applied.

## `names.ts`

| Function | Does |
| --- | --- |
| `splitFullName(fullName)` | Split on any whitespace after trimming: the first word is `firstName`, the other words joined by one space are `lastName` (`""` when there are none) |
| `joinName(firstName, lastName)` | Both trimmed, joined by one space; an empty part adds no space |

## `flags.ts`

| Function | Does |
| --- | --- |
| `fnv1a(text)` | 32-bit FNV-1a of the **UTF-8 bytes**: start at `0x811c9dc5`; for each byte, `hash ^= byte`, then `hash = Math.imul(hash, 0x01000193) >>> 0` |
| `bucket(flagName, userId)` | `fnv1a(`${flagName}:${userId}`) % 100` |
| `isEnabled(flag, userId)` | `false` for `undefined` or when `enabled` is `false` (the kill switch beats everything); `true` when `allow` has the user; otherwise `bucket(...) < percent` |

Hashing the flag name with the user id keeps each user's answer stable, makes a rollout only
grow (a user in at 10% is in at 50%), and keeps two flags from picking the same users.

## `users.ts`

`usersRepo(db)` returns:

| Method | Does |
| --- | --- |
| `get(id, readSplit)` | `{ id, fullName, firstName, lastName, email }` or `undefined`. `fullName` is always the `full_name` column. With `readSplit` **and** a row whose `first_name` is not `NULL`, the names come from the columns (`lastName` `""` if `NULL`); otherwise from `splitFullName(full_name)` |
| `rename(id, { fullName })` or `rename(id, { firstName, lastName })` | **One** `UPDATE` of `full_name`, `first_name` and `last_name`: from a full name, split it and store `joinName` of the parts as `full_name`; from the parts, trim them and store `joinName(...)`. Returns `get(id, true)`, or `undefined` for an unknown user |
| `backfillNames(batchSize)` | Fills `first_name` and `last_name` (with `splitFullName`) of at most `batchSize` rows whose `first_name` is `NULL`, lowest ids first, in one transaction; never touches a row whose names are already set; returns how many rows it filled |

## `contract.ts`

`checkUserForOldClients(body)` returns the problems that would break the old clients, one
string per broken field, each starting with the field's name; `[]` when the body is fine:

- `body`: not a JSON object (`null`, an array, a string...): the only problem returned;
- `id`: not a whole number;
- `fullName`: not a string, or empty after trimming;
- `email`: not a string containing `@`.

Extra fields are fine: old clients ignore what they do not know.

## `app.ts`

`createApp({ db, flags })`: `flags()` is called on **every request** (a flag change needs no
deploy), and the flag `READ_SPLIT_NAME` (`"read-split-name"`) gives `readSplit` for the user in
the URL.

| Route | Answer |
| --- | --- |
| `GET /api/users/:id` | `200` with the user from `get`, or `404` |
| `PATCH /api/users/:id` | The body must be **exactly one** shape (`parseRename`): `{ fullName }` with a non-blank string, or `{ firstName, lastName }` with two strings and a non-blank `firstName`. Otherwise `422`, also for both shapes at once or a body that is not JSON. `404` for an unknown user. `200` with the user as `GET` returns it |

## How the kata is checked

`tsc` checks every file, then `node:test` runs the migrations on in-memory SQLite databases,
runs the old release's SQL next to yours, rolls flags out to 1,000 users and calls the API with
flags that change between requests. There are 18 tests: 2 in `tests/visible.test.ts` and 16
hidden ones. Run `npm install` once and `npm test` to run the visible ones.
