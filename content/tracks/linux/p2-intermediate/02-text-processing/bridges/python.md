---
from: python
status: draft
concept: "Searching, editing and summarising text"
from_way: "The re module, str.replace, the csv module and a dict to total a column."
native_way: "grep, sed and awk do the reading, splitting and grouping in one line each."
sensei_tip: "Python's re uses \\d and \\w, which grep -E and awk do not understand: write [0-9] and [[:alnum:]_]. For real CSV with quoted commas, Python's csv module is the safer tool."
---

| Python | Linux |
| --- | --- |
| re.search(r'\d+ms', line) | grep -E '[0-9]+ms' |
| line.replace('http://', 'https://') | sed 's\|http://\|https://\|g' |
| line.split(',')[2] | cut -d, -f3 |
| totals[c] += amount | awk -F, '{ t[$1] += $2 }' |
| json.load | jq |
