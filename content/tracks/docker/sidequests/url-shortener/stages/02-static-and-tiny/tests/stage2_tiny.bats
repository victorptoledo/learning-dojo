load /opt/dojo/facts

@test "the image is at most 20 MB" {
    size=$(image_size_mb)
    [ "$size" -le 20 ] || fail "the image is $size MB"
}

@test "there is no shell in the image" {
    for shell in /bin/sh /bin/ash /bin/bash /bin/busybox /usr/bin/sh /usr/bin/bash; do
        ! image_has_path "$shell" || fail "$shell is in the image"
    done
}

@test "there is no package manager in the image" {
    for tool in /sbin/apk /usr/bin/apt /usr/bin/apt-get /usr/bin/dpkg /usr/bin/yum /usr/bin/microdnf; do
        ! image_has_path "$tool" || fail "$tool is in the image"
    done
}

@test "there is no C library: the binary is static" {
    found=$(image_files | grep -E '(ld-musl|libc\.musl|libc\.so)' | head -n 3 | tr '\n' ' ')
    [ -z "$found" ] || fail "found a C library: $found"
}
