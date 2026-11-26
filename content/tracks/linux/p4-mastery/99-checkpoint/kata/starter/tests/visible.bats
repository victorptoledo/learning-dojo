setup() {
    bats_load_library bats-support
    bats_load_library bats-assert
    bats_load_library bats-file
}

@test "a healthy host prints OK for every check" {
    root="$BATS_TEST_TMPDIR/host"
    mkdir -p "$root/proc/pressure" "$root/proc/sys/kernel" "$root/proc/1/fd" "$root/var/log" "$root/boot" \
        "$root/sys/fs/cgroup/system.slice/api.service"
    printf 'some avg10=1.50 avg60=1.00 avg300=0.50 total=1\nfull avg10=0.00 avg60=0.00 avg300=0.00 total=0\n' \
        > "$root/proc/pressure/cpu"
    printf 'some avg10=0.00 avg60=0.00 avg300=0.00 total=0\nfull avg10=0.00 avg60=0.00 avg300=0.00 total=0\n' \
        > "$root/proc/pressure/memory"
    printf 'some avg10=2.10 avg60=1.00 avg300=0.50 total=9\nfull avg10=0.50 avg60=0.10 avg300=0.00 total=2\n' \
        > "$root/proc/pressure/io"
    echo 'Nov 26 09:00:01 web1 kernel: Linux version 6.12.12-amd64' > "$root/var/log/kern.log"
    echo 536870912 > "$root/sys/fs/cgroup/system.slice/api.service/memory.current"
    echo 1073741824 > "$root/sys/fs/cgroup/system.slice/api.service/memory.max"
    printf 'oom 0\noom_kill 0\n' > "$root/sys/fs/cgroup/system.slice/api.service/memory.events"
    printf 'nr_periods 1000\nnr_throttled 10\n' > "$root/sys/fs/cgroup/system.slice/api.service/cpu.stat"
    echo systemd > "$root/proc/1/comm"
    printf 'Max open files            1024                 524288               files\n' > "$root/proc/1/limits"
    ln -s /dev/null "$root/proc/1/fd/0"
    echo 6.12.12-amd64 > "$root/proc/sys/kernel/osrelease"
    touch "$root/boot/vmlinuz-6.12.9-amd64" "$root/boot/vmlinuz-6.12.12-amd64"
    run bash triage.sh -r "$root"
    assert_success
    assert_output "OK pressure cpu: some 1.50
OK pressure memory: some 0.00 full 0.00
OK pressure io: some 2.10 full 0.50
OK oom: no kills
OK cgroups: no service near its limits
OK fds: no process near its limit or holding deleted files
OK kernel: running 6.12.12-amd64
OK reboot: not required
status: OK"
}
