---
from: python
status: draft
concept: "Writing small programs"
from_way: "A real programming language with types, exceptions, functions and libraries; scripts are .py files run with python3."
native_way: "Untyped strings, functions that return a status, exit codes instead of exceptions, and ShellCheck as the safety net."
sensei_tip: "Use bash to glue commands together and Python as soon as you need data structures, JSON or real error handling. subprocess.run([...], check=True) replaces most bash scripts that grew too big."
---

| Python | Linux |
| --- | --- |
| name = 'Ada' | name='Ada' |
| sys.argv[1], len(sys.argv) | "$1", $# |
| if not s: | if [[ -z "$s" ]] |
| for host in hosts: | for host in "${hosts[@]}" |
| raise SystemExit(2) | exit 2 |
| try/finally | trap cleanup EXIT |
| argparse | getopts |
