# Turns bats TAP output plus the lint summary from check.sh into `cargo test` style
# lines: "test name ... ok|FAILED", then one "---- name stdout ----" block per failure.
# Exits 0 when everything passed, 1 when something failed, 2 when no test ran.

function slug(text,    s) {
    s = text
    gsub(/[^A-Za-z0-9_]+/, "_", s)
    gsub(/^_+|_+$/, "", s)
    return s
}

function close_failure() {
    if (current != "") {
        messages[current] = message
    }
    current = ""
    message = ""
}

function add_line(line) {
    sub(/^#[ ]?/, "", line)
    sub(/^[ \t]+/, "", line)
    if (line == "" || line ~ /^\(in test file/ || line ~ /^\(from function/ || line == "--") {
        return
    }
    if (line ~ /todo:/ && message !~ /^todo/) {
        message = substr(line, index(line, "todo:")) (message == "" ? "" : "\n" message)
        return
    }
    message = message (message == "" ? "" : "\n") line
}

FNR == 1 { close_failure() }

# bats TAP
FILENAME ~ /bats\.tap$/ && /^ok [0-9]+ / {
    close_failure()
    name = $0; sub(/^ok [0-9]+ /, "", name); sub(/ # skip.*$/, "", name)
    order[++count] = slug(name); state[slug(name)] = "ok"
    next
}
FILENAME ~ /bats\.tap$/ && /^not ok [0-9]+ / {
    close_failure()
    name = $0; sub(/^not ok [0-9]+ /, "", name)
    current = slug(name)
    order[++count] = current; state[current] = "FAILED"
    next
}
FILENAME ~ /bats\.tap$/ && /^#/ && current != "" { add_line($0); next }
FILENAME ~ /bats\.tap$/ && current != "" && !/^1\.\./ { add_line($0); next }

# lint summary: "LINT ok TOOL FILE", "LINT fail TOOL FILE", then "LINT msg TEXT" lines
/^LINT ok / { close_failure(); name = $3 "::" slug($4); lint[name] = 1; order[++count] = name; state[name] = "ok"; next }
/^LINT fail / { close_failure(); current = $3 "::" slug($4); lint[current] = 1; order[++count] = current; state[current] = "FAILED"; next }
/^LINT msg / && current != "" { add_line(substr($0, 10)); next }

END {
    close_failure()
    bats_tests = 0
    for (i = 1; i <= count; i++) if (!(order[i] in lint)) bats_tests++
    if (bats_tests == 0) exit 2
    printf "\nrunning %d tests\n", count
    passed = 0; failed = 0
    for (i = 1; i <= count; i++) {
        printf "test %s ... %s\n", order[i], state[order[i]]
        if (state[order[i]] == "ok") passed++; else failed++
    }
    if (failed > 0) {
        printf "\nfailures:\n\n"
        for (i = 1; i <= count; i++) {
            n = order[i]
            if (state[n] != "ok") {
                text = messages[n]
                if (text == "") text = "the test failed"
                printf "---- %s stdout ----\nthread '%s' panicked at %s:\n%s\n\n", n, n, n, text
            }
        }
        printf "failures:\n\ntest result: FAILED. %d passed; %d failed\n", passed, failed
        exit 1
    }
    printf "\ntest result: ok. %d passed; 0 failed\n", passed
    exit 0
}
