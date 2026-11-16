# Image that checks Frontend katas: the learner's HTML, CSS and JavaScript load in a real
# headless Chromium (Playwright), served from 127.0.0.1 inside the container, and node:test
# tests drive the page. axe-core checks accessibility and html-validate checks the HTML.
# Everything is installed at build time: checks run with --network none.
# Prints results in the `cargo test` format the dojo already reads (see report.mjs).
# `serve` starts the same static server on port 8080 so a learner can open the kata in a browser.
FROM node:26-trixie-slim

ENV PLAYWRIGHT_BROWSERS_PATH=/opt/ms-playwright

WORKDIR /opt/dojo
COPY package.json ./
RUN npm install --no-audit --no-fund \
    && npx playwright-core install --with-deps --only-shell chromium \
    && npm cache clean --force \
    && rm -rf /var/lib/apt/lists/* /root/.cache

COPY check.sh serve.sh report.mjs cargo-reporter.mjs /opt/dojo/
COPY dojo-web/ /opt/dojo/node_modules/dojo-web/
RUN chmod 755 /opt/dojo/check.sh /opt/dojo/serve.sh \
    && ln -s /opt/dojo/serve.sh /usr/local/bin/serve \
    && useradd --create-home --uid 10001 dojo \
    && mkdir /target \
    && chown dojo /target

ENV LANG=C.UTF-8 \
    TZ=UTC \
    NODE_ENV=test \
    NO_COLOR=1 \
    NODE_OPTIONS=--max-old-space-size=512

USER dojo
WORKDIR /work

CMD ["/opt/dojo/check.sh"]
