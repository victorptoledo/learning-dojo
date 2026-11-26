// Package config loads a service's JSON configuration strictly: unknown fields, trailing
// data and bad values are errors, and every problem is reported at once.
package config

import (
	"errors"
	"io"
	"time"
)

// ErrTrailingData means the input had more than one JSON value.
var ErrTrailingData = errors.New("config: data after the JSON object")

// ErrNegativeDuration means a duration was below zero.
var ErrNegativeDuration = errors.New("negative duration")

// Default values for fields the file leaves out.
const (
	DefaultTimeout = 5 * time.Second
	DefaultRetries = 3
)

// Duration is a time.Duration written in JSON as a string such as "1m30s".
type Duration time.Duration

// MarshalText writes the duration the way time.Duration.String does.
func (d Duration) MarshalText() ([]byte, error) {
	panic("todo: write MarshalText")
}

// UnmarshalText reads a duration with time.ParseDuration and rejects negative values.
func (d *Duration) UnmarshalText(text []byte) error {
	panic("todo: write UnmarshalText")
}

// Config is the configuration of one service. Add the struct tags.
type Config struct {
	Name    string
	Listen  string
	Timeout Duration
	Retries *int
	Tags    []string
}

// FieldError is one invalid field.
type FieldError struct {
	Field   string
	Problem string
}

func (e *FieldError) Error() string { return e.Field + ": " + e.Problem }

// Load decodes one JSON object from r, fills the defaults and validates the result.
func Load(r io.Reader) (Config, error) {
	panic("todo: write Load")
}

// Validate reports every invalid field, joined.
func (c Config) Validate() error {
	panic("todo: write Validate")
}

// Save writes c as indented JSON (two spaces) followed by a newline.
func Save(w io.Writer, c Config) error {
	panic("todo: write Save")
}
