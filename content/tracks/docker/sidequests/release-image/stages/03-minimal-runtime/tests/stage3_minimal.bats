load /opt/dojo/facts

preflight() {
    cmd_out preflight | sed -n "s/^$1=//p"
}

@test "no shell and no package manager in the image" {
    for path in /bin/sh /bin/ash /bin/bash /bin/busybox /usr/bin/sh /sbin/apk /usr/bin/apt-get /usr/bin/dpkg; do
        ! image_has_path "$path" || fail "$path is in the image"
    done
}

@test "the image is at most 20 MB" {
    size=$(image_size_mb)
    [ "$size" -le 20 ] || fail "the image is $size MB"
}

@test "TLS works: the CA certificates are in the image" {
    run preflight ca_certs
    assert_output "ok"
}

@test "time zones work: America/Sao_Paulo loads" {
    run preflight zone
    assert_output "ok"
}

@test "there is a writable /tmp" {
    run preflight tmp_writable
    assert_output "true"
}
