load /opt/dojo/facts

@test "the image is at most 30 MB" {
    size=$(image_size_mb)
    [ "$size" -le 30 ] || fail "the image is $size MB"
}
