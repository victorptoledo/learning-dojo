setup() {
    source "$BATS_TEST_DIRNAME/helpers.sh"
    make_image
}

# A fingerprint of everything under etc and home: names, modes and contents.
snapshot() {
    (cd "$R" && find etc home -printf '%p %m\n' | LC_ALL=C sort && find etc -type f -exec md5sum {} + | LC_ALL=C sort)
}

@test "adds a user, its private group and a locked shadow entry" {
    run --separate-stderr accounts add bob
    assert_success
    assert_output "added bob (uid 1001, gid 1001)"
    [ -z "$stderr" ]
    [ "$(grep '^bob:' "$R/etc/passwd")" = "bob:x:1001:1001::/home/bob:/bin/bash" ]
    [ "$(grep '^bob:' "$R/etc/group")" = "bob:x:1001:" ]
    [ "$(grep '^bob:' "$R/etc/shadow")" = "bob:!:$(( $(date +%s) / 86400 )):0:99999:7:::" ]
    run accounts check
    assert_success
    assert_output "ok: 7 users, 9 groups"
}

@test "creates the home from the skeleton" {
    run accounts add bob
    assert_success
    home="$R/home/bob"
    [ "$(mode "$home")" = 700 ]
    [ "$(cat "$home/.bashrc")" = "# ~/.bashrc" ]
    [ "$(cat "$home/.profile")" = "# ~/.profile" ]
    [ "$(cat "$home/.config/settings")" = "theme=dark" ]
    [ "$(mode "$home/.bashrc")" = 644 ]
    [ "$(mode "$home/.config/settings")" = 600 ]
}

@test "takes a comment, a shell and extra groups" {
    run accounts add -c "Bob Builder" -s /bin/sh -G sudo,users bob
    assert_success
    [ "$(grep '^bob:' "$R/etc/passwd")" = "bob:x:1001:1001:Bob Builder:/home/bob:/bin/sh" ]
    [ "$(grep '^sudo:' "$R/etc/group")" = "sudo:x:27:alice,bob" ]
    [ "$(grep '^users:' "$R/etc/group")" = "users:x:100:bob" ]
    run accounts list
    assert_output "alice uid=1000 group=alice groups=sudo shell=/bin/bash
bob uid=1001 group=bob groups=sudo,users shell=/bin/sh"
}

@test "picks the smallest free uid and gid" {
    echo "carol:x:1001:100::/home/carol:/bin/bash" >> "$R/etc/passwd"
    echo "dave:x:1003:100::/home/dave:/bin/bash" >> "$R/etc/passwd"
    printf '%s\n' 'carol:!:20400:0:99999:7:::' 'dave:!:20400:0:99999:7:::' >> "$R/etc/shadow"
    echo "devs:x:1002:" >> "$R/etc/group"
    run accounts add bob
    assert_success
    assert_output "added bob (uid 1002, gid 1001)"
    [ "$(field passwd bob 4)" = 1001 ]
    [ "$(grep '^bob:' "$R/etc/group")" = "bob:x:1001:" ]
}

@test "keeps the modes, leaves backups and no temporary files" {
    old_passwd=$(cat "$R/etc/passwd")
    old_shadow=$(cat "$R/etc/shadow")
    run accounts add bob
    assert_success
    [ "$(mode "$R/etc/shadow")" = 640 ]
    [ "$(mode "$R/etc/passwd")" = 644 ]
    [ "$(cat "$R/etc/passwd-")" = "$old_passwd" ]
    [ "$(cat "$R/etc/shadow-")" = "$old_shadow" ]
    [ -z "$(find "$R/etc" -name '*+')" ]
}

@test "refusals change nothing" {
    mkdir "$R/home/zoe"
    before=$(snapshot)
    run --separate-stderr accounts add alice
    assert_failure 1
    [ "$stderr" = "accounts.sh: user alice already exists" ]
    run --separate-stderr accounts add users
    assert_failure 1
    [ "$stderr" = "accounts.sh: group users already exists" ]
    run --separate-stderr accounts add -s /bin/zsh bob
    assert_failure 1
    [ "$stderr" = "accounts.sh: /bin/zsh is not in /etc/shells" ]
    run --separate-stderr accounts add -G sudo,wheel bob
    assert_failure 1
    [ "$stderr" = "accounts.sh: no such group: wheel" ]
    run --separate-stderr accounts add zoe
    assert_failure 1
    [ "$stderr" = "accounts.sh: /home/zoe already exists" ]
    run --separate-stderr accounts add Bob
    assert_failure 2
    [ "$stderr" = "accounts.sh: bad user name: Bob" ]
    run --separate-stderr accounts add -c "a:b" bob
    assert_failure 2
    [ "$stderr" = "accounts.sh: bad comment: a:b" ]
    [ "$(snapshot)" = "$before" ]
}

@test "a comment is not a shell: a commented shell line does not count" {
    echo "#/bin/zsh" >> "$R/etc/shells"
    run --separate-stderr accounts add -s "#/bin/zsh" bob
    assert_failure 1
    [ "$stderr" = "accounts.sh: #/bin/zsh is not in /etc/shells" ]
}
