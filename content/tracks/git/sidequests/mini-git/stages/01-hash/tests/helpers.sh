# Shared by the tests of every stage (sourced by their setup).
bats_load_library bats-support
bats_load_library bats-assert
bats_load_library bats-file

export GIT_CONFIG_GLOBAL="$BATS_TEST_TMPDIR/gitconfig" GIT_CONFIG_NOSYSTEM=1
git config --global user.name "Dojo Tester"
git config --global user.email "tester@dojo.test"
git config --global init.defaultBranch main
git config --global advice.detachedHead false
git config --global gc.auto 0

MINIGIT="$BATS_TEST_DIRNAME/../minigit.sh"
repo="$BATS_TEST_TMPDIR/repo"
git init -q "$repo"
day=0

# The git the script sees: only the plumbing a command may use runs the real Git; every call
# is recorded in $calls, with what was sent to cat-file --batch.
REAL_GIT=$(command -v git)
shim="$BATS_TEST_TMPDIR/shim"
calls="$BATS_TEST_TMPDIR/calls.log"
mkdir -p "$shim"
: > "$calls"
cat > "$shim/git" << SHIM
#!/bin/bash
args=("\$@")
i=0
while [ "\$i" -lt "\${#args[@]}" ]; do
    case "\${args[\$i]}" in
        -C | -c | --git-dir | --work-tree | --namespace) i=\$((i + 2)) ;;
        -*) i=\$((i + 1)) ;;
        *) break ;;
    esac
done
sub=\${args[\$i]:-}
echo "git \$*" >> "$calls"
case " \$MINIGIT_ALLOWED " in
    *" \$sub "*) ;;
    *)
        echo "FORBIDDEN git \$sub" >> "$calls"
        echo "minigit tests: git \$sub is not allowed here" >&2
        exit 99
        ;;
esac
case " \$* " in
    *" --batch"* | *" --stdin "*)
        tee -a "$calls" | "$REAL_GIT" "\$@"
        exit "\${PIPESTATUS[1]}"
        ;;
esac
exec "$REAL_GIT" "\$@"
SHIM
chmod +x "$shim/git"

# minigit COMMAND ARGS: runs the script with the plumbing COMMAND may use.
minigit() {
    local allowed=""
    case "${1:-}" in
        snapshot) allowed="hash-object mktree commit-tree update-ref rev-parse cat-file" ;;
        log | ls-tree | diff) allowed="cat-file rev-parse" ;;
        unreachable) allowed="cat-file rev-parse for-each-ref show-index ls-files" ;;
    esac
    env MINIGIT_ALLOWED="$allowed" PATH="$shim:$PATH" bash "$MINIGIT" "$@"
}

# no_porcelain: fails when the script called a git subcommand it may not use.
no_porcelain() {
    if grep '^FORBIDDEN' "$calls"; then return 1; fi
}

# put FILE TEXT: writes TEXT (printf format) into FILE under REPO, creating folders.
put() {
    mkdir -p "$(dirname "$repo/$1")"
    # shellcheck disable=SC2059
    printf -- "$2" > "$repo/$1"
}

# commit MESSAGE: stages everything and commits it, one day after the previous commit.
commit() {
    day=$((day + 1))
    local date
    date=$(printf '2024-07-%02dT12:00:00Z' "$day")
    git -C "$repo" add -A
    GIT_AUTHOR_DATE=$date GIT_COMMITTER_DATE=$date git -C "$repo" commit -q --allow-empty -m "$1"
}

# git_tree DIR: the tree id the real Git gives the files in DIR.
git_tree() {
    local scratch="$BATS_TEST_TMPDIR/scratch-$RANDOM"
    git init -q "$scratch"
    (cd "$1" && tar --exclude=.git -cf - .) | tar -xf - -C "$scratch"
    git -C "$scratch" add -A
    git -C "$scratch" write-tree
    rm -rf "$scratch"
}
