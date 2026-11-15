---
from: javascript
status: draft
concept: "Streams of data between programs"
from_way: "console.log and console.error, process.exit, and array methods such as filter, map and reduce over data in memory."
native_way: "Separate processes connected by pipes, streaming lines instead of holding everything in memory."
sensei_tip: "console.log writes to stdout and console.error to stderr, so a Node script can sit in a pipeline. Set process.exitCode instead of calling process.exit() so buffered output is not lost."
---

| JavaScript / TypeScript | Linux |
| --- | --- |
| console.log(x) | echo "$x" |
| console.error(e) | echo "$e" >&2 |
| process.exitCode = 2 | exit 2 |
| lines.filter(l => l.includes('ERROR')).length | grep -c ERROR |
| readline over process.stdin | reading stdin in a pipeline |
