setup() {
    bats_load_library bats-support
    bats_load_library bats-assert
    bats_load_library bats-file
}

@test "a server ready to go live passes every check" {
    root="$BATS_TEST_TMPDIR/server"
    mkdir -p "$root/etc/ssh/sshd_config.d"
    printf '%s\n' 'Include /etc/ssh/sshd_config.d/*.conf' 'PermitRootLogin no' \
        'PasswordAuthentication no' 'KbdInteractiveAuthentication no' 'UsePAM yes' > "$root/etc/ssh/sshd_config"
    printf '%s\n' 'table inet filter {' '    chain input {' \
        '        type filter hook input priority 0; policy drop;' \
        '        ct state established,related accept' '        tcp dport { 22, 443 } accept' '    }' '}' > "$root/etc/nftables.conf"
    printf '%s\n' 'UUID=1111-aaaa  /     ext4  errors=remount-ro  0 1' \
        '/dev/mapper/vg0-srv  /srv  ext4  defaults,nofail  0 2' > "$root/etc/fstab"
    printf '%s\n' 'LISTEN 0 4096 0.0.0.0:22 0.0.0.0:*' 'LISTEN 0 511 0.0.0.0:443 0.0.0.0:*' \
        'LISTEN 0 200 127.0.0.1:5432 0.0.0.0:*' > "$BATS_TEST_TMPDIR/ss.txt"
    run bash preflight.sh -r "$root" -s "$BATS_TEST_TMPDIR/ss.txt"
    assert_success
    assert_output "PASS ssh: PermitRootLogin no
PASS ssh: PasswordAuthentication no
PASS ssh: KbdInteractiveAuthentication no
PASS firewall: input policy drop
PASS port: only allowed ports are public
PASS fstab: stable names and nofail on data disks
summary: 0 FAIL, 0 WARN"
}
