---
from: go
status: draft
concept: "Searching, editing and summarising text"
from_way: "regexp, strings.Split and a map[string]float64 to total a column."
native_way: "grep, sed and awk, which do the same with no compile step."
sensei_tip: "Go's regexp is RE2: no backreferences, and \\d is supported, unlike grep -E. For quick one-off reports, awk is faster to write than a Go program; for a tool you ship, Go is easier to test."
---

| Go | Linux |
| --- | --- |
| regexp.MustCompile(`\d+ms`) | grep -E '[0-9]+ms' |
| strings.ReplaceAll(s, a, b) | sed 's/a/b/g' |
| strings.Split(line, ",")[2] | cut -d, -f3 |
| totals[c] += amount | awk -F, '{ t[$1] += $2 }' |
| encoding/json | jq |
