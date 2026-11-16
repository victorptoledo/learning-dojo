# Image that checks React katas: TypeScript 7 in strict mode with JSX, React 19 rendered into a
# jsdom document and tested with Testing Library and node:test, offline. tsc type-checks the
# .ts/.tsx files and compiles them to JavaScript, then node:test runs the compiled tests.
# Prints results in the `cargo test` format the dojo already reads (see report.mjs).
FROM node:26-trixie-slim

# Installed once at build time: checks run with --network none.
WORKDIR /opt/dojo
COPY package.json ./
RUN npm install --no-audit --no-fund \
    && npm cache clean --force

COPY check.sh report.mjs cargo-reporter.mjs setup-dom.mjs tsconfig.dojo.json /opt/dojo/
RUN chmod 755 /opt/dojo/check.sh \
    && useradd --create-home --uid 10001 dojo \
    && mkdir /target \
    && chown dojo /target

ENV LANG=C.UTF-8 \
    TZ=UTC \
    NODE_ENV=test \
    NO_COLOR=1 \
    COLORS=false \
    NODE_OPTIONS=--max-old-space-size=768

USER dojo
WORKDIR /work

CMD ["/opt/dojo/check.sh"]
