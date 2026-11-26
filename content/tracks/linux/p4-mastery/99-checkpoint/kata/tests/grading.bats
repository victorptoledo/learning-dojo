setup() {
    bats_load_library bats-support
    bats_load_library bats-assert
    bats_load_library bats-file
    host
}

# A healthy host; each test breaks one part of it.
host() {
    root="$BATS_TEST_TMPDIR/host"
    mkdir -p "$root/proc/pressure" "$root/proc/sys/kernel" "$root/var/log" "$root/boot" "$root/run"
    pressure cpu 1.50 0.00
    pressure memory 0.00 0.00
    pressure io 2.10 0.50
    printf '%s\n' 'Nov 26 09:00:01 web1 kernel: Linux version 6.12.12-amd64' \
        'Nov 26 09:00:02 web1 kernel: EXT4-fs (sda1): mounted filesystem' > "$root/var/log/kern.log"
    service api.service 536870912 1073741824 0 1000 10
    service ssh.service 4194304 max 0
    process 1 systemd 1024 /dev/null 'socket:[1001]'
    process 987 nginx 1024 /dev/null /var/log/nginx/access.log
    echo 6.12.12-amd64 > "$root/proc/sys/kernel/osrelease"
    touch "$root/boot/vmlinuz-6.12.9-amd64" "$root/boot/vmlinuz-6.12.12-amd64"
}

pressure() {   # pressure RESOURCE SOME_AVG10 FULL_AVG10
    printf 'some avg10=%s avg60=1.00 avg300=0.50 total=123456\nfull avg10=%s avg60=0.00 avg300=0.00 total=789\n' \
        "$2" "$3" > "$root/proc/pressure/$1"
}

# service NAME MEMORY_CURRENT MEMORY_MAX OOM_KILLS [NR_PERIODS NR_THROTTLED]
service() {
    local dir="$root/sys/fs/cgroup/system.slice/$1"
    mkdir -p "$dir"
    echo "$2" > "$dir/memory.current"
    echo "$3" > "$dir/memory.max"
    printf 'low 0\nhigh 0\nmax 7\noom 3\noom_kill %s\noom_group_kill 0\n' "$4" > "$dir/memory.events"
    if [ "$#" -ge 6 ]; then
        printf 'usage_usec 90000\nuser_usec 60000\nsystem_usec 30000\nnr_periods %s\nnr_throttled %s\nthrottled_usec 4500000\n' \
            "$5" "$6" > "$dir/cpu.stat"
    else
        printf 'usage_usec 1200\nuser_usec 800\nsystem_usec 400\n' > "$dir/cpu.stat"
    fi
}

# process PID COMM SOFT_LIMIT TARGET...: one fd per target, numbered from 0
process() {
    local dir="$root/proc/$1" fd=0 target
    rm -rf "$dir"
    mkdir -p "$dir/fd"
    echo "$2" > "$dir/comm"
    printf '%-26s%-21s%-21s%s\n' 'Limit' 'Soft Limit' 'Hard Limit' 'Units' \
        'Max processes' '31164' '31164' 'processes' \
        'Max open files' "$3" '524288' 'files' > "$dir/limits"
    for target in "${@:4}"; do
        ln -s "$target" "$dir/fd/$fd"
        fd=$(( fd + 1 ))
    done
}

triage() {
    run bash triage.sh -r "$root" "$@"
}

@test "pressure above 10 warns, and a full stall of memory or io is critical" {
    pressure cpu 10.00 55.00
    pressure memory 35.10 12.00
    pressure io 18.00 2.10
    triage
    assert_failure 2
    assert_line --index 0 "WARN pressure cpu: some 10.00"
    assert_line --index 1 "CRIT pressure memory: some 35.10 full 12.00"
    assert_line --index 2 "WARN pressure io: some 18.00 full 2.10"
    assert_line "status: CRIT (1 crit, 2 warn)"
}

@test "a kernel without PSI and a missing kernel log" {
    rm -r "$root/proc/pressure" "$root/var/log/kern.log"
    triage
    assert_failure 1
    assert_output "WARN pressure: not available (boot with psi=1)
OK oom: no kills
OK cgroups: no service near its limits
OK fds: no process near its limit or holding deleted files
OK kernel: running 6.12.12-amd64
OK reboot: not required
status: WARN (1 warn)"
}

@test "counts the processes the OOM killer killed, not the ones that invoked it" {
    cat >> "$root/var/log/kern.log" <<'LOG'
Nov 26 09:41:10 web1 kernel: nginx invoked oom-killer: gfp_mask=0x140cca(GFP_HIGHUSER_MOVABLE|__GFP_COMP), order=0, oom_score_adj=0
Nov 26 09:41:10 web1 kernel: Out of memory: Killed process 4121 (java) total-vm:9123456kB, anon-rss:7650000kB, file-rss:0kB, shmem-rss:0kB, UID:1001 pgtables:15000kB oom_score_adj:0
Nov 26 09:41:10 web1 kernel: oom_reaper: reaped process 4121 (java), now anon-rss:0kB, file-rss:0kB, shmem-rss:0kB
Nov 26 09:52:33 web1 kernel: php-fpm8.4 invoked oom-killer: gfp_mask=0xcc0(GFP_KERNEL), order=0, oom_score_adj=0
Nov 26 09:52:33 web1 kernel: Memory cgroup out of memory: Killed process 5120 (php-fpm8.4) total-vm:612000kB, anon-rss:250000kB, file-rss:0kB, shmem-rss:0kB, UID:33 pgtables:900kB oom_score_adj:0
Nov 26 10:03:02 web1 kernel: Out of memory: Killed process 4388 (java) total-vm:9000000kB, anon-rss:7600000kB, file-rss:0kB, shmem-rss:0kB, UID:1001 pgtables:14900kB oom_score_adj:0
Nov 26 10:07:45 web1 kernel: Memory cgroup out of memory: Killed process 6001 (node) total-vm:1200000kB, anon-rss:500000kB, file-rss:0kB, shmem-rss:0kB, UID:1002 pgtables:1200kB oom_score_adj:0
LOG
    triage
    assert_failure 2
    assert_line --index 3 "CRIT oom: 4 killed: java x2, node x1, php-fpm8.4 x1"
    assert_line "status: CRIT (1 crit, 0 warn)"
}

@test "a service near its memory limit warns, and a service without a limit never does" {
    service api.service 1038090240 1073741824 0 1000 10
    service ssh.service 99999999999 max 0
    service worker.service 966367642 1073741824 0
    triage
    assert_failure 1
    assert_line --index 4 "WARN cgroup api.service: memory 990 of 1024 MiB (96%)"
    assert_line --index 5 "WARN cgroup worker.service: memory 921 of 1024 MiB (90%)"
    assert_line --index 6 "OK fds: no process near its limit or holding deleted files"
}

@test "OOM kills inside a cgroup are critical, read from the oom_kill counter" {
    service api.service 536870912 1073741824 2 1000 10
    triage
    assert_failure 2
    assert_line --index 4 "CRIT cgroup api.service: oom kills 2"
    assert_line --index 5 "OK fds: no process near its limit or holding deleted files"
}

@test "CPU throttling in a quarter of the periods warns" {
    service api.service 536870912 1073741824 0 2000 500
    service batch.service 536870912 1073741824 0 2000 499
    triage
    assert_failure 1
    assert_output "OK pressure cpu: some 1.50
OK pressure memory: some 0.00 full 0.00
OK pressure io: some 2.10 full 0.50
OK oom: no kills
WARN cgroup api.service: throttled in 25% of periods
OK fds: no process near its limit or holding deleted files
OK kernel: running 6.12.12-amd64
OK reboot: not required
status: WARN (1 warn)"
}

@test "the findings of one service come in order: memory, oom kills, throttling" {
    service api.service 1073741824 1073741824 5 100 90
    triage
    assert_failure 2
    assert_line --index 4 "WARN cgroup api.service: memory 1024 of 1024 MiB (100%)"
    assert_line --index 5 "CRIT cgroup api.service: oom kills 5"
    assert_line --index 6 "WARN cgroup api.service: throttled in 90% of periods"
}

@test "a process at 80% of its open files limit warns" {
    local targets=() i
    for (( i = 0; i < 40; i++ )); do targets+=("socket:[$(( 5000 + i ))]"); done
    process 987 nginx 50 "${targets[@]}"
    process 2001 postgres unlimited /dev/null /dev/null
    process 3001 redis-server 100 "${targets[@]:0:39}" /dev/null
    triage
    assert_failure 1
    assert_line --index 5 "WARN fd 987 nginx: 40 of 50 open files (80%)"
    assert_line --index 6 "OK kernel: running 6.12.12-amd64"
}

@test "deleted files still open are listed once per path, processes in numeric order" {
    process 1234 java 1024 /dev/null 'pipe:[3001]' '/var/log/app/app.log (deleted)' \
        '/srv/my data/cache.db (deleted)' '/var/log/app/app.log (deleted)' /tmp/report.csv
    process 987 nginx 1024 /dev/null '/var/log/nginx/access.log.1 (deleted)'
    triage
    assert_failure 1
    assert_output "OK pressure cpu: some 1.50
OK pressure memory: some 0.00 full 0.00
OK pressure io: some 2.10 full 0.50
OK oom: no kills
OK cgroups: no service near its limits
WARN fd 987 nginx: deleted file still open: /var/log/nginx/access.log.1
WARN fd 1234 java: deleted file still open: /srv/my data/cache.db
WARN fd 1234 java: deleted file still open: /var/log/app/app.log
OK kernel: running 6.12.12-amd64
OK reboot: not required
status: WARN (3 warn)"
}

@test "a newer kernel installed than the one running needs a reboot" {
    echo 6.12.9-amd64 > "$root/proc/sys/kernel/osrelease"
    triage
    assert_failure 1
    assert_line --index 6 "WARN kernel: running 6.12.9-amd64, newest installed 6.12.12-amd64"
    echo 6.12.12-amd64 > "$root/proc/sys/kernel/osrelease"
    rm "$root/boot/vmlinuz-6.12.12-amd64"
    triage
    assert_success
    assert_line --index 6 "OK kernel: running 6.12.12-amd64"
}

@test "reboot-required lists each package once" {
    touch "$root/run/reboot-required"
    printf '%s\n' linux-image-6.12.12-amd64 libc6 linux-image-6.12.12-amd64 > "$root/run/reboot-required.pkgs"
    triage
    assert_failure 1
    assert_line --index 7 "WARN reboot: required by libc6, linux-image-6.12.12-amd64"
    rm "$root/run/reboot-required.pkgs"
    triage
    assert_failure 1
    assert_line --index 7 "WARN reboot: required"
}

@test "usage errors exit 3" {
    run bash triage.sh extra
    assert_failure 3
    assert_output "usage: triage.sh [-r ROOT]"
    run bash triage.sh -x
    assert_failure 3
    assert_output "usage: triage.sh [-r ROOT]"
}
