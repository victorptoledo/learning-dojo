setup() {
    bats_load_library bats-support
    bats_load_library bats-assert
    root=$BATS_TEST_TMPDIR/root
    mkdir -p "$root/etc"
    printf '%s\n' '127.0.0.1 localhost' > "$root/etc/hosts"
}

@test "a run reports every resource in order and a summary" {
    mkdir -p "$root/etc/app"
    chmod 750 "$root/etc/app"
    echo 'x' > "$root/etc/app/legacy.conf"
    run bash converge.sh "$root"
    assert_success
    assert_output "ok: dir /etc/app
changed: file /etc/app/app.conf
changed: line /etc/hosts
changed: absent /etc/app/legacy.conf
summary: 3 changed, 1 ok"
}

@test "the config file is rendered from the template and vars.env" {
    run bash converge.sh "$root"
    assert_success
    run cat "$root/etc/app/app.conf"
    assert_output "# managed by converge.sh: local edits are overwritten
listen = 0.0.0.0:8080
database = postgres://db.internal:5432/app
log_level = info"
}

@test "modes are 750 for the directory and 640 for the file" {
    run bash converge.sh "$root"
    assert_success
    run stat -c '%a' "$root/etc/app" "$root/etc/app/app.conf"
    assert_output "750
640"
}

@test "a second run changes nothing" {
    bash converge.sh "$root" > /dev/null
    before=$(find "$root" -printf '%p %m %s %T@\n' | sort)
    run bash converge.sh "$root"
    assert_success
    assert_output "ok: dir /etc/app
ok: file /etc/app/app.conf
ok: line /etc/hosts
ok: absent /etc/app/legacy.conf
summary: 0 changed, 4 ok"
    after=$(find "$root" -printf '%p %m %s %T@\n' | sort)
    [ "$before" = "$after" ] || { echo "the second run touched files"; return 1; }
}

@test "the hosts line is added once and the other lines are kept" {
    bash converge.sh "$root" > /dev/null
    bash converge.sh "$root" > /dev/null
    run cat "$root/etc/hosts"
    assert_output "127.0.0.1 localhost
10.0.0.5 db.internal"
}

@test "drift is repaired: an edited config and a wrong mode" {
    bash converge.sh "$root" > /dev/null
    echo 'log_level = debug' >> "$root/etc/app/app.conf"
    chmod 777 "$root/etc/app"
    run bash converge.sh "$root"
    assert_success
    assert_line "changed: dir /etc/app"
    assert_line "changed: file /etc/app/app.conf"
    assert_line "summary: 2 changed, 2 ok"
    run stat -c '%a' "$root/etc/app"
    assert_output "750"
    run grep -c 'log_level' "$root/etc/app/app.conf"
    assert_output "1"
}

@test "--check reports what would change, writes nothing and exits 3" {
    before=$(find "$root" -printf '%p %m %s\n' | sort)
    run bash converge.sh --check "$root"
    assert_failure 3
    assert_line "would change: dir /etc/app"
    assert_line "would change: file /etc/app/app.conf"
    assert_line "would change: line /etc/hosts"
    assert_line "summary: 3 to change, 1 ok"
    after=$(find "$root" -printf '%p %m %s\n' | sort)
    [ "$before" = "$after" ] || { echo "--check changed files"; return 1; }
}

@test "--check exits 0 on a converged server" {
    bash converge.sh "$root" > /dev/null
    run bash converge.sh --check "$root"
    assert_success
    assert_line "summary: 0 to change, 4 ok"
}

@test "an unknown resource kind is an error with its line number" {
    cp -r . "$BATS_TEST_TMPDIR/kata"
    printf '%s\n' 'package   nginx' >> "$BATS_TEST_TMPDIR/kata/desired.txt"
    run bash "$BATS_TEST_TMPDIR/kata/converge.sh" "$root"
    assert_failure 1
    assert_output --partial "converge.sh: line 7: unknown resource 'package'"
}

@test "wrong arguments print the usage and exit 2" {
    run bash converge.sh
    assert_failure 2
    assert_output "usage: converge.sh [--check] ROOT"
    run bash converge.sh --check
    assert_failure 2
}
