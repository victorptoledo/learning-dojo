# Shared by the tests of every stage (sourced by their setup).
bats_load_library bats-support
bats_load_library bats-assert
bats_load_library bats-file

ACCOUNTS="$BATS_TEST_DIRNAME/../accounts.sh"
R="$BATS_TEST_TMPDIR/image"

accounts() {
    timeout 30 bash "$ACCOUNTS" --root "$R" "$@" 3>&-
}

# A small Debian 13 image: root, three system accounts, nobody and alice.
make_image() {
    mkdir -p "$R/etc/skel/.config" "$R/home/alice"
    cat > "$R/etc/passwd" <<'END'
root:x:0:0:root:/root:/bin/bash
daemon:x:1:1:daemon:/usr/sbin:/usr/sbin/nologin
www-data:x:33:33:www-data:/var/www:/usr/sbin/nologin
backup:x:34:34:backup:/var/backups:/usr/sbin/nologin
nobody:x:65534:65534:nobody:/nonexistent:/usr/sbin/nologin
alice:x:1000:1000:Alice Liddell:/home/alice:/bin/bash
END
    cat > "$R/etc/group" <<'END'
root:x:0:
daemon:x:1:
sudo:x:27:alice
www-data:x:33:
backup:x:34:
users:x:100:
nogroup:x:65534:
alice:x:1000:
END
    cat > "$R/etc/shadow" <<'END'
root:*:20400:0:99999:7:::
daemon:*:20400:0:99999:7:::
www-data:*:20400:0:99999:7:::
backup:*:20400:0:99999:7:::
nobody:*:20400:0:99999:7:::
alice:$y$j9T$Xk2Q0f1aV7$O3cG0yq1uQe6mBvZr2bnT8w1pQe0sLkJdHf4gVx9YzA:20400:0:99999:7:::
END
    cat > "$R/etc/shells" <<'END'
# /etc/shells: valid login shells
/bin/sh
/usr/bin/sh
/bin/bash
/usr/bin/bash
/usr/bin/dash
END
    echo "# ~/.bashrc" > "$R/etc/skel/.bashrc"
    echo "# ~/.profile" > "$R/etc/skel/.profile"
    echo "theme=dark" > "$R/etc/skel/.config/settings"
    chmod 644 "$R/etc/passwd" "$R/etc/group" "$R/etc/skel/.bashrc" "$R/etc/skel/.profile"
    chmod 640 "$R/etc/shadow"
    chmod 600 "$R/etc/skel/.config/settings"
    chmod 700 "$R/home/alice"
}

# field FILE NAME N: field N of NAME's line in FILE.
field() {
    awk -F: -v name="$2" -v n="$3" '$1 == name { print $n }' "$R/etc/$1"
}

mode() {
    stat -c %a "$1"
}
