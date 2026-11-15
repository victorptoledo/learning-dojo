# Image that checks Linux (bash) katas: bash -n, bats-core tests and shellcheck, offline.
# Prints results in the `cargo test` format the dojo already reads (see tap2cargo.awk).
FROM debian:trixie-slim

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        bats bats-assert bats-support bats-file shellcheck \
        procps psmisc gawk sed grep findutils coreutils file less tree bc \
    && rm -rf /var/lib/apt/lists/*

COPY check.sh tap2cargo.awk /opt/dojo/
RUN chmod 755 /opt/dojo/check.sh \
    && useradd --create-home --uid 10001 dojo \
    && mkdir /target \
    && chown dojo /target

ENV BATS_LIB_PATH=/usr/lib/bats \
    LANG=C.UTF-8 \
    TZ=UTC

USER dojo
WORKDIR /work

CMD ["/opt/dojo/check.sh"]
