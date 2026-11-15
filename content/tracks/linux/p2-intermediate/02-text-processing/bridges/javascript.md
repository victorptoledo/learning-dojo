---
from: javascript
status: draft
concept: "Searching, editing and summarising text"
from_way: "Regular expressions in String.match and replace, split(','), and reduce to group values."
native_way: "grep, sed and awk on streams of lines, with POSIX regular expressions."
sensei_tip: "JavaScript regexes and grep -E agree on most syntax, but lookarounds and \\d are not POSIX. jq is to JSON on the command line what JSON.parse plus map is in Node."
---

| JavaScript / TypeScript | Linux |
| --- | --- |
| /\d+ms/.test(line) | grep -E '[0-9]+ms' |
| s.replaceAll('a', 'b') | sed 's/a/b/g' |
| line.split(',')[2] | cut -d, -f3 |
| rows.reduce((t, r) => ..., {}) | awk with an associative array |
| JSON.parse(s).items.map(i => i.name) | jq -r '.items[].name' |
