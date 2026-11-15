// Package logs parses, filters, counts and writes log entries.
package logs

import (
	"bufio"
	"errors"
	"fmt"
	"io"
	"iter"
	"strings"
	"time"
)

// Level is the severity of an entry, from Debug to Error.
type Level int

// The levels, in increasing severity.
const (
	Debug Level = iota
	Info
	Warn
	Error
)

var levelNames = []string{"DEBUG", "INFO", "WARN", "ERROR"}

// String returns "DEBUG", "INFO", "WARN" or "ERROR", and "Level(n)" for other values.
func (l Level) String() string {
	if l < Debug || l > Error {
		return fmt.Sprintf("Level(%d)", int(l))
	}
	return levelNames[l]
}

// ParseLevel reads a level name in any case; "warning" is accepted for Warn.
func ParseLevel(name string) (Level, error) {
	upper := strings.ToUpper(name)
	if upper == "WARNING" {
		return Warn, nil
	}
	for i, known := range levelNames {
		if upper == known {
			return Level(i), nil
		}
	}
	return 0, fmt.Errorf("unknown level %q", name)
}

// Entry is one parsed log line.
type Entry struct {
	Time    time.Time
	Level   Level
	Service string
	Message string
}

// ParseError is a line that Parse could not read.
type ParseError struct {
	Line int
	Err  error
}

func (e *ParseError) Error() string { return fmt.Sprintf("line %d: %v", e.Line, e.Err) }

// Unwrap returns the cause.
func (e *ParseError) Unwrap() error { return e.Err }

// Parse reads lines such as "2026-11-15T10:00:00Z ERROR payments card declined" from r.
// Blank lines and lines starting with # are skipped. A bad line does not stop the others:
// the error joins one *ParseError per bad line. A failure of r itself stops the parse.
func Parse(r io.Reader) ([]Entry, error) {
	var entries []Entry
	var problems []error
	scanner := bufio.NewScanner(r)
	for number := 1; scanner.Scan(); number++ {
		line := strings.TrimSpace(scanner.Text())
		if line == "" || strings.HasPrefix(line, "#") {
			continue
		}
		entry, err := parseLine(line)
		if err != nil {
			problems = append(problems, &ParseError{Line: number, Err: err})
			continue
		}
		entries = append(entries, entry)
	}
	if err := scanner.Err(); err != nil {
		return entries, fmt.Errorf("read logs: %w", err)
	}
	return entries, errors.Join(problems...)
}

func parseLine(line string) (Entry, error) {
	parts := strings.SplitN(line, " ", 4)
	if len(parts) < 4 || strings.TrimSpace(parts[3]) == "" {
		return Entry{}, errors.New("want: TIME LEVEL SERVICE MESSAGE")
	}
	when, err := time.Parse(time.RFC3339, parts[0])
	if err != nil {
		return Entry{}, fmt.Errorf("bad time: %w", err)
	}
	level, err := ParseLevel(parts[1])
	if err != nil {
		return Entry{}, err
	}
	return Entry{Time: when, Level: level, Service: parts[2], Message: strings.TrimSpace(parts[3])}, nil
}

// Matcher decides whether an entry is kept.
type Matcher interface {
	Match(Entry) bool
}

// MinLevel matches entries at this level or above.
type MinLevel Level

// Match implements Matcher.
func (m MinLevel) Match(e Entry) bool { return e.Level >= Level(m) }

// ServiceIs matches entries of one service.
type ServiceIs string

// Match implements Matcher.
func (s ServiceIs) Match(e Entry) bool { return e.Service == string(s) }

// MatcherFunc lets a plain function be a Matcher.
type MatcherFunc func(Entry) bool

// Match implements Matcher by calling f.
func (f MatcherFunc) Match(e Entry) bool { return f(e) }

// All matches the entries every matcher accepts; with no matchers it matches everything.
func All(matchers ...Matcher) Matcher {
	return MatcherFunc(func(e Entry) bool {
		for _, m := range matchers {
			if !m.Match(e) {
				return false
			}
		}
		return true
	})
}

// Select lazily yields the entries m accepts, in order.
func Select(entries []Entry, m Matcher) iter.Seq[Entry] {
	return func(yield func(Entry) bool) {
		for _, e := range entries {
			if m.Match(e) && !yield(e) {
				return
			}
		}
	}
}

// CountBy counts the entries of seq per key.
func CountBy[K comparable](seq iter.Seq[Entry], key func(Entry) K) map[K]int {
	counts := make(map[K]int)
	for e := range seq {
		counts[key(e)]++
	}
	return counts
}

// Write prints one line per entry, "15:04:05 LEVEL service: message", and stops at the
// first write error.
func Write(w io.Writer, seq iter.Seq[Entry]) error {
	for e := range seq {
		_, err := fmt.Fprintf(w, "%s %s %s: %s\n", e.Time.UTC().Format(time.TimeOnly), e.Level, e.Service, e.Message)
		if err != nil {
			return fmt.Errorf("write logs: %w", err)
		}
	}
	return nil
}
