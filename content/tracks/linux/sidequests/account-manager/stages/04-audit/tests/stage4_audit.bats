setup() {
    source "$BATS_TEST_DIRNAME/helpers.sh"
    make_image
}

# human NAME UID SHELL HOME_MODE: a user with its group, a locked password and a home.
human() {
    echo "$1:x:$2:$2::/home/$1:$3" >> "$R/etc/passwd"
    echo "$1:x:$2:" >> "$R/etc/group"
    echo "$1:!:20400:0:99999:7:::" >> "$R/etc/shadow"
    mkdir -p "$R/home/$1"
    chmod "$4" "$R/home/$1"
}

@test "a clean image has no findings" {
    run accounts audit
    assert_success
    assert_output "audit: no findings"
}

@test "finds a second root, empty passwords, odd shells and system accounts that log in" {
    sed -i 's#^backup:x:34:34:backup:/var/backups:/usr/sbin/nologin$#backup:x:34:34:backup:/var/backups:/bin/bash#' "$R/etc/passwd"
    sed -i 's#^backup:\*:#backup:$y$j9T$abc$def:#' "$R/etc/shadow"
    echo 'toor:x:0:0::/root:/bin/bash' >> "$R/etc/passwd"
    echo 'toor:$y$j9T$abc$def:20400:0:99999:7:::' >> "$R/etc/shadow"
    human bob 1001 /bin/zsh 700
    sed -i 's/^bob:!:/bob::/' "$R/etc/shadow"
    run accounts audit
    assert_failure 1
    assert_output "backup: system account can log in
toor: uid 0
bob: empty password
bob: shell /bin/zsh is not in /etc/shells"
}

@test "a locked system account with a shell is fine" {
    sed -i 's#^backup:x:34:34:backup:/var/backups:/usr/sbin/nologin$#backup:x:34:34:backup:/var/backups:/bin/sh#' "$R/etc/passwd"
    sed -i 's#^backup:\*:#backup:!$y$j9T$abc$def:#' "$R/etc/shadow"
    human svc 1002 /bin/false 700
    run accounts audit
    assert_success
    assert_output "audit: no findings"
}

@test "checks homes, .ssh and world-writable files" {
    mkdir -p "$R/home/alice/.ssh" "$R/home/alice/projects"
    chmod 755 "$R/home/alice/.ssh"
    echo "ssh-ed25519 AAAA alice@laptop" > "$R/home/alice/.ssh/authorized_keys"
    chmod 664 "$R/home/alice/.ssh/authorized_keys"
    echo "todo" > "$R/home/alice/notes.txt"
    echo "echo hi" > "$R/home/alice/projects/run.sh"
    echo "safe" > "$R/home/alice/projects/safe.txt"
    chmod 666 "$R/home/alice/notes.txt"
    chmod 777 "$R/home/alice/projects/run.sh"
    chmod 644 "$R/home/alice/projects/safe.txt"
    human bob 1001 /bin/bash 700
    rmdir "$R/home/bob"
    human carol 1002 /bin/bash 755
    human dave 1003 /bin/bash 775
    run accounts audit
    assert_failure 1
    assert_output "alice: .ssh has mode 0755, expected 0700
alice: .ssh/authorized_keys is writable by group or others (0664)
alice: world-writable file notes.txt
alice: world-writable file projects/run.sh
bob: home /home/bob is missing
carol: home /home/carol is readable by others (0755)
dave: home /home/dave is writable by group or others (0775)"
}

@test "prints modes with their special bits" {
    human carol 1002 /bin/bash 2770
    human dave 1003 /bin/bash 701
    run accounts audit
    assert_failure 1
    assert_output "carol: home /home/carol is writable by group or others (2770)
dave: home /home/dave is readable by others (0701)"
}

@test "audit only reads, even while the files are locked" {
    echo "$$" > "$R/etc/.pwd.lock"
    human carol 1002 /bin/bash 755
    before=$( (cd "$R" && find . -printf '%p %m %s\n' | LC_ALL=C sort) )
    ACCOUNTS_LOCK_WAIT=0.2 run accounts audit
    assert_failure 1
    assert_output "carol: home /home/carol is readable by others (0755)"
    [ "$( (cd "$R" && find . -printf '%p %m %s\n' | LC_ALL=C sort) )" = "$before" ]
}
