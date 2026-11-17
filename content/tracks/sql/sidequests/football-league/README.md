# Sidequest: Football league

Six clubs play a league in a double round robin: ten rounds of three matches, each club hosts
every other club once. Eight rounds are played, two are still to come. You build the league's
database from an empty folder, in three stages:

1. the schema, with keys and rules the database enforces;
2. loading the season from the spreadsheet the league exported, where every value is text;
3. the numbers fans argue about: the league table, the top scorers, the players who never
   scored and a summary of each round.

How it is checked:

- Your work is the `.sql` files at the top of the folder. `schema.sql` creates the tables; the
  other files are one script or one query each, as each stage says. Write plain statements:
  no `BEGIN`/`COMMIT` and no `psql` commands such as `\i`, because the dojo runs each file
  inside its own transaction and rolls everything back after every test.
- The table and column names in each stage's statement are the contract: the tests insert
  rows and read results through them. Types, extra constraints and the way you write each
  query are yours.
- From stage 2 on, `fixture/` holds the league's export (`import_*` tables). The dojo loads it
  before every check; do not edit it.
- Try your files with `docker run -it --rm --network none -v "${PWD}:/work" dojo-sql play`
  from this folder, then `\i schema.sql`, `\i load.sql` and so on.

When you are done, `[A]` copies a review prompt: ask an AI about your schema and queries.
