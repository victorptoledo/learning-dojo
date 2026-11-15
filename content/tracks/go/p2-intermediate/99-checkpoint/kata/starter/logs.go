// Package logs parses, filters, counts and writes log entries.
package logs

import (
	"fmt"
	"io"
	"iter"
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

// String returns "DEBUG", "INFO", "WARN" or "ERROR", and "Level(n)" for other values.
func (l Level) String() string {
	panic("todo: write Level.String")
}

// ParseLevel reads a level name in any case; "warning" is accepted for Warn.
func ParseLevel(name string) (Level, error) {
	panic("todo: write ParseLevel")
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
	panic("todo: write Parse")
}

// Matcher decides whether an entry is kept.
type Matcher interface {
	Match(Entry) bool
}

// MinLevel matches entries at this level or above.
type MinLevel Level

// Match implements Matcher.
func (m MinLevel) Match(e Entry) bool {
	panic("todo: write MinLevel.Match")
}

// ServiceIs matches entries of one service.
type ServiceIs string

// Match implements Matcher.
func (s ServiceIs) Match(e Entry) bool {
	panic("todo: write ServiceIs.Match")
}

// MatcherFunc lets a plain function be a Matcher.
type MatcherFunc func(Entry) bool

// Match implements Matcher by calling f.
func (f MatcherFunc) Match(e Entry) bool {
	panic("todo: write MatcherFunc.Match")
}

// All matches the entries every matcher accepts; with no matchers it matches everything.
func All(matchers ...Matcher) Matcher {
	panic("todo: write All")
}

// Select lazily yields the entries m accepts, in order.
func Select(entries []Entry, m Matcher) iter.Seq[Entry] {
	panic("todo: write Select")
}

// CountBy counts the entries of seq per key.
func CountBy[K comparable](seq iter.Seq[Entry], key func(Entry) K) map[K]int {
	panic("todo: write CountBy")
}

// Write prints one line per entry, "15:04:05 LEVEL service: message", and stops at the
// first write error.
func Write(w io.Writer, seq iter.Seq[Entry]) error {
	panic("todo: write Write")
}
