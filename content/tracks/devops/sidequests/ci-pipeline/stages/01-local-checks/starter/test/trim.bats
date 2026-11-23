#!/usr/bin/env bats

setup() {
    BIN="$BATS_TEST_DIRNAME/../bin"
}

@test "trim removes the blanks around every line" {
    run bash -c "printf '  a b \n\tc\t\n' | bash '$BIN/trim.sh'"
    [ "$status" -eq 0 ]
    [ "$output" = "a b
c" ]
}
