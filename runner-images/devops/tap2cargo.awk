# Turns bats TAP output plus the tool results from check.sh (actionlint, ShellCheck, Terraform, promtool) into `cargo test` style
# lines: "test name ... ok|FAILED", then one "---- name stdout ----" block per failure.
# Exits 0 when everything passed, 1 when something failed, 2 when no test ran.

function slug(text,    s) {
    s = text
    gsub(/[^A-Za-z0-9_]+/, "_", s)
    gsub(/^_+|_+$/, "", s)
    return s
}

# "actionlint::ci.yml" -> "actionlint::ci_yml": the tool prefix and each part slugged.
function tool_name(text,    parts, n, i, s) {
    n = split(text, parts, "::")
    s = slug(parts[1])
    for (i = 2; i <= n; i++) s = s "::" slug(parts[i])
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
    # bats' stack trace and the "`@test ...' failed" line repeat what the test name says.
    if (line == "" || line ~ /^\(?in test file/ || line ~ /^\(from function/ || line ~ /^`.*' failed$/ || line == "--") {
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

# tool results: "R ok NAME", "R fail NAME", "R msg TEXT"; NAME is tool::file[::run]
/^R ok / { close_failure(); name = tool_name(substr($0, 6)); order[++count] = name; state[name] = "ok"; next }
/^R fail / { close_failure(); current = tool_name(substr($0, 8)); order[++count] = current; state[current] = "FAILED"; next }
/^R msg / && current != "" { add_line(substr($0, 7)); next }

END {
    close_failure()
    if (count == 0) exit 2
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
