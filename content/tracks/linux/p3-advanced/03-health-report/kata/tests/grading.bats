setup() {
    bats_load_library bats-support
    bats_load_library bats-assert
    bats_load_library bats-file
}

fake_proc() {
    # fake_proc TOTAL_KB AVAILABLE_KB LOAD CPUS
    proc="$BATS_TEST_TMPDIR/proc"
    mkdir -p "$proc"
    printf 'MemTotal:     %s kB\nMemFree:      1000 kB\nMemAvailable: %s kB\nBuffers:      10 kB\n' "$1" "$2" > "$proc/meminfo"
    printf '%s 0.90 0.80 2/345 6789\n' "$3" > "$proc/loadavg"
    : > "$proc/cpuinfo"
    local i
    for (( i = 0; i < $4; i++ )); do
        printf 'processor\t: %d\nmodel name\t: Fake CPU\n\n' "$i" >> "$proc/cpuinfo"
    done
}

fake_df() {
    df_file="$BATS_TEST_TMPDIR/df.txt"
    printf 'Filesystem     1024-blocks     Used Available Capacity Mounted on\n' > "$df_file"
    printf '%s\n' "$@" >> "$df_file"
}

@test "a healthy server prints OK and exits 0" {
    fake_proc 8192000 5079040 1.20 4
    fake_df '/dev/sda2 41152736 33333316 7819420 81% /' 'tmpfs 1638400 1638400 0 100% /run'
    run bash health.sh -p "$proc" -d "$df_file"
    assert_success
    assert_output "memory: 38% used (3040 of 8000 MiB)
load: 1.20 on 4 cpus (0.30 per cpu)
disk /: 81% used
status: OK"
}

@test "marks a full disk and exits 1" {
    fake_proc 8192000 5079040 1.20 4
    fake_df '/dev/sda2 41152736 33333316 7819420 81% /' '/dev/sdb1 103081248 97927186 5154062 95% /data'
    run bash health.sh -p "$proc" -d "$df_file"
    assert_failure 1
    assert_line --index 3 "disk /data: 95% used WARN"
    assert_line --index 4 "status: WARN (1 check)"
}

@test "marks memory and load at exactly the thresholds" {
    fake_proc 1000000 100000 2.00 2
    fake_df '/dev/sda2 100 90 10 90% /'
    run bash health.sh -p "$proc" -d "$df_file"
    assert_failure 1
    assert_output "memory: 90% used (878 of 976 MiB) WARN
load: 2.00 on 2 cpus (1.00 per cpu) WARN
disk /: 90% used WARN
status: WARN (3 checks)"
}

@test "rounds the memory percentage down" {
    fake_proc 1000000 100001 0.10 1
    fake_df
    run bash health.sh -p "$proc" -d "$df_file"
    assert_success
    assert_line --index 0 "memory: 89% used (878 of 976 MiB)"
}

@test "skips tmpfs, devtmpfs and udev" {
    fake_proc 8192000 8000000 0.10 8
    fake_df 'udev 100 100 0 100% /dev' 'devtmpfs 100 100 0 100% /dev/shm' 'tmpfs 100 99 1 99% /run' '/dev/vda1 100 10 90 10% /'
    run bash health.sh -p "$proc" -d "$df_file"
    assert_success
    assert_line --index 2 "disk /: 10% used"
    assert_line --index 3 "status: OK"
}

@test "reads the real /proc and df without options" {
    run bash health.sh
    assert_line --regexp '^memory: [0-9]+% used \([0-9]+ of [0-9]+ MiB\)( WARN)?$'
    assert_line --regexp '^load: [0-9.]+ on [0-9]+ cpus \([0-9]+\.[0-9]{2} per cpu\)( WARN)?$'
    assert_line --regexp '^status: (OK|WARN \([0-9]+ checks?\))$'
}

@test "reports a missing source file" {
    mkdir -p "$BATS_TEST_TMPDIR/empty"
    run --separate-stderr bash health.sh -p "$BATS_TEST_TMPDIR/empty" -d /dev/null
    assert_failure 2
    [ "$stderr" = "health.sh: cannot read $BATS_TEST_TMPDIR/empty/meminfo" ]
}

@test "prints the usage for an unknown option or an operand" {
    run --separate-stderr bash health.sh -x
    assert_failure 2
    [ "$stderr" = "usage: health.sh [-p PROC] [-d DF_FILE]" ]
    run --separate-stderr bash health.sh extra
    assert_failure 2
    [ "$stderr" = "usage: health.sh [-p PROC] [-d DF_FILE]" ]
}
