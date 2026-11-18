setup() {
    source "$BATS_TEST_DIRNAME/helpers.sh"
    make_image
    lock="$R/etc/.pwd.lock"
}

@test "lock and unlock keep the password hash" {
    hash=$(field shadow alice 2)
    run accounts lock alice
    assert_success
    assert_output "locked alice"
    [ "$(field shadow alice 2)" = "!$hash" ]
    run accounts lock alice
    assert_success
    assert_output "alice is already locked"
    [ "$(field shadow alice 2)" = "!$hash" ]
    run accounts unlock alice
    assert_success
    assert_output "unlocked alice"
    [ "$(field shadow alice 2)" = "$hash" ]
    run accounts unlock alice
    assert_success
    assert_output "alice is not locked"
    [ "$(mode "$R/etc/shadow")" = 640 ]
}

@test "unlock refuses to leave an empty password" {
    run accounts add bob
    assert_success
    run --separate-stderr accounts unlock bob
    assert_failure 1
    [ "$stderr" = "accounts.sh: unlocking bob would leave an empty password" ]
    [ "$(field shadow bob 2)" = "!" ]
}

@test "lock, unlock and del refuse unknown users" {
    for command in lock unlock del; do
        run --separate-stderr accounts "$command" zed
        assert_failure 1
        [ "$stderr" = "accounts.sh: no such user: zed" ]
    done
}

@test "del removes the user, its private group and its memberships" {
    run accounts add -G sudo,users bob
    assert_success
    run accounts del bob
    assert_success
    assert_output "deleted bob"
    run grep -c '^bob:' "$R/etc/passwd" "$R/etc/shadow" "$R/etc/group"
    assert_output "$R/etc/passwd:0
$R/etc/shadow:0
$R/etc/group:0"
    [ "$(grep '^sudo:' "$R/etc/group")" = "sudo:x:27:alice" ]
    [ "$(grep '^users:' "$R/etc/group")" = "users:x:100:" ]
    [ -d "$R/home/bob" ]
    run accounts check
    assert_success
    assert_output "ok: 6 users, 8 groups"
}

@test "del -r removes the home too" {
    run accounts add bob
    assert_success
    run accounts del -r bob
    assert_success
    [ ! -e "$R/home/bob" ]
    [ -d "$R/home/alice" ]
}

@test "del -r never removes a home outside /home" {
    mkdir -p "$R/srv/data"
    echo "web:x:1500:100::/srv/data:/bin/sh" >> "$R/etc/passwd"
    echo 'web:!:20400:0:99999:7:::' >> "$R/etc/shadow"
    run --separate-stderr accounts del -r web
    assert_success
    assert_output "deleted web"
    [ "$stderr" = "accounts.sh: not removing /srv/data: not under /home" ]
    [ -d "$R/srv/data" ]
}

@test "keeps a private group that another user still uses" {
    run accounts add bob
    assert_success
    echo "carol:x:1700:1001::/home/carol:/bin/bash" >> "$R/etc/passwd"
    echo 'carol:!:20400:0:99999:7:::' >> "$R/etc/shadow"
    run accounts del bob
    assert_success
    [ "$(grep '^bob:' "$R/etc/group")" = "bob:x:1001:" ]
}

@test "del refuses system accounts" {
    for name in root daemon nobody; do
        run --separate-stderr accounts del "$name"
        assert_failure 1
        [ "$stderr" = "accounts.sh: $name is a system account" ]
    done
    run accounts check
    assert_success
}

@test "a lock held by a live process stops a writer" {
    echo "$$" > "$lock"
    start=$SECONDS
    ACCOUNTS_LOCK_WAIT=0.5 run --separate-stderr accounts add bob
    assert_failure 1
    [ "$stderr" = "accounts.sh: account files are locked by pid $$" ]
    [ $((SECONDS - start)) -lt 4 ]
    [ "$(cat "$lock")" = "$$" ]
    ! grep -q '^bob:' "$R/etc/passwd"
}

@test "a writer waits until the lock is released" {
    echo "$$" > "$lock"
    (sleep 0.7; rm -f "$lock") 3>&- &
    run --separate-stderr accounts add bob
    assert_success
    assert_output "added bob (uid 1001, gid 1001)"
    [ ! -e "$lock" ]
}

@test "a stale lock is removed" {
    echo 999999 > "$lock"
    run --separate-stderr accounts lock alice
    assert_success
    assert_output "locked alice"
    [ "$stderr" = "accounts.sh: removing stale lock of pid 999999" ]
    [ ! -e "$lock" ]
}

@test "the lock is released after a refusal" {
    run accounts add alice
    assert_failure 1
    [ ! -e "$lock" ]
    run accounts unlock alice
    assert_success
    [ ! -e "$lock" ]
}

@test "eight adds at the same moment get eight different uids" {
    for i in 1 2 3 4 5 6 7 8; do
        accounts add "user$i" > "$BATS_TEST_TMPDIR/out.$i" 2>&1 &
    done
    failed=0
    for job in $(jobs -p); do
        wait "$job" || failed=$((failed + 1))
    done
    cat "$BATS_TEST_TMPDIR"/out.* >&2
    [ "$failed" -eq 0 ]
    [ "$(grep -c '^user' "$R/etc/passwd")" -eq 8 ]
    [ "$(grep -c '^user' "$R/etc/shadow")" -eq 8 ]
    [ "$(grep '^user' "$R/etc/passwd" | cut -d: -f3 | sort -u | wc -l)" -eq 8 ]
    run accounts check
    assert_success
    assert_output "ok: 14 users, 16 groups"
}
