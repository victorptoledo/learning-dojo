#!/bin/bash
# Serves a kata folder so you can open it in your own browser, with the same server the checks use.
# From a kata folder:  docker run --rm -p 8080:8080 -v "${PWD}:/work" dojo-web serve
# Then open http://localhost:8080/ and press Ctrl+C to stop.
set -u
exec node /opt/dojo/node_modules/dojo-web/serve.mjs /work 8080
