#!/usr/bin/env bash
# Usage: publish.sh VERSION - uploads the release artefacts; reads DEPLOY_TOKEN from the
# environment and never prints it.
set -euo pipefail

version=${1:?usage: publish.sh VERSION}
: "${DEPLOY_TOKEN:?DEPLOY_TOKEN is not set}"
echo "publishing $version"
