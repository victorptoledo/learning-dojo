package units_test

import (
	"bytes"
	"strings"
	"testing"

	"dojo/units"
)

// run calls units.Run and returns the exit code, stdout and stderr (trimmed).
func run(args ...string) (int, string, string) {
	var stdout, stderr bytes.Buffer
	code := units.Run(args, &stdout, &stderr)
	return code, strings.TrimSpace(stdout.String()), strings.TrimSpace(stderr.String())
}

// expectLine checks a successful run that prints exactly `want`.
func expectLine(t *testing.T, want string, args ...string) {
	t.Helper()
	code, out, errOut := run(args...)
	if code != 0 || out != want {
		t.Errorf("units %s\n got: code %d, stdout %q, stderr %q\nwant: code 0, stdout %q",
			strings.Join(args, " "), code, out, errOut, want)
	}
}

// expectFailure checks a run that exits with `code`, prints nothing on stdout and writes a
// stderr starting with `prefix`.
func expectFailure(t *testing.T, code int, prefix string, args ...string) {
	t.Helper()
	got, out, errOut := run(args...)
	if got != code || out != "" || !strings.HasPrefix(errOut, prefix) {
		t.Errorf("units %s\n got: code %d, stdout %q, stderr %q\nwant: code %d, no stdout, stderr starting with %q",
			strings.Join(args, " "), got, out, errOut, code, prefix)
	}
}
