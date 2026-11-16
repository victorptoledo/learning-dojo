#!/bin/bash
# Checks an AWS kata in /work, offline and with no AWS account:
#   1. every learner file must parse: Python modules compile, templates and policies are
#      valid YAML or JSON (CloudFormation short forms such as !Ref included), and Guard
#      rule files parse,
#   2. the visible and grading tests (staged in /work/tests) run with pytest; they read the
#      files with the helpers in /opt/dojo/dojo_aws (templates, the IAM policy evaluator,
#      cfn-guard) and run boto3 code against moto,
#   3. report.py adds one cfn-lint check per CloudFormation template and one ruff check
#      per Python module.
# Prints the result in `cargo test` format.
set -u

# boto3 needs credentials to sign requests even against moto. These are fake, and the
# container runs with no network, so nothing can reach AWS.
export AWS_ACCESS_KEY_ID=testing AWS_SECRET_ACCESS_KEY=testing AWS_SESSION_TOKEN=testing

out=/target
rm -rf "${out:?}"/*

# Everything runs on a copy. Windows editors may save CRLF line ends, which YAML block
# scalars (inline Lambda code, Guard rules) would keep as part of the text.
cp -r /work "$out/kata"
find "$out/kata" -type f \( -name '*.yaml' -o -name '*.yml' -o -name '*.json' -o -name '*.py' \
    -o -name '*.guard' -o -name '*.txt' \) -exec sed -i 's/\r$//' {} +
cd "$out/kata" || exit 1

learner_files() {
    find . -type f \( "$@" \) -not -path './tests/*' -not -path './.*' -not -path '*/__pycache__/*' \
        | sed 's#^\./##' | LC_ALL=C sort
}
mapfile -t modules < <(learner_files -name '*.py')
mapfile -t documents < <(learner_files -name '*.yaml' -o -name '*.yml' -o -name '*.json')
mapfile -t rules < <(learner_files -name '*.guard')
if [ "$((${#modules[@]} + ${#documents[@]} + ${#rules[@]}))" -eq 0 ]; then
    echo "error: no templates, policies, rules or Python modules found in the kata folder"
    exit 1
fi

# A file that does not parse is this kata's "does not compile".
if ! python - "${modules[@]}" -- "${documents[@]}" > "$out/syntax.log" 2>&1 <<'EOF'
import sys
import traceback

from dojo_aws import load

args = sys.argv[1:]
split = args.index("--")
broken = False
for name in args[:split]:
    with open(name, encoding="utf-8") as source:
        text = source.read()
    try:
        compile(text, name, "exec")
    except SyntaxError as error:
        broken = True
        print("".join(traceback.format_exception_only(error)).rstrip())
for name in args[split + 1:]:
    try:
        load(name)
    except ValueError as error:
        broken = True
        print(error)
sys.exit(1 if broken else 0)
EOF
then
    echo "error: a file does not parse"
    cat "$out/syntax.log"
    exit 1
fi
for file in "${rules[@]}"; do
    if ! cfn-guard parse-tree --rules "$file" > /dev/null 2> "$out/guard.log"; then
        echo "error: the Guard rules in $file do not parse"
        cat "$out/guard.log"
        exit 1
    fi
done

python -m pytest tests -p no:cacheprovider -q -o junit_family=xunit2 \
    --junitxml="$out/junit.xml" > "$out/pytest.log" 2>&1

python /opt/dojo/report.py "$out/junit.xml" "${modules[@]}" -- "${documents[@]}"
status=$?
if [ "$status" -eq 2 ]; then
    echo "error: no tests ran"
    head -40 "$out/pytest.log"
fi
exit "$status"
