// Command units converts between units of measure.
package main

import (
	"os"

	"dojo/units"
)

func main() {
	os.Exit(units.Run(os.Args[1:], os.Stdout, os.Stderr))
}
