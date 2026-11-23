bats_require_minimum_version 1.5.0

setup() {
    source "$BATS_TEST_DIRNAME/helpers.sh"
    dist="$repo/dist"
}

manifest() {
    (cd "$BATS_TEST_TMPDIR" && timeout 30 bash "$repo/scripts/manifest.sh" "$@" 3>&-)
}

verify() {
    (cd "$BATS_TEST_TMPDIR" && timeout 30 bash "$repo/scripts/verify.sh" "$@" 3>&-)
}

# released VERSION [TARGET...]: builds, then writes the manifest, with a commit id.
released() {
    build "$@" > /dev/null
    GIT_COMMIT=4f2a9c1e0b7d manifest "$dist" > /dev/null
}

@test "SHA256SUMS lists every archive, sorted, and sha256sum -c accepts it" {
    build 1.2.0 > /dev/null
    run --separate-stderr manifest "$dist"
    assert_success
    assert_output "manifest: 1.2.0, 3 artifacts"
    run cut -d' ' -f3 "$dist/SHA256SUMS"
    assert_output "greet-1.2.0-darwin-arm64.tar.gz
greet-1.2.0-linux-amd64.tar.gz
greet-1.2.0-linux-arm64.tar.gz"
    run bash -c "cd '$dist' && sha256sum -c SHA256SUMS"
    assert_success
}

@test "manifest.json describes the release and every artifact" {
    released 1.3.0-rc.1
    run jq -r '"\(.name) \(.version) \(.commit) \(.artifacts | length)"' "$dist/manifest.json"
    assert_output "greet 1.3.0-rc.1 4f2a9c1e0b7d 3"
    run jq -r '.artifacts[] | "\(.file) \(.target) \(.size) \(.sha256)"' "$dist/manifest.json"
    expected=""
    for file in greet-1.3.0-rc.1-darwin-arm64.tar.gz greet-1.3.0-rc.1-linux-amd64.tar.gz greet-1.3.0-rc.1-linux-arm64.tar.gz; do
        target=${file#greet-1.3.0-rc.1-}
        expected+="$file ${target%.tar.gz} $(stat -c %s "$dist/$file") $(sha "$dist/$file")"$'\n'
    done
    assert_output "${expected%$'\n'}"
    run jq -r '.artifacts[0].size | type' "$dist/manifest.json"
    assert_output "number"
}

@test "the commit is unknown without GIT_COMMIT, and nothing temporary is left" {
    build 1.2.0 linux-amd64 > /dev/null
    run bash -c "cd '$BATS_TEST_TMPDIR' && env -u GIT_COMMIT bash '$repo/scripts/manifest.sh' '$dist'"
    assert_success
    run jq -r '.commit' "$dist/manifest.json"
    assert_output "unknown"
    run ls "$dist"
    assert_output "SHA256SUMS
greet-1.2.0-linux-amd64.tar.gz
manifest.json"
}

@test "manifest refuses an empty folder and archives of two versions" {
    mkdir -p "$dist"
    run --separate-stderr manifest "$dist"
    assert_failure 1
    build 1.2.0 linux-amd64 > /dev/null
    cp "$dist/greet-1.2.0-linux-amd64.tar.gz" "$BATS_TEST_TMPDIR/old.tar.gz"
    build 1.3.0 linux-amd64 > /dev/null
    cp "$BATS_TEST_TMPDIR/old.tar.gz" "$dist/greet-1.2.0-linux-amd64.tar.gz"
    run --separate-stderr manifest "$dist"
    assert_failure 1
    [ ! -e "$dist/manifest.json" ] || { echo "a manifest was written for two versions"; return 1; }
    run --separate-stderr manifest "$BATS_TEST_TMPDIR/nowhere"
    assert_failure 2
}

@test "verify accepts a release it describes" {
    released 1.2.0
    run --separate-stderr verify "$dist"
    assert_success
    assert_output "verify: ok (3 artifacts, version 1.2.0)"
}

@test "verify catches an archive changed after the release" {
    released 1.2.0
    printf 'x' >> "$dist/greet-1.2.0-linux-arm64.tar.gz"
    run --separate-stderr verify "$dist"
    assert_failure 1
    assert_line "verify: greet-1.2.0-linux-arm64.tar.gz: size mismatch"
    assert_line "verify: greet-1.2.0-linux-arm64.tar.gz: checksum mismatch"
    refute_output --partial "linux-amd64"
}

@test "verify catches a swapped archive of the same size" {
    released 1.2.0
    # Same size, other bytes: only the checksum tells.
    size=$(stat -c %s "$dist/greet-1.2.0-linux-amd64.tar.gz")
    head -c "$size" /dev/zero > "$dist/greet-1.2.0-linux-amd64.tar.gz"
    run --separate-stderr verify "$dist"
    assert_failure 1
    assert_line "verify: greet-1.2.0-linux-amd64.tar.gz: checksum mismatch"
    refute_line "verify: greet-1.2.0-linux-amd64.tar.gz: size mismatch"
}

@test "verify catches a missing archive and an extra one" {
    released 1.2.0
    rm "$dist/greet-1.2.0-darwin-arm64.tar.gz"
    cp "$dist/greet-1.2.0-linux-amd64.tar.gz" "$dist/greet-1.2.0-windows-amd64.tar.gz"
    run --separate-stderr verify "$dist"
    assert_failure 1
    assert_line "verify: greet-1.2.0-darwin-arm64.tar.gz: missing"
    assert_line "verify: greet-1.2.0-windows-amd64.tar.gz: not in the manifest"
}

@test "verify catches SHA256SUMS edited apart from the manifest" {
    released 1.2.0
    sed -i 's/^[0-9a-f]\{8\}/00000000/' "$dist/SHA256SUMS"
    run --separate-stderr verify "$dist"
    assert_failure 1
    assert_line "verify: greet-1.2.0-darwin-arm64.tar.gz: SHA256SUMS disagrees"
    assert_line "verify: greet-1.2.0-linux-amd64.tar.gz: SHA256SUMS disagrees"
    assert_line "verify: greet-1.2.0-linux-arm64.tar.gz: SHA256SUMS disagrees"
}

@test "verify catches an archive of another version under the right name" {
    build 1.1.0 linux-amd64 > /dev/null
    cp "$dist/greet-1.1.0-linux-amd64.tar.gz" "$BATS_TEST_TMPDIR/old.tar.gz"
    build 1.2.0 linux-amd64 > /dev/null
    cp "$BATS_TEST_TMPDIR/old.tar.gz" "$dist/greet-1.2.0-linux-amd64.tar.gz"
    manifest "$dist" > /dev/null
    run --separate-stderr verify "$dist"
    assert_failure 1
    assert_line "verify: greet-1.2.0-linux-amd64.tar.gz: is not version 1.2.0 inside"
}

@test "verify needs the manifest and the checksums, and a real manifest" {
    released 1.2.0
    mv "$dist/SHA256SUMS" "$BATS_TEST_TMPDIR/"
    run --separate-stderr verify "$dist"
    assert_failure 1
    assert_output "verify: missing SHA256SUMS"
    mv "$BATS_TEST_TMPDIR/SHA256SUMS" "$dist/"
    echo '{"hello": "world"}' > "$dist/manifest.json"
    run --separate-stderr verify "$dist"
    assert_failure 1
    assert_output "verify: manifest.json is not a release manifest"
    rm "$dist/manifest.json"
    run --separate-stderr verify "$dist"
    assert_failure 1
    assert_output "verify: missing manifest.json"
}
