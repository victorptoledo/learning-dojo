---
from: javascript
status: draft
concept: "Writing small programs"
from_way: "Dynamic types, exceptions, async functions and npm packages; scripts run with node or zx."
native_way: "Strings and exit statuses, sequential commands, and the shell's own expansion rules."
sensei_tip: "Google's zx library brings shell commands into JavaScript with proper quoting. In plain bash, quote every expansion: there is no template literal that escapes for you."
---

| JavaScript / TypeScript | Linux |
| --- | --- |
| const name = 'Ada' | name='Ada' |
| process.argv[2] | "$1" |
| if (!s) | if [[ -z "$s" ]] |
| for (const h of hosts) | for h in "${hosts[@]}" |
| throw new Error() | echo error >&2; exit 1 |
| finally { cleanup() } | trap cleanup EXIT |
