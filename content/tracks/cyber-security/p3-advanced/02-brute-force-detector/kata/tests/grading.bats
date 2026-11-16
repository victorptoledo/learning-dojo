setup() {
    bats_load_library bats-support
    bats_load_library bats-assert
    bats_load_library bats-file
}

fail_line() { # ip user [invalid]
    if [ "${3:-}" = invalid ]; then
        echo "Nov 14 11:00:00 web1 sshd[1]: Failed password for invalid user $2 from $1 port 40000 ssh2"
    else
        echo "Nov 14 11:00:00 web1 sshd[1]: Failed password for $2 from $1 port 40000 ssh2"
    fi
}

ok_line() { # ip user method
    echo "Nov 14 11:30:00 web1 sshd[2]: Accepted ${3:-password} for $2 from $1 port 40100 ssh2"
}

@test "prints the whole sample report" {
    run bash bruteforce.sh sample.log
    assert_failure 1
    assert_output "SUSPECT 203.0.113.7 failures=6 users=admin,oracle,root
ALERT 203.0.113.7 accepted user=deploy after 6 failures"
}

@test "a higher threshold silences the sample and exits 0" {
    run bash bruteforce.sh sample.log 7
    assert_success
    assert_output ""
}

@test "orders suspects by failures, then by IP as text" {
    log="$BATS_TEST_TMPDIR/order.log"
    {
        for _ in 1 2 3; do fail_line 192.0.2.9 root; done
        for _ in 1 2 3 4; do fail_line 192.0.2.10 root; done
        for _ in 1 2 3; do fail_line 192.0.2.1 bob; done
    } > "$log"
    run bash bruteforce.sh "$log" 3
    assert_success
    assert_output "SUSPECT 192.0.2.10 failures=4 users=root
SUSPECT 192.0.2.1 failures=3 users=bob
SUSPECT 192.0.2.9 failures=3 users=root"
}

@test "only failures before the login count for an alert" {
    log="$BATS_TEST_TMPDIR/before.log"
    {
        fail_line 192.0.2.5 root
        fail_line 192.0.2.5 root
        ok_line 192.0.2.5 ada
        fail_line 192.0.2.5 root
        ok_line 192.0.2.5 grace publickey
    } > "$log"
    run bash bruteforce.sh "$log" 3
    assert_failure 1
    assert_output "SUSPECT 192.0.2.5 failures=3 users=root
ALERT 192.0.2.5 accepted user=grace after 3 failures"
}

@test "every accepted method counts and key details are ignored" {
    log="$BATS_TEST_TMPDIR/methods.log"
    {
        fail_line 192.0.2.7 root
        fail_line 192.0.2.7 root
        echo "Nov 14 11:30:00 web1 sshd[3]: Accepted publickey for ops from 192.0.2.7 port 1 ssh2: ED25519 SHA256:xyz"
        ok_line 192.0.2.7 kim keyboard-interactive/pam
    } > "$log"
    run bash bruteforce.sh "$log" 2
    assert_failure 1
    assert_output "SUSPECT 192.0.2.7 failures=2 users=root
ALERT 192.0.2.7 accepted user=ops after 2 failures
ALERT 192.0.2.7 accepted user=kim after 2 failures"
}

@test "usernames with spaces and 'from' cannot spoof the source IP" {
    log="$BATS_TEST_TMPDIR/spoof.log"
    {
        for _ in 1 2 3; do fail_line 203.0.113.50 "x from 10.9.9.9 port 1 ssh2 y" invalid; done
        fail_line 203.0.113.50 "evil from 10.9.9.9" invalid
    } > "$log"
    run bash bruteforce.sh "$log" 4
    assert_success
    assert_output "SUSPECT 203.0.113.50 failures=4 users=evil from 10.9.9.9,x from 10.9.9.9 port 1 ssh2 y"
}

@test "IPv6 sources and invalid users" {
    log="$BATS_TEST_TMPDIR/v6.log"
    {
        fail_line 2001:db8::1 admin invalid
        fail_line 2001:db8::1 test invalid
        fail_line 2001:db8::1 admin invalid
    } > "$log"
    run bash bruteforce.sh "$log" 3
    assert_success
    assert_output "SUSPECT 2001:db8::1 failures=3 users=admin,test"
}

@test "windows line ends are handled" {
    log="$BATS_TEST_TMPDIR/crlf.log"
    {
        fail_line 192.0.2.8 root
        fail_line 192.0.2.8 admin invalid
        ok_line 192.0.2.8 deploy
    } | sed 's/$/\r/' > "$log"
    run bash bruteforce.sh "$log" 2
    assert_failure 1
    assert_output "SUSPECT 192.0.2.8 failures=2 users=admin,root
ALERT 192.0.2.8 accepted user=deploy after 2 failures"
}

@test "lines from other programs are ignored" {
    log="$BATS_TEST_TMPDIR/other.log"
    {
        for _ in 1 2 3; do
            echo "Nov 14 11:00:00 web1 myapp[9]: Failed password for root from 192.0.2.3 port 1 ssh2"
        done
        echo "Nov 14 11:00:00 web1 sshd[1]: Connection closed by 192.0.2.3 port 1 [preauth]"
        echo "Nov 14 11:00:00 web1 sshd[1]: Failed password for root from 192.0.2.3 port 1 ssh2 and more"
    } > "$log"
    run bash bruteforce.sh "$log" 1
    assert_success
    assert_output ""
}

@test "argument errors use stderr and exit 2" {
    run bash bruteforce.sh
    assert_failure 2
    assert_output "usage: bruteforce.sh LOG [THRESHOLD]"
    run bash bruteforce.sh sample.log 3 extra
    assert_failure 2
    run --separate-stderr bash bruteforce.sh missing.log
    assert_failure 2
    assert_output ""
    [ "$stderr" = "error: cannot read missing.log" ]
}

@test "the threshold must be a positive integer" {
    for bad in 0 -3 abc 2.5 "5; rm -rf ~"; do
        run --separate-stderr bash bruteforce.sh sample.log "$bad"
        assert_failure 2
        [ "$stderr" = "error: threshold must be a positive integer" ]
    done
}
