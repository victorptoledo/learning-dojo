#!/bin/bash
# An interactive psql on a throwaway PostgreSQL 17 with the kata's fixture loaded.
# From a kata folder:  docker run -it --rm --network none -v "${PWD}:/work" dojo-sql play
# Everything is lost when you quit; your .sql files stay in the kata folder.
set -u
# shellcheck source=pg.sh
. /opt/dojo/pg.sh

start_server
load_fixture /work
cat <<'EOF'
PostgreSQL 17 with the kata's tables. Try:
  \dt            list the tables          \d table    describe one
  \i query.sql   run one of your files    \q          quit (the database is thrown away)
EOF
exec psql -X -d dojo
