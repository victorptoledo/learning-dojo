package config_test

import (
	"bytes"
	"encoding/json"
	"errors"
	"slices"
	"strings"
	"testing"
	"time"

	"dojo/config"
)

func load(t *testing.T, input string) config.Config {
	t.Helper()
	c, err := config.Load(strings.NewReader(input))
	if err != nil {
		t.Fatalf("Load(%s) = %v, want nil", input, err)
	}
	return c
}

func loadErr(t *testing.T, input string) error {
	t.Helper()
	_, err := config.Load(strings.NewReader(input))
	if err == nil {
		t.Fatalf("Load(%s) = nil error, want one", input)
	}
	return err
}

func fieldErrors(err error) []string {
	var out []string
	var visit func(error)
	visit = func(err error) {
		if fe, ok := err.(*config.FieldError); ok {
			out = append(out, fe.Error())
			return
		}
		if joined, ok := err.(interface{ Unwrap() []error }); ok {
			for _, e := range joined.Unwrap() {
				visit(e)
			}
			return
		}
		if inner := errors.Unwrap(err); inner != nil {
			visit(inner)
		}
	}
	visit(err)
	slices.Sort(out)
	return out
}

func TestDurationTextRoundTrip(t *testing.T) {
	for _, d := range []time.Duration{0, 1500 * time.Millisecond, 90 * time.Second, 2 * time.Hour} {
		text, err := config.Duration(d).MarshalText()
		if err != nil || string(text) != d.String() {
			t.Errorf("Duration(%v).MarshalText() = %q, %v, want %q", d, text, err, d.String())
		}
		var back config.Duration
		if err := back.UnmarshalText(text); err != nil || time.Duration(back) != d {
			t.Errorf("UnmarshalText(%q) = %v, %v, want %v", text, time.Duration(back), err, d)
		}
	}
	var d config.Duration
	if err := d.UnmarshalText([]byte("ten seconds")); err == nil {
		t.Error(`UnmarshalText("ten seconds") = nil, want an error`)
	}
	if err := d.UnmarshalText([]byte("-5s")); !errors.Is(err, config.ErrNegativeDuration) {
		t.Errorf(`UnmarshalText("-5s") = %v, want ErrNegativeDuration`, err)
	}
}

func TestDefaultsOnlyForMissingFields(t *testing.T) {
	c := load(t, `{"name": "billing", "listen": ":8080"}`)
	if time.Duration(c.Timeout) != config.DefaultTimeout {
		t.Errorf("Timeout = %v, want the default %v", time.Duration(c.Timeout), config.DefaultTimeout)
	}
	if c.Retries == nil || *c.Retries != config.DefaultRetries {
		t.Errorf("Retries = %v, want a pointer to the default %d", c.Retries, config.DefaultRetries)
	}
	c = load(t, `{"name": "billing", "listen": ":8080", "retries": 0}`)
	if c.Retries == nil || *c.Retries != 0 {
		t.Errorf(`with "retries": 0, Retries = %v, want a pointer to 0: an explicit zero is not a missing field`, c.Retries)
	}
}

func TestDurationsMustBeStrings(t *testing.T) {
	err := loadErr(t, `{"name": "billing", "listen": ":8080", "timeout": 30}`)
	var typeErr *json.UnmarshalTypeError
	if !errors.As(err, &typeErr) {
		t.Errorf(`"timeout": 30 gave %v, want a *json.UnmarshalTypeError: a bare number has no unit`, err)
	}
	err = loadErr(t, `{"name": "billing", "listen": ":8080", "timeout": "-1s"}`)
	if !errors.Is(err, config.ErrNegativeDuration) {
		t.Errorf(`"timeout": "-1s" gave %v, want it to wrap ErrNegativeDuration`, err)
	}
}

func TestUnknownFieldsAreErrors(t *testing.T) {
	err := loadErr(t, `{"name": "billing", "listn": ":8080"}`)
	if !strings.Contains(err.Error(), `unknown field "listn"`) {
		t.Errorf("Load with a typo = %v, want an unknown field error naming \"listn\"", err)
	}
}

func TestSyntaxErrorsAreWrapped(t *testing.T) {
	err := loadErr(t, `{"name": "billing",}`)
	var syntax *json.SyntaxError
	if !errors.As(err, &syntax) {
		t.Fatalf("Load of broken JSON = %v, want a wrapped *json.SyntaxError", err)
	}
	if !strings.HasPrefix(err.Error(), "decode config: ") {
		t.Errorf("error = %q, want it to start with \"decode config: \"", err)
	}
}

func TestTrailingDataIsAnError(t *testing.T) {
	for _, input := range []string{
		`{"name": "a", "listen": ":1"} {"name": "b", "listen": ":2"}`,
		`{"name": "a", "listen": ":1"} oops`,
	} {
		if err := loadErr(t, input); !errors.Is(err, config.ErrTrailingData) {
			t.Errorf("Load(%s) = %v, want ErrTrailingData", input, err)
		}
	}
	load(t, "{\"name\": \"a\", \"listen\": \":1\"}\n\n") // white space after the object is fine
}

func TestEveryProblemIsReportedAtOnce(t *testing.T) {
	err := loadErr(t, `{"listen": "localhost", "timeout": "2m", "retries": 11, "tags": ["eu", "", "eu"]}`)
	got := fieldErrors(err)
	want := []string{
		"listen: must be host:port",
		"name: is required",
		"retries: must be from 0 to 10",
		`tags: "eu" appears twice`,
		"tags: must not be empty",
		"timeout: must be at most 1m0s",
	}
	if !slices.Equal(got, want) {
		t.Errorf("field errors =\n%q\nwant\n%q", got, want)
	}
	var fe *config.FieldError
	if !errors.As(err, &fe) {
		t.Errorf("errors.As(err, *FieldError) = false, want true")
	}
}

func TestListenPorts(t *testing.T) {
	for _, listen := range []string{":8080", "localhost:1", "0.0.0.0:65535", "[::1]:443"} {
		load(t, `{"name": "a", "listen": "`+listen+`"}`)
	}
	for _, listen := range []string{"localhost", "host:0", "host:http", "host:70000", ""} {
		err := loadErr(t, `{"name": "a", "listen": "`+listen+`"}`)
		if got := fieldErrors(err); len(got) != 1 || !strings.HasPrefix(got[0], "listen: ") {
			t.Errorf("listen %q gave field errors %q, want one listen error", listen, got)
		}
	}
}

func TestSaveWritesIndentedJSONAndOmitsEmptyFields(t *testing.T) {
	var buf bytes.Buffer
	if err := config.Save(&buf, config.Config{Name: "billing", Listen: ":8080"}); err != nil {
		t.Fatalf("Save = %v", err)
	}
	want := "{\n  \"name\": \"billing\",\n  \"listen\": \":8080\"\n}\n"
	if buf.String() != want {
		t.Errorf("Save of a minimal config =\n%s\nwant\n%s(zero timeout, nil retries and no tags are left out)", buf.String(), want)
	}
	buf.Reset()
	retries := 0
	full := config.Config{Name: "billing", Listen: ":8080", Timeout: config.Duration(90 * time.Second), Retries: &retries, Tags: []string{"eu"}}
	if err := config.Save(&buf, full); err != nil {
		t.Fatalf("Save = %v", err)
	}
	want = "{\n  \"name\": \"billing\",\n  \"listen\": \":8080\",\n  \"timeout\": \"1m30s\",\n  \"retries\": 0,\n  \"tags\": [\n    \"eu\"\n  ]\n}\n"
	if buf.String() != want {
		t.Errorf("Save of a full config =\n%s\nwant\n%s", buf.String(), want)
	}
}

func TestSaveThenLoadRoundTrips(t *testing.T) {
	retries := 4
	in := config.Config{Name: "api", Listen: "127.0.0.1:9000", Timeout: config.Duration(250 * time.Millisecond), Retries: &retries, Tags: []string{"a", "b"}}
	var buf bytes.Buffer
	if err := config.Save(&buf, in); err != nil {
		t.Fatalf("Save = %v", err)
	}
	out := load(t, buf.String())
	if out.Name != in.Name || out.Listen != in.Listen || out.Timeout != in.Timeout || *out.Retries != 4 || !slices.Equal(out.Tags, in.Tags) {
		t.Errorf("Load(Save(c)) = %+v, want %+v", out, in)
	}
}
