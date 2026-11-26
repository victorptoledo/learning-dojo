// Package config loads a service's JSON configuration strictly: unknown fields, trailing
// data and bad values are errors, and every problem is reported at once.
package config

import (
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"net"
	"strconv"
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
	return []byte(time.Duration(d).String()), nil
}

// UnmarshalText reads a duration with time.ParseDuration and rejects negative values.
func (d *Duration) UnmarshalText(text []byte) error {
	parsed, err := time.ParseDuration(string(text))
	if err != nil {
		return err
	}
	if parsed < 0 {
		return fmt.Errorf("%q: %w", text, ErrNegativeDuration)
	}
	*d = Duration(parsed)
	return nil
}

// Config is the configuration of one service.
type Config struct {
	Name    string   `json:"name"`
	Listen  string   `json:"listen"`
	Timeout Duration `json:"timeout,omitzero"`
	Retries *int     `json:"retries,omitempty"`
	Tags    []string `json:"tags,omitempty"`
}

// FieldError is one invalid field.
type FieldError struct {
	Field   string
	Problem string
}

func (e *FieldError) Error() string { return e.Field + ": " + e.Problem }

// Load decodes one JSON object from r, fills the defaults and validates the result.
func Load(r io.Reader) (Config, error) {
	var c Config
	dec := json.NewDecoder(r)
	dec.DisallowUnknownFields()
	if err := dec.Decode(&c); err != nil {
		return Config{}, fmt.Errorf("decode config: %w", err)
	}
	if err := dec.Decode(&struct{}{}); !errors.Is(err, io.EOF) {
		return Config{}, ErrTrailingData
	}
	if c.Timeout == 0 {
		c.Timeout = Duration(DefaultTimeout)
	}
	if c.Retries == nil {
		c.Retries = new(int(DefaultRetries))
	}
	if err := c.Validate(); err != nil {
		return Config{}, err
	}
	return c, nil
}

// Validate reports every invalid field, joined.
func (c Config) Validate() error {
	var problems []error
	bad := func(field, problem string) {
		problems = append(problems, &FieldError{Field: field, Problem: problem})
	}
	if c.Name == "" {
		bad("name", "is required")
	}
	if _, port, err := net.SplitHostPort(c.Listen); err != nil {
		bad("listen", "must be host:port")
	} else if n, err := strconv.Atoi(port); err != nil || n < 1 || n > 65535 {
		bad("listen", "port must be a number from 1 to 65535")
	}
	if time.Duration(c.Timeout) > time.Minute {
		bad("timeout", "must be at most 1m0s")
	}
	if c.Retries != nil && (*c.Retries < 0 || *c.Retries > 10) {
		bad("retries", "must be from 0 to 10")
	}
	seen := make(map[string]bool, len(c.Tags))
	for _, tag := range c.Tags {
		switch {
		case tag == "":
			bad("tags", "must not be empty")
		case seen[tag]:
			bad("tags", fmt.Sprintf("%q appears twice", tag))
		}
		seen[tag] = true
	}
	return errors.Join(problems...)
}

// Save writes c as indented JSON (two spaces) followed by a newline.
func Save(w io.Writer, c Config) error {
	enc := json.NewEncoder(w)
	enc.SetIndent("", "  ")
	if err := enc.Encode(c); err != nil {
		return fmt.Errorf("encode config: %w", err)
	}
	return nil
}
