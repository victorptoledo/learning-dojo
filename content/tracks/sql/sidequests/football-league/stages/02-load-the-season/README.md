The league exported the season from a spreadsheet: `fixture/01_import.sql` creates and fills
six `import_*` tables where **every column is text**. Read them first (`\d import_matches`,
`SELECT * FROM import_goals LIMIT 5`): some names arrive with stray spaces.

| Table | Columns |
|---|---|
| `import_teams` | `name`, `city` |
| `import_players` | `team`, `player`, `shirt` |
| `import_matches` | `round`, `played_on` (`2026-08-01`), `home`, `away`, `score` (`'2-1'`, or `''` when not played yet) |
| `import_goals` | `round`, `home`, `away` (the match), `scorer`, `scorer_team` (the scorer's own team), `minute`, `own_goal` (`'yes'` or `'no'`) |
| `import_squad_changes` | `team`, `player`, `shirt` |
| `import_results` | `round`, `home`, `away`, `score` |

Write three scripts, each run after `schema.sql`:

- **`load.sql`** fills `teams`, `players`, `matches` and `goals` from the first four tables:
  names trimmed, numbers and dates cast, an empty score stored as a match not played yet
  (`NULL`, `NULL`), and every goal joined to its match and to its scorer. Use
  `INSERT ... SELECT` with joins on the names, never ids typed by hand.
- **`squad_changes.sql`** applies `import_squad_changes` in one `INSERT ... ON CONFLICT`:
  a player new to the team is added; a player the team already has keeps their row (and so
  their goals) and only gets the new shirt number. Running it twice gives the same players.
- **`record_results.sql`** writes the scores of `import_results` into the matching rows of
  `matches`, found by round, home team and away team, in one `UPDATE ... FROM`. Rows that
  match no match change nothing.

> [!TIP]
> `split_part('2-1', '-', 1)` is `'2'`, and `nullif(x, '')` turns an empty string into
> `NULL` before the cast.
