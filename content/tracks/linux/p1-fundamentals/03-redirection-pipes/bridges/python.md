---
from: python
status: draft
concept: "Streams of data between programs"
from_way: "print() to sys.stdout, logging to sys.stderr, subprocess.run to call programs, and list comprehensions or collections.Counter to summarise data."
native_way: "stdout and stderr between programs, and pipelines of small tools that stream text while they run."
sensei_tip: "A Python script fits in a pipeline when it reads sys.stdin line by line and writes results to stdout, errors to stderr, and returns a proper exit code with sys.exit."
---

| Python | Linux |
| --- | --- |
| print(x) | echo "$x" |
| print(e, file=sys.stderr) | echo "$e" >&2 |
| sys.exit(2) | exit 2 |
| Counter(lines).most_common(3) | sort \| uniq -c \| sort -rn \| head -n 3 |
| subprocess.run(..., check=True) | cmd && next |
