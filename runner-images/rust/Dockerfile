# Image that checks Rust katas: cargo test today, clippy and rustfmt for the review layer (M5).
FROM rust:1.100-slim

RUN rustup component add clippy rustfmt

# Checks run as an unprivileged user; build output goes to a cached volume at /target.
RUN useradd --create-home --uid 10001 dojo \
    && mkdir /target \
    && chown dojo /target

ENV CARGO_TARGET_DIR=/target \
    CARGO_TERM_COLOR=never

USER dojo
WORKDIR /work

CMD ["cargo", "test", "--offline", "--no-fail-fast", "--color", "never"]
