// Package units converts between units of measure. See README.md.
package units

import (
	"fmt"
	"io"
)

// Run runs the command line `args` (without the program name) and returns its exit code.
func Run(args []string, stdout, stderr io.Writer) int {
	fmt.Fprintln(stderr, "todo: write the converter")
	return 1
}
