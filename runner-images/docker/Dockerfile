# Image that checks Docker katas, offline and without a Docker engine: hadolint on the
# Dockerfile, `docker-compose config` on compose.yaml, and bats tests over the facts the
# dojo collected about the learner's image (crates/dojo-runner/src/image_check.rs).
# Prints results in the `cargo test` format the dojo already reads (see tap2cargo.awk).
FROM hadolint/hadolint:v2.15.1 AS hadolint

FROM debian:trixie-slim

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        bats bats-assert bats-support bats-file jq curl docker-compose gawk sed grep findutils coreutils \
    && rm -rf /var/lib/apt/lists/*

COPY --from=hadolint /bin/hadolint /usr/local/bin/hadolint
COPY check.sh tap2cargo.awk facts.bash hadolint.yaml /opt/dojo/
RUN chmod 755 /opt/dojo/check.sh \
    && useradd --create-home --uid 10001 dojo

ENV BATS_LIB_PATH=/usr/lib/bats \
    LANG=C.UTF-8 \
    TZ=UTC

USER dojo
WORKDIR /work

CMD ["/opt/dojo/check.sh"]
