# Checkpoint 2: The Log Pipeline

Build a small log tool in `logs.go`: parse entries from any `io.Reader`, filter them with
matchers that satisfy an interface, select them lazily, count them with a generic function
and write them to any `io.Writer`. The types are declared; the functions are yours.

A log line has four parts separated by single spaces, the message being the rest of the line:

```text
2026-11-15T10:00:03Z ERROR payments card declined: insufficient funds
TIME (RFC 3339)      LEVEL SERVICE  MESSAGE
```

| Function or method | Does |
| --- | --- |
| `Level.String()` | `"DEBUG"`, `"INFO"`, `"WARN"`, `"ERROR"`, and `"Level(7)"` for unknown values |
| `ParseLevel(name)` | The level for a name in any case (`"warning"` also means `Warn`), or an error |
| `Parse(r io.Reader) ([]Entry, error)` | Reads every line (skip blank lines and lines starting with `#`, trim spaces). A bad line (bad time, unknown level, missing parts or an empty message) does **not** stop the parse: return the good entries and `errors.Join` of one `*ParseError{Line, Err}` per bad line (lines count from 1). If **reading** fails, return an error that wraps the reader's error. |
| `MinLevel.Match`, `ServiceIs.Match` | `MinLevel(Warn)` keeps WARN and ERROR; `ServiceIs("api")` keeps the api entries |
| `MatcherFunc.Match` | Calls the function, so any `func(Entry) bool` can be a `Matcher` (like `http.HandlerFunc`) |
| `All(matchers ...Matcher) Matcher` | Keeps the entries every matcher keeps; `All()` keeps everything |
| `Select(entries, m) iter.Seq[Entry]` | **Lazily** yields the kept entries in order: no matcher call before the loop starts, and none after it breaks |
| `CountBy[K comparable](seq, key)` | How many entries per key: per service, per level, per hour… |
| `Write(w io.Writer, seq)` | One line per entry, `"10:00:03 ERROR payments: card declined: insufficient funds\n"` (the time in UTC as `15:04:05`); stop at the first write error and return it wrapped |

```go
entries, err := logs.Parse(file)
errorsOfAPI := logs.Select(entries, logs.All(logs.MinLevel(logs.Error), logs.ServiceIs("api")))
logs.Write(os.Stdout, errorsOfAPI)
logs.CountBy(logs.Select(entries, logs.All()), func(e logs.Entry) string { return e.Service })
```

Tools: `bufio.Scanner` (and its `Err()`), `strings.SplitN`, `time.Parse(time.RFC3339, ...)`,
`time.TimeOnly`, `fmt.Fprintf`, `errors.Join`, and the tests use `strings.NewReader`,
`bytes.Buffer` and `iotest.ErrReader` as readers and writers.

Run the visible test with `go test`. The dojo runs 11 tests: 1 in `logs_test.go` and 10
hidden ones, plus `go vet` and `gofmt` on `logs.go`.
