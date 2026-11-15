package logs_test

import (
	"bytes"
	"errors"
	"maps"
	"strings"
	"testing"
	"testing/iotest"
	"time"

	"dojo/logs"
)

const sample = `# morning logs
2026-11-15T10:00:00Z INFO api request served
2026-11-15T10:00:01Z debug api cache miss for /users/7

2026-11-15T10:00:02Z Warning payments retrying charge 42
2026-11-15T10:00:03Z ERROR payments card declined: insufficient funds
2026-11-15T10:00:04Z ERROR api upstream timeout
`

func parsed(t *testing.T) []logs.Entry {
	t.Helper()
	entries, err := logs.Parse(strings.NewReader(sample))
	if err != nil {
		t.Fatalf("Parse(sample) = %v, want nil", err)
	}
	if len(entries) != 5 {
		t.Fatalf("Parse(sample) returned %d entries, want 5", len(entries))
	}
	return entries
}

func TestLevelStringAndParseLevel(t *testing.T) {
	for level, name := range map[logs.Level]string{logs.Debug: "DEBUG", logs.Info: "INFO", logs.Warn: "WARN", logs.Error: "ERROR", logs.Level(7): "Level(7)"} {
		if got := level.String(); got != name {
			t.Errorf("Level(%d).String() = %q, want %q", int(level), got, name)
		}
	}
	for name, want := range map[string]logs.Level{"debug": logs.Debug, "Info": logs.Info, "WARN": logs.Warn, "warning": logs.Warn, "error": logs.Error} {
		if got, err := logs.ParseLevel(name); err != nil || got != want {
			t.Errorf("ParseLevel(%q) = %v, %v; want %v, nil", name, got, err, want)
		}
	}
	if _, err := logs.ParseLevel("fatal"); err == nil {
		t.Error(`ParseLevel("fatal") returned no error`)
	}
}

func TestParseSkipsCommentsAndBlankLines(t *testing.T) {
	entries := parsed(t)
	first := entries[0]
	want := time.Date(2026, 11, 15, 10, 0, 0, 0, time.UTC)
	if !first.Time.Equal(want) || first.Level != logs.Info || first.Service != "api" || first.Message != "request served" {
		t.Errorf("first entry = %+v, want 10:00:00 INFO api \"request served\"", first)
	}
	if entries[2].Level != logs.Warn || entries[3].Message != "card declined: insufficient funds" {
		t.Errorf("entries 3 and 4 = %+v, %+v", entries[2], entries[3])
	}
}

func TestParseCollectsBadLinesAndKeepsTheGoodOnes(t *testing.T) {
	input := "2026-11-15T10:00:00Z INFO api ok\n" +
		"yesterday INFO api bad time\n" +
		"2026-11-15T10:00:01Z LOUD api bad level\n" +
		"2026-11-15T10:00:02Z INFO\n" +
		"2026-11-15T10:00:03Z WARN api still parsed\n"
	entries, err := logs.Parse(strings.NewReader(input))
	if len(entries) != 2 {
		t.Errorf("Parse kept %d entries, want the 2 good ones", len(entries))
	}
	var parseErr *logs.ParseError
	if !errors.As(err, &parseErr) || parseErr.Line != 2 {
		t.Fatalf("Parse error = %v, want a *ParseError for line 2 first", err)
	}
	joined, ok := err.(interface{ Unwrap() []error })
	if !ok || len(joined.Unwrap()) != 3 {
		t.Errorf("Parse should return errors.Join of 3 *ParseError, got %q", err)
	}
	if !strings.Contains(err.Error(), "line 4: ") {
		t.Errorf("error %q should mention line 4", err)
	}
}

func TestParseStopsAndWrapsAReaderFailure(t *testing.T) {
	broken := errors.New("disk on fire")
	_, err := logs.Parse(iotest.ErrReader(broken))
	if !errors.Is(err, broken) {
		t.Errorf("Parse(failing reader) = %v, want an error wrapping the reader's error", err)
	}
}

func TestMatchers(t *testing.T) {
	entries := parsed(t)
	warn, payments := logs.MinLevel(logs.Warn), logs.ServiceIs("payments")
	if warn.Match(entries[0]) || !warn.Match(entries[2]) || !warn.Match(entries[3]) {
		t.Error("MinLevel(Warn) should reject INFO and accept WARN and ERROR")
	}
	if payments.Match(entries[0]) || !payments.Match(entries[2]) {
		t.Error("ServiceIs(payments) should match only the payments entries")
	}
	long := logs.MatcherFunc(func(e logs.Entry) bool { return len(e.Message) > 15 })
	if !long.Match(entries[1]) || long.Match(entries[0]) {
		t.Error("MatcherFunc should call the function")
	}
	var _ logs.Matcher = long
}

func TestAllCombinesMatchers(t *testing.T) {
	entries := parsed(t)
	both := logs.All(logs.MinLevel(logs.Error), logs.ServiceIs("api"))
	var kept []string
	for _, e := range entries {
		if both.Match(e) {
			kept = append(kept, e.Message)
		}
	}
	if len(kept) != 1 || kept[0] != "upstream timeout" {
		t.Errorf("All(ERROR, api) kept %q, want [upstream timeout]", kept)
	}
	if !logs.All().Match(entries[0]) {
		t.Error("All() with no matchers should match everything")
	}
}

func TestSelectIsLazyAndStops(t *testing.T) {
	entries := parsed(t)
	calls := 0
	counting := logs.MatcherFunc(func(e logs.Entry) bool { calls++; return e.Level >= logs.Warn })
	seq := logs.Select(entries, counting)
	if calls != 0 {
		t.Fatalf("Select called the matcher %d times before the loop; it must be lazy", calls)
	}
	for e := range seq {
		if e.Level != logs.Warn {
			t.Errorf("first selected entry = %+v, want the WARN one", e)
		}
		break
	}
	if calls != 3 {
		t.Errorf("the matcher ran %d times, want 3 (up to the first WARN)", calls)
	}
}

func TestCountBy(t *testing.T) {
	entries := parsed(t)
	all := logs.Select(entries, logs.All())
	byService := logs.CountBy(all, func(e logs.Entry) string { return e.Service })
	if want := map[string]int{"api": 3, "payments": 2}; !maps.Equal(byService, want) {
		t.Errorf("CountBy(service) = %v, want %v", byService, want)
	}
	byLevel := logs.CountBy(logs.Select(entries, logs.All()), func(e logs.Entry) logs.Level { return e.Level })
	if want := map[logs.Level]int{logs.Debug: 1, logs.Info: 1, logs.Warn: 1, logs.Error: 2}; !maps.Equal(byLevel, want) {
		t.Errorf("CountBy(level) = %v, want %v", byLevel, want)
	}
}

func TestWriteFormatsEntries(t *testing.T) {
	entries := parsed(t)
	var out bytes.Buffer
	if err := logs.Write(&out, logs.Select(entries, logs.MinLevel(logs.Error))); err != nil {
		t.Fatalf("Write = %v", err)
	}
	want := "10:00:03 ERROR payments: card declined: insufficient funds\n10:00:04 ERROR api: upstream timeout\n"
	if out.String() != want {
		t.Errorf("Write wrote %q, want %q", out.String(), want)
	}
}

type failingWriter struct{ writes int }

var errFull = errors.New("disk full")

func (f *failingWriter) Write(p []byte) (int, error) {
	f.writes++
	return 0, errFull
}

func TestWriteStopsAtTheFirstWriteError(t *testing.T) {
	entries := parsed(t)
	w := &failingWriter{}
	err := logs.Write(w, logs.Select(entries, logs.All()))
	if !errors.Is(err, errFull) {
		t.Errorf("Write = %v, want an error wrapping the writer's error", err)
	}
	if w.writes != 1 {
		t.Errorf("Write tried %d writes, want to stop after the first failure", w.writes)
	}
}
