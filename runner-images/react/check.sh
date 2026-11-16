#!/bin/bash
# Checks a React kata in /work: the .ts/.tsx code must type-check with the dojo's strict settings,
# each "// test:" section of tests/*.types.ts is a type-level test, and tests/*.test.tsx run
# with node:test and Testing Library in a jsdom document. Prints the result in `cargo test`
# format (see report.mjs).
set -u

out=/target
rm -rf "${out:?}"/*

# Everything runs on a copy, so code that writes or deletes files cannot harm /work.
cp -r /work "$out/kata"
cd "$out/kata" || exit 1

# The packages preinstalled in the image (react, react-dom, Testing Library, jsdom, typescript):
# no network needed.
rm -rf node_modules .out
ln -s /opt/dojo/node_modules node_modules
cp /opt/dojo/tsconfig.dojo.json tsconfig.dojo.json
if [ ! -f package.json ]; then
    echo '{ "type": "module" }' > package.json
fi

exec node /opt/dojo/report.mjs
