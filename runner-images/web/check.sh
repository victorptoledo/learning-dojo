#!/bin/bash
# Checks a Frontend kata in /work: tests/**/*.test.js run with node:test, open the kata's pages
# in headless Chromium through the dojo-web helper, and print the result in `cargo test` format.
set -u

out=/target
rm -rf "${out:?}"/*

# Everything runs on a copy, so code that writes or deletes files cannot harm /work.
cp -r /work "$out/kata"
cd "$out/kata" || exit 1

# The packages preinstalled in the image (playwright-core, axe-core, html-validate, dojo-web).
rm -rf node_modules
ln -s /opt/dojo/node_modules node_modules
if [ ! -f package.json ]; then
    echo '{ "type": "module" }' > package.json
fi

exec node /opt/dojo/report.mjs
