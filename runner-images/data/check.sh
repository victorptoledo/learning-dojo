#!/bin/bash
# Checks a Python kata in /work: every module must compile, the visible and grading tests
# (staged in /work/tests) run with pytest, and ruff checks each module for real mistakes.
# Prints the result in `cargo test` format.
set -u

out=/target
rm -rf "${out:?}"/*

# Everything runs on a copy, so code that writes or deletes files cannot harm /work.
cp -r /work "$out/kata"
cd "$out/kata" || exit 1

mapfile -t modules < <(
    find . -name '*.py' -not -path './tests/*' -not -path './.*' -not -path '*/__pycache__/*' \
        | sed 's#^\./##' | LC_ALL=C sort
)
if [ "${#modules[@]}" -eq 0 ]; then
    echo "error: no .py modules found in the kata folder"
    exit 1
fi

# A syntax error is Python's "does not compile".
if ! python - "${modules[@]}" > "$out/syntax.log" 2>&1 <<'EOF'
import sys
import traceback

broken = False
for name in sys.argv[1:]:
    with open(name, encoding="utf-8") as source:
        text = source.read()
    try:
        compile(text, name, "exec")
    except SyntaxError as error:
        broken = True
        print("".join(traceback.format_exception_only(error)).rstrip())
sys.exit(1 if broken else 0)
EOF
then
    echo "error: the code does not compile"
    cat "$out/syntax.log"
    exit 1
fi

python -m pytest tests -p no:cacheprovider -q -o junit_family=xunit2 \
    --junitxml="$out/junit.xml" > "$out/pytest.log" 2>&1

python /opt/dojo/junit2cargo.py "$out/junit.xml" "${modules[@]}"
status=$?
if [ "$status" -eq 2 ]; then
    echo "error: no tests ran"
    head -40 "$out/pytest.log"
fi
exit "$status"
