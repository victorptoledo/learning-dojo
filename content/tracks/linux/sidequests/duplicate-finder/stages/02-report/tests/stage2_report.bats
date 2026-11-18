setup() {
    source "$BATS_TEST_DIRNAME/helpers.sh"
    d="$BATS_TEST_TMPDIR/data"
    mkdir -p "$d"
}

@test "reports the wasted space of each group, largest first" {
    fill "$d/iso/debian.iso" 3000 d
    fill "$d/old/debian.iso" 3000 d
    fill "$d/iso/debian (1).iso" 3000 d
    put "$d/a.txt" "0123456789"
    put "$d/b.txt" "0123456789"
    put "$d/unique.txt" "only one"
    run --separate-stderr dupes report "$d"
    assert_success
    assert_output "3 files of 3000 bytes, 6000 wasted:
iso/debian (1).iso
iso/debian.iso
old/debian.iso

2 files of 10 bytes, 10 wasted:
a.txt
b.txt

total: 6010 bytes wasted in 2 groups"
}

@test "orders groups with the same waste by their first path" {
    put "$d/m/1" "bb"
    put "$d/m/2" "bb"
    put "$d/c/1" "aa"
    put "$d/c/2" "aa"
    run dupes report "$d"
    assert_success
    assert_output "2 files of 2 bytes, 2 wasted:
c/1
c/2

2 files of 2 bytes, 2 wasted:
m/1
m/2

total: 4 bytes wasted in 2 groups"
}

@test "counts hard links as one file" {
    fill "$d/a" 500 x
    ln "$d/a" "$d/a-link"
    fill "$d/b" 500 x
    run dupes report "$d"
    assert_success
    assert_output "3 files of 500 bytes, 500 wasted:
a
a-link
b

total: 500 bytes wasted in 1 group"
}

@test "a group of hard links only wastes nothing" {
    fill "$d/a" 500 x
    ln "$d/a" "$d/b"
    run dupes report "$d"
    assert_success
    assert_output "total: 0 bytes wasted in 0 groups"
    run dupes scan "$d"
    assert_success
    assert_output "a
b"
}

@test "--min-size skips small files in scan and report" {
    fill "$d/big1" 5000 b
    fill "$d/big2" 5000 b
    fill "$d/edge1" 4096 e
    fill "$d/edge2" 4096 e
    fill "$d/small1" 100 s
    fill "$d/small2" 100 s
    run dupes scan --min-size 4K "$d"
    assert_success
    assert_output "big1
big2

edge1
edge2"
    run dupes report --min-size 4097 "$d"
    assert_success
    assert_output "2 files of 5000 bytes, 5000 wasted:
big1
big2

total: 5000 bytes wasted in 1 group"
    run dupes report --min-size 1M "$d"
    assert_success
    assert_output "total: 0 bytes wasted in 0 groups"
}

@test "a small file is not read even if another file has its size" {
    fill "$d/big1" 2000 b
    fill "$d/big2" 2000 b
    fill "$d/small1" 10 s
    fill "$d/small2" 10 t
    chmod 000 "$d/small1" "$d/small2"
    run --separate-stderr dupes report --min-size 1K "$d"
    assert_success
    [ -z "$stderr" ]
}

@test "rejects bad sizes and unknown options" {
    run --separate-stderr dupes report --min-size lots "$d"
    assert_failure 2
    [ "$stderr" = "dupes.sh: bad size: lots" ]
    run --separate-stderr dupes scan --min-size 4X "$d"
    assert_failure 2
    [ "$stderr" = "dupes.sh: bad size: 4X" ]
    run --separate-stderr dupes report --verbose "$d"
    assert_failure 2
    [ "$stderr" = "usage: dupes.sh scan|report|link [options] DIR" ]
}
