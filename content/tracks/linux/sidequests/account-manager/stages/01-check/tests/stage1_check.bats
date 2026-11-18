setup() {
    source "$BATS_TEST_DIRNAME/helpers.sh"
    make_image
}

@test "a clean image passes the check" {
    run --separate-stderr accounts check
    assert_success
    assert_output "ok: 6 users, 8 groups"
    [ -z "$stderr" ]
}

@test "empty lines are ignored" {
    echo "" >> "$R/etc/passwd"
    echo "" >> "$R/etc/group"
    run accounts check
    assert_success
    assert_output "ok: 6 users, 8 groups"
}

@test "lists the human users with their groups" {
    echo "bob:x:1001:100:Bob:/home/bob:/bin/sh" >> "$R/etc/passwd"
    echo 'bob:!:20400:0:99999:7:::' >> "$R/etc/shadow"
    sed -i 's/^users:x:100:$/users:x:100:alice/' "$R/etc/group"
    run accounts list
    assert_success
    assert_output "alice uid=1000 group=alice groups=sudo,users shell=/bin/bash
bob uid=1001 group=users groups=- shell=/bin/sh"
}

@test "reports the problems of passwd, line by line" {
    cat >> "$R/etc/passwd" <<'END'
Bob:x:1002:1000::/home/Bob:/bin/bash
carol:x:1000:1000::/home/carol:/bin/bash
toor:x:0:0::/root:/bin/bash
dave:x:1003:1500::/home/dave:/bin/bash
alice:x:1004:1000::/home/alice2:/bin/bash
erin:x:1005:1000:/home/erin:/bin/bash
frank:x:12x:1000::/home/frank:/bin/bash
END
    run --separate-stderr accounts check
    assert_failure 1
    assert_output "passwd:7: bad user name: Bob
passwd:8: uid 1000 already used by alice
passwd:9: uid 0 for toor (only root may have uid 0)
passwd:10: gid 1500 of dave has no group
passwd:11: duplicate user: alice
passwd:12: expected 7 fields, got 6
passwd:13: bad uid: 12x
shadow: no entry for Bob
shadow: no entry for carol
shadow: no entry for toor
shadow: no entry for dave
shadow: no entry for frank"
}

@test "reports the problems of group" {
    cat >> "$R/etc/group" <<'END'
sudo:x:28:
devs:x:1000:
ops:x:2000:alice,zed
broken:x:3000
END
    run accounts check
    assert_failure 1
    assert_output "group:9: duplicate group: sudo
group:10: gid 1000 already used by alice
group:11: unknown member zed in ops
group:12: expected 4 fields, got 3"
}

@test "reports the problems of shadow and the users it lacks" {
    sed -i -e '/^www-data:/d' -e 's/^alice:[^:]*:/alice::/' "$R/etc/shadow"
    echo 'ghost:*:20400:0:99999:7:::' >> "$R/etc/shadow"
    echo 'short:x:1' >> "$R/etc/shadow"
    run accounts check
    assert_failure 1
    assert_output "shadow:5: empty password for alice
shadow:6: unknown user ghost
shadow:7: expected 9 fields, got 3
shadow: no entry for www-data"
}

@test "rejects bad arguments and missing files" {
    usage="usage: accounts.sh [--root DIR] check|list|add|lock|unlock|del|audit [options] [NAME]"
    run --separate-stderr accounts
    assert_failure 2
    [ "$stderr" = "$usage" ]
    run --separate-stderr accounts frobnicate
    assert_failure 2
    [ "$stderr" = "$usage" ]
    run --separate-stderr accounts check now
    assert_failure 2
    [ "$stderr" = "$usage" ]
    rm "$R/etc/shadow"
    run --separate-stderr accounts check
    assert_failure 2
    [ "$stderr" = "accounts.sh: cannot read $R/etc/shadow" ]
}
