---
from: java
status: draft
concept: "Searching, editing and summarising text"
from_way: "java.util.regex.Pattern, String.replaceAll, and a small program with a BufferedReader and a HashMap to total a column."
native_way: "grep, sed and awk, which use the same regular expression ideas and do the reading, splitting and grouping for you in one line."
sensei_tip: "Pattern.compile uses Perl-style syntax close to grep -P and sed -E, but \\d is not in POSIX regexes: write [0-9] or [[:digit:]] in grep -E, sed and awk."
---

| Java | Shell |
| --- | --- |
| `Pattern.compile("\\d+ms").matcher(line).find()` | `grep -E '[0-9]+ms'` |
| `line.replaceAll("http://", "https://")` | `sed 's|http://|https://|g'` |
| `line.split(",")[2]` | `cut -d, -f3` or `awk -F, '{ print $3 }'` |
| `map.merge(country, amount, Double::sum)` | `awk -F, '{ sum[$3] += $4 }'` |
| `String.format("%-10s %8.2f", a, b)` | `printf "%-10s %8.2f\n", a, b` in awk |
| Jackson `JsonNode.path("items")` | `jq '.items'` |
