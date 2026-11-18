setup() {
    source "$BATS_TEST_DIRNAME/helpers.sh"
}

# history: a log of every branch and tag, without ids, to compare before and after.
history() {
    git -C "$repo" log --branches --tags --topo-order --date=iso-strict \
        --format='%an|%ae|%ad|%cn|%cd|%B'
}

# in_history PATH: the commits of any branch or tag whose tree has PATH.
in_history() {
    local c
    for c in $(git -C "$repo" rev-list --branches --tags); do
        if git -C "$repo" cat-file -e "$c:$1" 2> /dev/null; then echo "$c"; fi
    done
}

@test "purge: a file leaves every commit of every branch" {
    put README.md "# Shop\n"
    commit "readme"
    put src/app.txt "app v1\n"
    put .env "AWS_ACCESS_KEY_ID=$AWS_KEY\n"
    commit "app"
    put src/app.txt "app v2\n"
    put .env "AWS_ACCESS_KEY_ID=$AWS_KEY\nDB=shop\n"
    commit "app v2"
    git -C "$repo" switch -q -c feature
    put src/feature.txt "f\n"
    put .env "changed on feature\n"
    commit "feature"
    git -C "$repo" switch -q main
    before=$(history)
    readme=$(id main~2)
    run --separate-stderr scrub purge "$repo" .env
    assert_success
    assert_output "rewrote 3 commits, dropped 0"
    [ -z "$(in_history .env)" ]
    [ "$(history)" = "$before" ]
    [ "$(id main~2)" = "$readme" ]
    [ "$(git -C "$repo" show main:src/app.txt)" = "app v2" ]
    [ "$(git -C "$repo" show main~1:src/app.txt)" = "app v1" ]
    [ "$(git -C "$repo" show feature:src/feature.txt)" = "f" ]
    [ "$(id feature~1)" = "$(id main)" ]
}

@test "purge: commits left empty are dropped, roots included" {
    put .env "SECRET=1\n"
    commit "add env"
    put src/app.txt "app\n"
    commit "app"
    put .env "SECRET=2\n"
    commit "rotate env"
    put src/app.txt "app 2\n"
    commit "app 2"
    run --separate-stderr scrub purge "$repo" .env
    assert_success
    assert_output "rewrote 2 commits, dropped 2"
    run git -C "$repo" log --format=%s main
    assert_output "app 2
app"
    [ "$(git -C "$repo" rev-list --max-parents=0 main)" = "$(id main~1)" ]
    [ -z "$(in_history .env)" ]
}

@test "purge: a branch on a dropped commit moves to its parent" {
    put src/app.txt "app\n"
    commit "app"
    git -C "$repo" switch -q -c leak
    put secret.key "k\n"
    commit "only the key"
    git -C "$repo" switch -q main
    put src/app.txt "app 2\n"
    commit "app 2"
    app=$(id main~1)
    run scrub purge "$repo" secret.key
    assert_success
    assert_output "rewrote 0 commits, dropped 1"
    [ "$(id leak)" = "$app" ]
    [ "$(id main~1)" = "$app" ]
}

@test "purge: merges, empty commits and messages without a final newline are kept" {
    put a.txt "a\n"
    commit "base"
    git -C "$repo" commit -q --allow-empty -m "release marker"
    git -C "$repo" switch -q -c topic
    put b.txt "b\n"
    put big.bin "big\n"
    commit "topic

with a body

and blank lines"
    git -C "$repo" switch -q main
    put c.txt "c\n"
    commit "main"
    git -C "$repo" merge -q --no-ff -m "merge topic" topic
    tree=$(git -C "$repo" write-tree)
    raw=$(printf 'no final newline' | git -C "$repo" commit-tree -p HEAD "$tree")
    git -C "$repo" reset -q "$raw"
    before=$(history)
    run scrub purge "$repo" big.bin
    assert_success
    assert_output "rewrote 3 commits, dropped 0"
    [ "$(history)" = "$before" ]
    [ "$(git -C "$repo" rev-list --count --merges main)" -eq 1 ]
    [ "$(git -C "$repo" log -1 --format=%B main~1)" = "merge topic" ]
    [ "$(git -C "$repo" cat-file commit main | tail -c 16)" = "no final newline" ]
    [ "$(git -C "$repo" log -1 --format=%s 'main~1^1~1')" = "release marker" ]
    [ -z "$(in_history big.bin)" ]
}

@test "purge: tags follow, annotated ones keep their message and tagger" {
    put a.txt "a\n"
    put media/demo.mp4 "video\n"
    commit "one"
    git -C "$repo" tag -a -m "Release 1.0

Our first release." v1.0
    git -C "$repo" tag light
    put a.txt "a2\n"
    commit "two"
    run scrub purge "$repo" media
    assert_success
    [ "$(git -C "$repo" cat-file -t v1.0)" = tag ]
    [ "$(git -C "$repo" rev-parse 'v1.0^{commit}')" = "$(id main~1)" ]
    [ "$(id light)" = "$(id main~1)" ]
    [ "$(git -C "$repo" tag -l --format='%(contents)' v1.0)" = "Release 1.0

Our first release." ]
    [ "$(git -C "$repo" tag -l --format='%(taggername) %(taggeremail)' v1.0)" = "Dojo Tester <tester@dojo.test>" ]
    [ -z "$(in_history media/demo.mp4)" ]
}

@test "purge: folders, names with spaces, and only whole names" {
    put "my secrets/prod key.txt" "k\n"
    put "my secrets/deep/more.txt" "m\n"
    put config/app.yaml "a\n"
    put config.bak "b\n"
    put configs/x.yaml "x\n"
    commit "files"
    run scrub purge "$repo" "my secrets" config
    assert_success
    run git -C "$repo" ls-tree -r --name-only main
    assert_output "config.bak
configs/x.yaml"
}

@test "purge: the working tree follows, untracked files stay" {
    put src/app.txt "app\n"
    put dump.sql "rows\n"
    commit "with a dump"
    put notes.txt "untracked\n"
    run scrub purge "$repo" dump.sql
    assert_success
    [ "$(git -C "$repo" symbolic-ref HEAD)" = refs/heads/main ]
    assert_file_not_exist "$repo/dump.sql"
    assert_file_exist "$repo/notes.txt"
    [ "$(git -C "$repo" status --porcelain)" = "?? notes.txt" ]
}

@test "purge: refuses uncommitted changes and empty repositories" {
    run --separate-stderr scrub purge "$repo" x
    assert_failure 1
    [ "$stderr" = "scrub.sh: no commits" ]
    put a.txt "a\n"
    put secret.txt "s\n"
    commit "one"
    put a.txt "edited\n"
    before=$(state)
    run --separate-stderr scrub purge "$repo" secret.txt
    assert_failure 1
    [ "$stderr" = "scrub.sh: uncommitted changes" ]
    [ "$(state)" = "$before" ]
    run --separate-stderr scrub purge "$repo"
    assert_failure 2
}
