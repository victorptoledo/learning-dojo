---
from: go
status: draft
concept: "Writing small programs"
from_way: "Static types, explicit error values and a compiler; Go programs replace many scripts in DevOps teams."
native_way: "Bash: no types, exit statuses as errors, and set -e as a partial safety net."
sensei_tip: "Go's if err != nil is the same idea as checking $? after every command. In bash, set -euo pipefail does that checking for you, with exceptions you must know (if, &&, ||)."
---

| Go | Linux |
| --- | --- |
| name := "Ada" | name="Ada" |
| os.Args[1], len(os.Args) | "$1", $# |
| if s == "" | if [[ -z "$s" ]] |
| for _, h := range hosts | for h in "${hosts[@]}" |
| if err != nil { return err } | set -e, or cmd \|\| exit 1 |
| defer cleanup() | trap cleanup EXIT |
| flag package | getopts |
