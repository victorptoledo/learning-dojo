# Image that checks SQL katas on PostgreSQL 17, offline.
# check.sh starts a throwaway server, loads the kata's fixture, runs the SQL tests through the
# `dojo` schema (dojo.sql) and prints the results in the `cargo test` format the dojo reads.
# `play` opens psql on the same database so a learner can try queries without installing anything.
FROM debian:trixie-slim

# Debian creates a "main" cluster when the server is installed; the dojo makes its own.
RUN mkdir -p /etc/postgresql-common \
    && echo "create_main_cluster = false" > /etc/postgresql-common/createcluster.conf \
    && apt-get update \
    && apt-get install -y --no-install-recommends postgresql-17 postgresql-client-17 less \
    && rm -rf /var/lib/apt/lists/*

COPY check.sh play.sh pg.sh dojo.sql /opt/dojo/
RUN chmod 755 /opt/dojo/check.sh /opt/dojo/play.sh \
    && ln -s /opt/dojo/play.sh /usr/local/bin/play \
    && useradd --create-home --uid 10001 dojo \
    && mkdir -p /var/lib/dojo /target \
    && chown dojo /var/lib/dojo /target

ENV PATH=/usr/lib/postgresql/17/bin:$PATH \
    PGDATA=/var/lib/dojo/pgdata \
    PGHOST=/tmp \
    PGUSER=dojo \
    LANG=C.UTF-8 \
    TZ=UTC

USER dojo
# The cluster is created once here; every container starts from this pristine copy.
RUN initdb --auth=trust --encoding=UTF8 --locale=C.UTF-8 --username=dojo > /dev/null

WORKDIR /work

CMD ["/opt/dojo/check.sh"]
