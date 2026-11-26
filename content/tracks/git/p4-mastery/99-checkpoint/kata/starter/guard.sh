#!/usr/bin/env bash
# Usage: install as hooks/pre-receive of a bare repository. Reads "OLD NEW REF" lines on stdin
# and rejects the whole push, listing every broken rule, when one of the lines breaks a rule.
echo "guard: todo: write guard.sh" >&2
exit 1
