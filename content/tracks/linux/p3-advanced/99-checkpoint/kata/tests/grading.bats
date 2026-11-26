setup() {
    bats_load_library bats-support
    bats_load_library bats-assert
    bats_load_library bats-file
    server
}

# A server that passes everything; each test breaks one part of it.
server() {
    root="$BATS_TEST_TMPDIR/server"
    ss_file="$BATS_TEST_TMPDIR/ss.txt"
    mkdir -p "$root/etc/ssh/sshd_config.d"
    sshd_main 'PermitRootLogin no' 'PasswordAuthentication no' 'KbdInteractiveAuthentication no'
    cat > "$root/etc/nftables.conf" <<'NFT'
#!/usr/sbin/nft -f
flush ruleset

table inet filter {
    chain input {
        type filter hook input priority 0; policy drop;
        ct state established,related accept
        iif "lo" accept
        tcp dport { 22, 443 } accept
    }
    chain forward { type filter hook forward priority 0; policy drop; }
    chain output { type filter hook output priority 0; policy accept; }
}
NFT
    cat > "$root/etc/fstab" <<'FSTAB'
# /etc/fstab: static file system information.
UUID=1111-aaaa       /          ext4   errors=remount-ro       0 1
UUID=2222-bbbb       /boot/efi  vfat   umask=0077              0 1
/dev/mapper/vg0-srv  /srv       ext4   defaults,nofail         0 2
UUID=3333-cccc       none       swap   sw                      0 0
tmpfs                /tmp       tmpfs  defaults,nosuid,nodev   0 0
FSTAB
    listening 'LISTEN 0 4096 0.0.0.0:22 0.0.0.0:*' 'LISTEN 0 4096 [::]:22 [::]:*' \
        'LISTEN 0 511 0.0.0.0:443 0.0.0.0:*'
}

# sshd_main LINE...: the Debian layout, drop-ins first, then the given lines.
sshd_main() {
    printf '%s\n' '# See sshd_config(5)' 'Include /etc/ssh/sshd_config.d/*.conf' '' "$@" \
        'UsePAM yes' 'Subsystem sftp /usr/lib/openssh/sftp-server' > "$root/etc/ssh/sshd_config"
}

dropin() {   # dropin NAME LINE...
    printf '%s\n' "${@:2}" > "$root/etc/ssh/sshd_config.d/$1"
}

listening() {
    printf '%s\n' "$@" > "$ss_file"
}

preflight() {
    run bash preflight.sh -r "$root" -s "$ss_file" "$@"
}

@test "a drop-in read by Include wins over the main file" {
    dropin 50-cloud-init.conf 'PasswordAuthentication yes'
    preflight
    assert_failure 2
    assert_line --index 1 "FAIL ssh: PasswordAuthentication yes (want no)"
    assert_line "summary: 1 FAIL, 0 WARN"
}

@test "the first value of a keyword is the one that counts" {
    sshd_main 'PermitRootLogin no' 'PasswordAuthentication no' 'KbdInteractiveAuthentication no' \
        'PasswordAuthentication yes' 'PermitRootLogin yes'
    preflight
    assert_success
    assert_line --index 0 "PASS ssh: PermitRootLogin no"
    assert_line --index 1 "PASS ssh: PasswordAuthentication no"
}

@test "drop-ins are read in lexical order" {
    sshd_main
    dropin 10-hardening.conf 'PermitRootLogin no' 'PasswordAuthentication no' 'KbdInteractiveAuthentication no'
    dropin 60-vendor.conf 'PermitRootLogin yes' 'PasswordAuthentication yes'
    dropin 9-legacy.conf 'KbdInteractiveAuthentication yes'
    # lexical order: 10-hardening.conf, 60-vendor.conf, 9-legacy.conf
    preflight
    assert_success
    assert_output "PASS ssh: PermitRootLogin no
PASS ssh: PasswordAuthentication no
PASS ssh: KbdInteractiveAuthentication no
PASS firewall: input policy drop
PASS port: only allowed ports are public
PASS fstab: stable names and nofail on data disks
summary: 0 FAIL, 0 WARN"
}

@test "keywords ignore case, accept an equals sign, and comments are skipped" {
    sshd_main '# PermitRootLogin yes' '  permitrootlogin   NO' 'PasswordAuthentication=no' \
        'KbdInteractiveAuthentication = no'
    preflight
    assert_success
    assert_line --index 0 "PASS ssh: PermitRootLogin no"
    assert_line --index 1 "PASS ssh: PasswordAuthentication no"
    assert_line --index 2 "PASS ssh: KbdInteractiveAuthentication no"
}

@test "unset keywords fall back to the sshd defaults" {
    sshd_main
    preflight
    assert_failure 2
    assert_line --index 0 "FAIL ssh: PermitRootLogin prohibit-password (want no)"
    assert_line --index 1 "FAIL ssh: PasswordAuthentication yes (want no)"
    assert_line --index 2 "FAIL ssh: KbdInteractiveAuthentication yes (want no)"
    assert_line "summary: 3 FAIL, 0 WARN"
}

@test "settings inside a Match block are not global" {
    sshd_main 'KbdInteractiveAuthentication no' 'Match User backup' '    PermitRootLogin yes' \
        '    PasswordAuthentication no'
    preflight
    assert_failure 2
    assert_line --index 0 "FAIL ssh: PermitRootLogin prohibit-password (want no)"
    assert_line --index 1 "FAIL ssh: PasswordAuthentication yes (want no)"
    assert_line --index 2 "PASS ssh: KbdInteractiveAuthentication no"
}

@test "a relative Include is found under etc/ssh" {
    printf '%s\n' 'Include hardening.conf' 'PasswordAuthentication yes' > "$root/etc/ssh/sshd_config"
    printf '%s\n' 'PermitRootLogin no' 'PasswordAuthentication no' 'KbdInteractiveAuthentication no' \
        > "$root/etc/ssh/hardening.conf"
    preflight
    assert_success
    assert_line --index 1 "PASS ssh: PasswordAuthentication no"
}

@test "a drop policy on another chain does not protect the input" {
    cat > "$root/etc/nftables.conf" <<'NFT'
table inet filter {
    # policy drop would be safer here
    chain input {
        type filter hook input priority 0;
        tcp dport 22 accept
    }
    chain forward {
        type filter hook forward priority 0
        policy drop
    }
}
NFT
    preflight
    assert_failure 2
    assert_line --index 3 "FAIL firewall: input policy is accept (want drop)"
}

@test "an input policy on its own line counts, and a missing ruleset fails" {
    printf '%s\n' 'table inet filter {' '  chain inbound {' '    type filter hook input priority filter' \
        '    policy drop' '    tcp dport { 22, 443 } accept' '  }' '}' > "$root/etc/nftables.conf"
    preflight
    assert_success
    assert_line --index 3 "PASS firewall: input policy drop"
    rm "$root/etc/nftables.conf"
    preflight
    assert_failure 2
    assert_line --index 3 "FAIL firewall: no input chain (want policy drop)"
}

@test "only sockets off loopback are public, one line per port in numeric order" {
    listening 'LISTEN 0 4096 0.0.0.0:22 0.0.0.0:*' 'LISTEN 0 4096 [::]:22 [::]:*' \
        'LISTEN 0 4096 0.0.0.0:9100 0.0.0.0:*' 'LISTEN 0 4096 0.0.0.0:9100 0.0.0.0:*' \
        'LISTEN 0 200 0.0.0.0:5432 0.0.0.0:*' 'LISTEN 0 200 [::]:5432 [::]:*' \
        'LISTEN 0 4096 127.0.0.53%lo:53 0.0.0.0:*' 'LISTEN 0 200 [::1]:6379 [::]:*' \
        'LISTEN 0 128 127.0.0.1:8080 0.0.0.0:*' 'LISTEN 0 128 10.0.0.5:8080 0.0.0.0:*' \
        'LISTEN 0 128 *:80 *:*'
    preflight
    assert_failure 2
    assert_output "PASS ssh: PermitRootLogin no
PASS ssh: PasswordAuthentication no
PASS ssh: KbdInteractiveAuthentication no
PASS firewall: input policy drop
FAIL port: 80 open on *
FAIL port: 5432 open on 0.0.0.0, [::]
FAIL port: 8080 open on 10.0.0.5
FAIL port: 9100 open on 0.0.0.0
PASS fstab: stable names and nofail on data disks
summary: 4 FAIL, 0 WARN"
}

@test "-p sets the ports that may be public" {
    listening 'State Recv-Q Send-Q Local-Address:Port Peer-Address:Port' \
        'LISTEN 0 4096 0.0.0.0:22 0.0.0.0:*' 'LISTEN 0 511 [::]:443 [::]:*' 'LISTEN 0 511 [::]:80 [::]:*'
    preflight -p 22,80
    assert_failure 2
    assert_line --index 4 "FAIL port: 443 open on [::]"
    assert_line "summary: 1 FAIL, 0 WARN"
}

@test "kernel names and data disks without nofail are warnings" {
    cat >> "$root/etc/fstab" <<'FSTAB'
/dev/sdb1            /data      ext4   defaults                0 2
LABEL=archive        /archive   xfs    noatime                 0 2
/dev/nvme1n1p1       /scratch   ext4   defaults,nofail         0 2
/dev/sda3            none       swap   sw                      0 0
FSTAB
    preflight
    assert_failure 1
    assert_output "PASS ssh: PermitRootLogin no
PASS ssh: PasswordAuthentication no
PASS ssh: KbdInteractiveAuthentication no
PASS firewall: input policy drop
PASS port: only allowed ports are public
WARN fstab: /data uses /dev/sdb1 (use UUID= or LABEL=)
WARN fstab: /data has no nofail
WARN fstab: /archive has no nofail
WARN fstab: /scratch uses /dev/nvme1n1p1 (use UUID= or LABEL=)
summary: 0 FAIL, 4 WARN"
}

@test "usage errors and unreadable files exit 3" {
    run bash preflight.sh extra
    assert_failure 3
    assert_output "usage: preflight.sh [-r ROOT] [-s SS_FILE] [-p PORTS]"
    run bash preflight.sh -x
    assert_failure 3
    preflight -p 22,https
    assert_failure 3
    assert_output "preflight.sh: invalid port list: 22,https"
    rm "$root/etc/ssh/sshd_config"
    preflight
    assert_failure 3
    assert_output "preflight.sh: cannot read $root/etc/ssh/sshd_config"
}
