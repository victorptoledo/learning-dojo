setup() {
    source "$BATS_TEST_DIRNAME/helpers.sh"
}

@test "big: versions of files over the size, at HEAD or only in history" {
    fill media/demo.mp4 3000 v
    fill vendor/libpay.a 2000 a
    put README.md "# Shop\n"
    commit "first"
    fill vendor/libpay.a 2500 b
    git -C "$repo" rm -q media/demo.mp4
    commit "new libpay, no demo"
    run --separate-stderr scrub big "$repo" --min-size 1000
    assert_success
    assert_output "$(printf '3000\tmedia/demo.mp4\thistory\n2500\tvendor/libpay.a\tHEAD\n2000\tvendor/libpay.a\thistory')"
}

@test "big: the default is 1 MiB" {
    fill a.bin 1048576 a
    fill b.bin 1048575 b
    commit "binaries"
    run scrub big "$repo"
    assert_success
    assert_output "$(printf '1048576\ta.bin\tHEAD')"
}

@test "big: one line per name, and every branch and tag" {
    fill same/one.bin 1500 s
    fill same/two.bin 1500 s
    commit "twins"
    git -C "$repo" switch -q -c media
    fill media/promo.mov 1800 m
    commit "promo"
    git -C "$repo" switch -q main
    git -C "$repo" switch -q -c old
    fill old/archive.zip 1700 z
    commit "archive"
    git -C "$repo" tag -a -m "old stuff" archive-tag
    git -C "$repo" switch -q main
    git -C "$repo" branch -q -D old
    run scrub big "$repo" --min-size 1000
    assert_success
    assert_output "$(printf '1800\tmedia/promo.mov\thistory\n1700\told/archive.zip\thistory\n1500\tsame/one.bin\tHEAD\n1500\tsame/two.bin\tHEAD')"
}

@test "big: equal sizes are sorted by path, then where" {
    fill b.bin 1200 x
    fill a.bin 1200 y
    commit "one"
    fill a.bin 1200 z
    commit "two"
    run scrub big "$repo" --min-size 1200
    assert_output "$(printf '1200\ta.bin\tHEAD\n1200\ta.bin\thistory\n1200\tb.bin\tHEAD')"
    run scrub big "$repo" --min-size 5000
    assert_success
    assert_output ""
}

@test "secrets: every kind, at HEAD or in history, and exit 1" {
    put config/prod.env "DB=shop\nAWS_ACCESS_KEY_ID=$AWS_KEY\n"
    put deploy/id_ed25519 "$KEY_HEADER\nabc\n-----END OPENSSH PRIVATE KEY-----\n"
    put src/app.txt "token: $GH_TOKEN\n"
    put src/clean.txt "nothing here: AKIA1234 ghp_short\n"
    commit "oops"
    put config/prod.env "DB=shop\nAWS_ACCESS_KEY_ID=\${AWS_ACCESS_KEY_ID}\n"
    git -C "$repo" rm -q src/app.txt
    commit "read the key from the environment"
    run --separate-stderr scrub secrets "$repo"
    assert_failure 1
    assert_output "$(printf 'aws-access-key\tconfig/prod.env\thistory\nprivate-key\tdeploy/id_ed25519\tHEAD\ngithub-token\tsrc/app.txt\thistory')"
}

@test "secrets: RSA and EC keys and keys without a type, once per line" {
    put keys/rsa.pem "-----BEGIN"" RSA PRIVATE KEY-----\nx\n"
    put keys/ec.pem "-----BEGIN"" EC PRIVATE KEY-----\nx\n"
    put keys/plain.pem "-----BEGIN"" PRIVATE KEY-----\nx\n"
    put keys/public.pem "-----BEGIN PUBLIC KEY-----\nx\n"
    put both.txt "$AWS_KEY\n$AWS_KEY\n$GH_TOKEN\n"
    commit "keys"
    run scrub secrets "$repo"
    assert_failure 1
    assert_output "$(printf 'aws-access-key\tboth.txt\tHEAD\ngithub-token\tboth.txt\tHEAD\nprivate-key\tkeys/ec.pem\tHEAD\nprivate-key\tkeys/plain.pem\tHEAD\nprivate-key\tkeys/rsa.pem\tHEAD')"
}

@test "secrets: a clean history exits 0" {
    put README.md "# Shop\nSet AWS_ACCESS_KEY_ID in your environment.\n"
    commit "docs"
    run --separate-stderr scrub secrets "$repo"
    assert_success
    assert_output ""
    git init -q "$BATS_TEST_TMPDIR/empty"
    run scrub secrets "$BATS_TEST_TMPDIR/empty"
    assert_success
    assert_output ""
    run scrub big "$BATS_TEST_TMPDIR/empty" --min-size 1
    assert_success
    assert_output ""
}

@test "big and secrets only read the repository" {
    put config/prod.env "AWS_ACCESS_KEY_ID=$AWS_KEY\n"
    fill big.bin 2000 b
    commit "one"
    put wip.txt "draft\n"
    before=$(state)
    run scrub secrets "$repo"
    run scrub big "$repo" --min-size 10
    [ "$(state)" = "$before" ]
}

@test "scrub rejects bad arguments" {
    usage="usage: scrub.sh big|secrets|purge|redact|undo|expire REPO [ARGS]"
    for args in "" "frobnicate $repo" "big" "secrets $repo extra" "big $repo --min-size" \
        "big $repo --min-size 1k" "big $repo --everything"; do
        # shellcheck disable=SC2086
        run --separate-stderr scrub $args
        assert_failure 2
        [ "$stderr" = "$usage" ]
    done
    mkdir "$BATS_TEST_TMPDIR/plain"
    run --separate-stderr scrub big "$BATS_TEST_TMPDIR/plain"
    assert_failure 2
    [ "$stderr" = "scrub.sh: not a git repository: $BATS_TEST_TMPDIR/plain" ]
}
