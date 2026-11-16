setup() {
    bats_load_library bats-support
    bats_load_library bats-assert
    bats_load_library bats-file
    conf="$BATS_TEST_TMPDIR/sshd_config"
}

@test "prints the whole report of the example" {
    run bash audit.sh sshd_config.example
    assert_failure 1
    assert_output "HIGH PermitRootLogin yes: root may log in with a password
MEDIUM PasswordAuthentication yes (default): allow keys only
MEDIUM MaxAuthTries 10: allow at most 4 attempts
HIGH Ciphers aes128-cbc: weak algorithms
findings: 4 (high: 2)"
}

@test "a hardened file has no findings and exits 0" {
    cat > "$conf" <<'EOF'
PermitRootLogin no
PasswordAuthentication no
KbdInteractiveAuthentication no
MaxAuthTries 3
X11Forwarding no
Ciphers chacha20-poly1305@openssh.com,aes256-gcm@openssh.com
MACs hmac-sha2-512-etm@openssh.com,hmac-sha2-256-etm@openssh.com
EOF
    run bash audit.sh "$conf"
    assert_success
    assert_output "findings: 0 (high: 0)"
}

@test "an empty file reports the risky defaults" {
    : > "$conf"
    run bash audit.sh "$conf"
    assert_success
    assert_output "MEDIUM PasswordAuthentication yes (default): allow keys only
MEDIUM MaxAuthTries 6 (default): allow at most 4 attempts
findings: 2 (high: 0)"
}

@test "keywords and yes/no values are case-insensitive, and key=value works" {
    cat > "$conf" <<'EOF'
permitrootlogin=YES
  PERMITEMPTYPASSWORDS   =   Yes
passwordauthentication No
MaxAuthTries=2
x11forwarding	yes
EOF
    run bash audit.sh "$conf"
    assert_failure 1
    assert_output "HIGH PermitRootLogin YES: root may log in with a password
HIGH PermitEmptyPasswords Yes: accounts without a password may log in
LOW X11Forwarding yes: disable unless needed
findings: 3 (high: 2)"
}

@test "the first value wins" {
    cat > "$conf" <<'EOF'
PasswordAuthentication no
MaxAuthTries 4
PasswordAuthentication yes
MaxAuthTries 20
PermitRootLogin prohibit-password
PermitRootLogin yes
EOF
    run bash audit.sh "$conf"
    assert_success
    assert_output "findings: 0 (high: 0)"
}

@test "settings inside Match blocks are not global" {
    cat > "$conf" <<'EOF'
PasswordAuthentication no
MaxAuthTries 3
  match Address 10.0.0.0/8
    PermitRootLogin yes
    PermitEmptyPasswords yes
EOF
    run bash audit.sh "$conf"
    assert_success
    assert_output "findings: 0 (high: 0)"
}

@test "comments, blank lines and windows line ends are handled" {
    printf '# PermitRootLogin yes\r\n\r\n   # X11Forwarding yes\r\nPasswordAuthentication no\r\nMaxAuthTries 3\r\nX11Forwarding yes   \r\n' > "$conf"
    run bash audit.sh "$conf"
    assert_success
    assert_output "LOW X11Forwarding yes: disable unless needed
findings: 1 (high: 0)"
}

@test "MaxAuthTries must be a number from 1 to 4" {
    for value in 0 5 abc -1; do
        printf 'PasswordAuthentication no\nMaxAuthTries %s\n' "$value" > "$conf"
        run bash audit.sh "$conf"
        assert_output "MEDIUM MaxAuthTries $value: allow at most 4 attempts
findings: 1 (high: 0)"
    done
}

@test "weak ciphers and MACs list only the weak entries in file order" {
    cat > "$conf" <<'EOF'
PasswordAuthentication no
MaxAuthTries 3
Ciphers +3DES-CBC,aes256-gcm@openssh.com,arcfour256,aes128-ctr
MACs ^hmac-sha2-256,hmac-sha1,umac-64-etm@openssh.com,hmac-md5-96,hmac-sha1-etm@openssh.com,umac-128@openssh.com
EOF
    run bash audit.sh "$conf"
    assert_failure 1
    assert_output "HIGH Ciphers 3DES-CBC,arcfour256: weak algorithms
HIGH MACs hmac-sha1,umac-64-etm@openssh.com,hmac-md5-96,hmac-sha1-etm@openssh.com: weak algorithms
findings: 2 (high: 2)"
}

@test "removing algorithms is not a finding, and errors exit 2" {
    printf 'PasswordAuthentication no\nMaxAuthTries 3\nCiphers -aes128-cbc,3des-cbc\nMACs -hmac-sha1\n' > "$conf"
    run bash audit.sh "$conf"
    assert_success
    assert_output "findings: 0 (high: 0)"
    run --separate-stderr bash audit.sh
    assert_failure 2
    [ "$stderr" = "usage: audit.sh CONFIG" ]
    run --separate-stderr bash audit.sh "$BATS_TEST_TMPDIR/missing"
    assert_failure 2
    [ "$stderr" = "error: cannot read $BATS_TEST_TMPDIR/missing" ]
    assert_output ""
}
