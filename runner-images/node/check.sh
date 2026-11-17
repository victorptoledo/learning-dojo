#!/bin/bash
# Checks a TypeScript kata in /work: the code must type-check with the dojo's strict settings,
# each "// test:" section of tests/*.types.ts is a type-level test, and tests/*.test.ts run
# with node:test. Prints the result in `cargo test` format (see report.mjs).
set -u

out=/target
rm -rf "${out:?}"/*

# Everything runs on a copy, so code that writes or deletes files cannot harm /work.
cp -r /work "$out/kata"
cd "$out/kata" || exit 1

cp /opt/dojo/tsconfig.dojo.json tsconfig.dojo.json
if [ ! -f package.json ]; then
    echo '{ "type": "module" }' > package.json
fi
# The packages preinstalled in the image (typescript, @types/node, zod): no network needed.
# With "workspaces" in package.json, each workspace package is linked under its name too.
rm -rf node_modules
node /opt/dojo/link-workspaces.mjs || exit 1

exec node /opt/dojo/report.mjs
