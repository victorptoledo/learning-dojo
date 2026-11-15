# Image that checks Go katas: the compiler, the tests under the race detector, go vet and
# gofmt, offline and with the standard library only.
# Prints results in the `cargo test` format the dojo already reads (see report/main.go).
FROM golang:1.27-trixie

# No toolchain or module downloads at check time: the sandbox has no network anyway.
ENV GOTOOLCHAIN=local \
    GOPROXY=off \
    GOFLAGS=-buildvcs=false \
    CGO_ENABLED=1 \
    LANG=C.UTF-8 \
    TZ=UTC

COPY report /opt/dojo/report
COPY warm /opt/dojo/warm
COPY check.sh /opt/dojo/
RUN cd /opt/dojo/report \
    && go build -o /opt/dojo/dojo-report . \
    && chmod 755 /opt/dojo/check.sh \
    && useradd --create-home --uid 10001 dojo \
    && mkdir /target \
    && chown dojo /target

USER dojo

# Warm the build cache in the dojo user's home, so a check compiles only the kata:
# the standard library with and without the race detector, and the vet facts of the
# packages katas import.
RUN go telemetry off \
    && go build std \
    && go build -race std \
    && cd /opt/dojo/warm \
    && go vet . \
    && go test -c -race -vet=off -o /dev/null .

WORKDIR /work

CMD ["/opt/dojo/check.sh"]
