The questions fans ask, one query per file. The tests run `schema.sql` and `load.sql` first,
and also change results (a match recorded, a new club) to check that the answers follow the
data.

## The league table

`standings.sql` is the league table after the matches played so far:

| Column | Meaning |
|---|---|
| `team` | the team's name |
| `played` | matches played (a match with a `NULL` score is not played) |
| `won`, `drawn`, `lost` | results of those matches |
| `goals_for`, `goals_against` | goals scored and conceded |
| `goal_difference` | `goals_for - goals_against` |
| `points` | 3 per win, 1 per draw |

Every team appears, also one that has not played yet (all zeros). The order is the order of
the standings: most points first; equal points by goal difference, then by goals scored,
then by name.

> [!TIP]
> Each match counts for two teams. Turning every match into two rows, one from each side
> (`team, scored, conceded`), with `UNION ALL` or a join to `(VALUES ('home'), ('away'))`,
> makes the rest a plain `GROUP BY`.

## The season's numbers

Three more queries, one per file, on the same data.

| File | Columns | Rows and order |
|---|---|---|
| `top_scorers.sql` | `player`, `team`, `goals` | the five players with the most goals, **plus everyone level with the fifth**; most goals first, equal goals by name |
| `never_scored.sql` | `team`, `player`, `shirt_number` | players without a goal of their own; by team, then shirt number |
| `rounds.sql` | `round`, `played_on`, `played`, `goals`, `average` | every round, also those not played yet: the earliest date of the round, matches played, goals scored, and goals per played match rounded to 2 decimals (`NULL` when nothing is played); by round |

An own goal counts for the other team: it is never a goal of the player who scored it, so
it does not put them among the scorers nor take them out of `never_scored.sql`.

> [!WARNING]
> `FETCH FIRST 5 ROWS WITH TIES` keeps the rows that tie with the fifth on **every** column of
> the `ORDER BY`. Look at who has as many goals as the fifth before you trust it.
