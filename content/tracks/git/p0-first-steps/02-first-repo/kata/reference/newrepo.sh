#!/usr/bin/env bash
# Usage: newrepo.sh DIR - creates a repository with a README, a .gitignore and a first commit.
set -euo pipefail

if [ "$#" -ne 1 ]; then
    echo "usage: newrepo.sh DIR" >&2
    exit 2
fi
dir="$1"
if [ -e "$dir/.git" ]; then
    echo "newrepo.sh: $dir is already a git repository" >&2
    exit 1
fi

mkdir -p "$dir"
git init -q -b main "$dir"
name=$(basename "$dir")
if [ ! -e "$dir/README.md" ]; then
    echo "# $name" > "$dir/README.md"
fi
printf '%s\n' '*.log' '.env' > "$dir/.gitignore"
git -C "$dir" add README.md .gitignore
git -C "$dir" commit -q -m "Initial commit"
echo "created $dir on branch main"
