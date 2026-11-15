---
from: csharp
status: draft
concept: "Searching, editing and summarising text"
from_way: "Regex, string.Split and LINQ GroupBy with Sum to report over a file."
native_way: "grep, sed and awk, which stream the file and need no program around them."
sensei_tip: "A LINQ query such as File.ReadLines(f).GroupBy(...).Select(g => (g.Key, g.Sum(...))) maps almost one to one onto awk's array plus END block."
---

| C# | Linux |
| --- | --- |
| Regex.IsMatch(line, @"\d+ms") | grep -E '[0-9]+ms' |
| Regex.Replace(s, "a", "b") | sed -E 's/a/b/g' |
| line.Split(',')[2] | cut -d, -f3 |
| GroupBy(r => r.Country).Select(g => g.Sum(r => r.Amount)) | awk -F, '{ s[$3] += $4 }' |
