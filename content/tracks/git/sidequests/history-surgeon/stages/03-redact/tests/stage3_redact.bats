setup() {
    source "$BATS_TEST_DIRNAME/helpers.sh"
    words="$BATS_TEST_TMPDIR/leaked.txt"
}

# grep_history TEXT: the commits of any branch or tag where some file holds TEXT.
grep_history() {
    # shellcheck disable=SC2046
    git -C "$repo" grep -F -l -e "$1" $(git -C "$repo" rev-list --branches --tags) || true
}

@test "redact: every version on every branch, and secrets is clean afterwards" {
    put README.md "# Shop\n"
    commit "readme"
    put config/prod.env "DB=shop\nAWS_ACCESS_KEY_ID=$AWS_KEY\n"
    commit "config"
    put config/prod.env "DB=shop2\nAWS_ACCESS_KEY_ID=$AWS_KEY\n"
    commit "db"
    git -C "$repo" switch -q -c ops
    put ops/deploy.sh "export GITHUB_TOKEN=$GH_TOKEN # temporary\n"
    commit "deploy"
    git -C "$repo" switch -q main
    printf '%s\n' "$AWS_KEY" "$GH_TOKEN" > "$words"
    run --separate-stderr scrub redact "$repo" "$words"
    assert_success
    assert_output "redacted 3 file versions
rewrote 3 commits, dropped 0"
    [ -z "$(grep_history "$AWS_KEY")" ]
    [ -z "$(grep_history "$GH_TOKEN")" ]
    [ "$(git -C "$repo" show main:config/prod.env)" = "DB=shop2
AWS_ACCESS_KEY_ID=***REMOVED***" ]
    [ "$(git -C "$repo" show main~1:config/prod.env)" = "DB=shop
AWS_ACCESS_KEY_ID=***REMOVED***" ]
    [ "$(git -C "$repo" show ops:ops/deploy.sh)" = "export GITHUB_TOKEN=***REMOVED*** # temporary" ]
    [ "$(cat "$repo/config/prod.env")" = "DB=shop2
AWS_ACCESS_KEY_ID=***REMOVED***" ]
    run scrub secrets "$repo"
    assert_success
    assert_output ""
}

@test "redact: blobs and commits without the strings keep their ids" {
    put README.md "# Shop\n"
    put src/app.txt "app\n"
    commit "start"
    put src/app.txt "app 2\n"
    commit "app 2"
    put .env "PASSWORD=hunter2\n"
    commit "env"
    put src/app.txt "app 3\n"
    commit "app 3"
    start=$(id main~3)
    app2=$(id main~2)
    readme=$(id main:README.md)
    app3=$(id main:src/app.txt)
    msg=$(git -C "$repo" log -1 --format=%B main)
    printf 'hunter2\n' > "$words"
    run scrub redact "$repo" "$words"
    assert_success
    assert_output "redacted 1 file versions
rewrote 2 commits, dropped 0"
    [ "$(id main~3)" = "$start" ]
    [ "$(id main~2)" = "$app2" ]
    [ "$(id main:README.md)" = "$readme" ]
    [ "$(id main:src/app.txt)" = "$app3" ]
    [ "$(git -C "$repo" log -1 --format=%B main)" = "$msg" ]
    [ "$(git -C "$repo" log -1 --format='%an %at %cn %ct' main)" = "Ana Souza $(date -d 2024-05-04T12:00:00Z +%s) Dojo Tester $(date -d 2024-05-04T12:00:00Z +%s)" ]
}

@test "redact: literal strings, CRLF and empty lines in the list, bytes kept around them" {
    put notes.txt "pass: p@ss.w0rd/[x]* and pXssAw0rd/x and & done"
    printf 'bin\0\1\2hunter2\0tail' > "$repo/data.bin"
    commit "notes"
    printf 'p@ss.w0rd/[x]*\r\n\nhunter2\r\n& \n' > "$words"
    run scrub redact "$repo" "$words"
    assert_success
    assert_output "redacted 2 file versions
rewrote 1 commits, dropped 0"
    git -C "$repo" cat-file blob main:notes.txt > "$BATS_TEST_TMPDIR/notes"
    printf 'pass: ***REMOVED*** and pXssAw0rd/x and ***REMOVED***done' > "$BATS_TEST_TMPDIR/expected"
    cmp "$BATS_TEST_TMPDIR/notes" "$BATS_TEST_TMPDIR/expected"
    git -C "$repo" cat-file blob main:data.bin > "$BATS_TEST_TMPDIR/data"
    printf 'bin\0\1\2***REMOVED***\0tail' > "$BATS_TEST_TMPDIR/expected"
    cmp "$BATS_TEST_TMPDIR/data" "$BATS_TEST_TMPDIR/expected"
}

@test "redact: merges and tags are kept and no commit is dropped" {
    put a.txt "key=hunter2\n"
    commit "one"
    git -C "$repo" tag -a -m "first" v1
    git -C "$repo" switch -q -c topic
    put a.txt "key=hunter3\n"
    commit "rotate"
    git -C "$repo" switch -q main
    put b.txt "b\n"
    commit "b"
    git -C "$repo" merge -q --no-ff -m "merge topic" topic
    printf 'hunter2\nhunter3\n' > "$words"
    run scrub redact "$repo" "$words"
    assert_success
    assert_output "redacted 2 file versions
rewrote 4 commits, dropped 0"
    [ "$(git -C "$repo" rev-list --count main)" -eq 4 ]
    [ "$(git -C "$repo" rev-list --count --merges main)" -eq 1 ]
    [ "$(git -C "$repo" rev-parse 'v1^{commit}')" = "$(git -C "$repo" rev-list --max-parents=0 main)" ]
    [ "$(git -C "$repo" tag -l --format='%(contents)' v1)" = "first" ]
    [ "$(git -C "$repo" show v1:a.txt)" = "key=***REMOVED***" ]
}

@test "redact: nothing to replace changes nothing" {
    put a.txt "clean\n"
    commit "one"
    printf 'hunter2\n' > "$words"
    before=$(state)
    run scrub redact "$repo" "$words"
    assert_success
    assert_output "redacted 0 file versions
rewrote 0 commits, dropped 0"
    [ "$(state)" = "$before" ]
}

@test "redact: refusals" {
    put a.txt "key=hunter2\n"
    commit "one"
    run --separate-stderr scrub redact "$repo" "$BATS_TEST_TMPDIR/missing.txt"
    assert_failure 1
    [ "$stderr" = "scrub.sh: no strings in $BATS_TEST_TMPDIR/missing.txt" ]
    printf '\n\r\n' > "$words"
    run --separate-stderr scrub redact "$repo" "$words"
    assert_failure 1
    [ "$stderr" = "scrub.sh: no strings in $words" ]
    printf 'hunter2\n' > "$words"
    put a.txt "edited\n"
    before=$(state)
    run --separate-stderr scrub redact "$repo" "$words"
    assert_failure 1
    [ "$stderr" = "scrub.sh: uncommitted changes" ]
    [ "$(state)" = "$before" ]
    run --separate-stderr scrub redact "$repo"
    assert_failure 2
    run --separate-stderr scrub redact "$repo" "$words" extra
    assert_failure 2
}
