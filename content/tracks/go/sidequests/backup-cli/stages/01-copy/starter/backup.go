// Package backup copies a folder to a backup folder, incrementally. See README.md.
package backup

import (
	"fmt"
	"io"
)

// Run runs the command line `args` (without the program name) and returns its exit code.
func Run(args []string, stdout, stderr io.Writer) int {
	fmt.Fprintln(stderr, "todo: write the backup")
	return 1
}
