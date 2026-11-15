setup() {
    bats_load_library bats-support
    bats_load_library bats-assert
    bats_load_library bats-file
}

@test "reports memory from a fake /proc" {
    proc="$BATS_TEST_TMPDIR/proc"
    mkdir -p "$proc"
    printf 'MemTotal:        8192000 kB\nMemFree:          100000 kB\nMemAvailable:    4096000 kB\n' > "$proc/meminfo"
    printf '0.50 0.40 0.30 1/200 1234\n' > "$proc/loadavg"
    printf 'processor\t: 0\nprocessor\t: 1\n' > "$proc/cpuinfo"
    printf 'Filesystem 1024-blocks Used Available Capacity Mounted on\n' > "$BATS_TEST_TMPDIR/df.txt"
    run bash health.sh -p "$proc" -d "$BATS_TEST_TMPDIR/df.txt"
    assert_line --index 0 "memory: 50% used (4000 of 8000 MiB)"
}
