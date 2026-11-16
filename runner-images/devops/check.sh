#!/bin/bash
# Checks a DevOps kata in /work, offline. Each tool below runs only when the kata has its
# files, and each finding becomes one test next to the bats tests:
#   - GitHub Actions workflows (.github/workflows/*.yml): actionlint, which also runs
#     ShellCheck on `run:` scripts                          -> actionlint::<file>
#   - shell scripts (*.sh): bash -n (a syntax error is the "does not compile") and
#     ShellCheck                                            -> shellcheck::<file>
#   - Terraform (*.tf): `terraform fmt -check` over the kata  -> terraform::fmt
#     and `init` + `validate` in every folder with .tf files -> terraform::validate::<dir>
#     with providers from the baked-in mirror; the hidden tests/*.tftest.hcl files run with
#     `terraform test`, one test per run block              -> terraform_test::<file>::<run>
#   - Prometheus: rule files (*.rules.yml, or .yml files under rules/) with
#     `promtool check rules`                                -> promtool::<file>
#     prometheus.yml with `promtool check config`           -> promtool::prometheus_yml
#     and the hidden tests/*.test.yml rule tests with `promtool test rules`
#                                                           -> promtool_test::<file>
# Then the visible and grading bats tests (staged in /work/tests) run with the helpers in
# /opt/dojo/devops.bash. Prints the result in `cargo test` format.
set -u

out=/target
rm -rf "${out:?}"/*

# Everything runs on a copy. Windows editors may save CRLF line ends, which YAML block
# scalars, HCL heredocs and bash would all keep as part of the text. A local `.terraform`
# folder or lock file holds Windows providers and hashes, so they are dropped.
cp -r /work "$out/kata"
cd "$out/kata" || exit 1
find . -type d -name .terraform -prune -exec rm -rf {} +
find . -type f -name .terraform.lock.hcl -delete
find . -type f \( -name '*.yaml' -o -name '*.yml' -o -name '*.tf' -o -name '*.tfvars' -o -name '*.hcl' \
    -o -name '*.sh' -o -name '*.bats' -o -name '*.txt' -o -name '*.json' -o -name '*.tpl' \
    -o -name '*.cfg' -o -name '*.conf' -o -name '*.ini' -o -name '*.md' \) \
    -exec sed -i 's/\r$//' {} +

log="$out/tools.log"
: > "$log"

# record NAME STATUS OUTPUT_FILE: one tool test in the log tap2cargo.awk reads.
record() {
    if [ "$2" -eq 0 ]; then
        echo "R ok $1" >> "$log"
    else
        echo "R fail $1" >> "$log"
        sed -e "s#$out/kata/##g" -e 's/^/R msg /' "$3" >> "$log"
    fi
}

# The learner's files: everything outside tests/.
learner_files() {
    find . -path ./tests -prune -o -type f "$@" -print | sed 's#^\./##' | sort
}

# ---- shell scripts ---------------------------------------------------------------------
mapfile -t scripts < <(learner_files -name '*.sh')
broken=0
for script in "${scripts[@]}"; do
    if ! bash -n "$script" 2> "$out/syntax.log"; then
        echo "error: $script has a syntax error"
        cat "$out/syntax.log"
        broken=1
    fi
done
[ "$broken" -eq 0 ] || exit 1
for script in "${scripts[@]}"; do
    shellcheck --severity=warning --format=gcc "$script" > "$out/one.log" 2>&1
    record "shellcheck::$script" $? "$out/one.log"
done

# ---- GitHub Actions --------------------------------------------------------------------
mapfile -t workflows < <(learner_files -path './.github/workflows/*' \( -name '*.yml' -o -name '*.yaml' \))
for workflow in "${workflows[@]}"; do
    actionlint -no-color -oneline "$workflow" > "$out/one.log" 2>&1
    record "actionlint::${workflow#.github/workflows/}" $? "$out/one.log"
done

# ---- Terraform -------------------------------------------------------------------------
mapfile -t tf_dirs < <(learner_files -name '*.tf' | xargs -r -n1 dirname | sort -u)
if [ "${#tf_dirs[@]}" -gt 0 ]; then
    # fmt also walks tests/: only the learner's files count.
    mapfile -t unformatted < <(terraform fmt -check -recursive -no-color . 2>&1 | grep -v '^tests/')
    status=0
    if [ "${#unformatted[@]}" -gt 0 ]; then
        status=1
        {
            echo "terraform fmt would change these files (run \`terraform fmt -recursive\`):"
            for file in "${unformatted[@]}"; do
                echo "--- $file"
                [ -f "$file" ] && terraform fmt -no-color - < "$file" | diff "$file" - | head -20
            done
        } > "$out/fmt.log" 2>&1
    fi
    record "terraform::fmt" "$status" "$out/fmt.log"
    for dir in "${tf_dirs[@]}"; do
        name=$dir
        [ "$name" = "." ] && name=root
        if terraform -chdir="$dir" init -backend=false -no-color > "$out/one.log" 2>&1; then
            terraform -chdir="$dir" validate -no-color > "$out/one.log" 2>&1
            record "terraform::validate::$name" $? "$out/one.log"
        else
            { echo "terraform init failed (only the hashicorp providers random, null and local are available offline):"; cat "$out/one.log"; } > "$out/init.log"
            record "terraform::validate::$name" 1 "$out/init.log"
        fi
    done
    if compgen -G 'tests/*.tftest.hcl' > /dev/null; then
        # One `terraform test` per file, then one test per run block from its verbose output.
        for file in tests/*.tftest.hcl; do
            terraform test -no-color -test-directory=tests -filter="$file" > "$out/tftest.log" 2>&1
            awk -v file="$(basename "$file" .tftest.hcl)" '
                function flush() { if (run != "") { print "R " (state == "pass" ? "ok" : "fail") " terraform_test::" file "::" run; if (state != "pass") printf "%s", body; } run = ""; body = "" }
                /^  run "[^"]*"\.\.\. (pass|fail|skip|error)/ {
                    flush()
                    run = $0; sub(/^  run "/, "", run); sub(/"\.\.\..*$/, "", run)
                    state = $0; sub(/^.*\.\.\. /, "", state)
                    next
                }
                /\.\.\. (tearing down|pass|fail)$/ && !/^  run / { next }
                /^(Success|Failure)! / { next }
                run != "" && NF > 0 { line = $0; gsub(/[│╷╵├─]/, "", line); sub(/^[ \t]+/, "", line); if (line != "") body = body "R msg " line "\n" }
                END { flush() }
            ' "$out/tftest.log" > "$out/runs.log"
            if [ -s "$out/runs.log" ]; then
                cat "$out/runs.log" >> "$log"
            else
                { echo "terraform test did not run:"; cat "$out/tftest.log"; } > "$out/one.log"
                record "terraform_test::$(basename "$file" .tftest.hcl)" 1 "$out/one.log"
            fi
        done
    fi
fi

# ---- Prometheus ------------------------------------------------------------------------
mapfile -t rule_files < <(learner_files \( -name '*.rules.yml' -o -name '*.rules.yaml' -o -path './rules/*.yml' -o -path './rules/*.yaml' \))
for rules in "${rule_files[@]}"; do
    promtool check rules "$rules" > "$out/one.log" 2>&1
    record "promtool::$rules" $? "$out/one.log"
done
if [ -f prometheus.yml ]; then
    promtool check config --syntax-only prometheus.yml > "$out/one.log" 2>&1
    record "promtool::prometheus.yml" $? "$out/one.log"
fi
if compgen -G 'tests/*.test.yml' > /dev/null; then
    for file in tests/*.test.yml; do
        (cd tests && promtool test rules "${file#tests/}") > "$out/one.log" 2>&1
        status=$?
        record "promtool_test::$(basename "$file" .test.yml)" "$status" "$out/one.log"
    done
fi

# ---- bats ------------------------------------------------------------------------------
: > "$out/bats.tap"
if compgen -G 'tests/*.bats' > /dev/null; then
    KATA="$out/kata" bats --tap --recursive tests > "$out/bats.tap" 2>&1
fi

awk -f /opt/dojo/tap2cargo.awk "$out/bats.tap" "$log"
status=$?
if [ "$status" -eq 2 ]; then
    echo "error: no tests ran"
    head -40 "$out/bats.tap"
fi
exit "$status"
