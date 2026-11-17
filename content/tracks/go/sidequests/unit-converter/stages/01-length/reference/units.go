// Package units converts between units of measure.
package units

import (
	"fmt"
	"io"
	"math"
	"strconv"
)

// A unit belongs to a category and converts to that category's base unit.
type unit struct {
	category string
	toBase   func(float64) float64
	fromBase func(float64) float64
}

func linear(category string, factor float64) unit {
	return unit{
		category: category,
		toBase:   func(v float64) float64 { return v * factor },
		fromBase: func(v float64) float64 { return v / factor },
	}
}

var table = map[string]unit{
	"mm": linear("length", 0.001),
	"cm": linear("length", 0.01),
	"m":  linear("length", 1),
	"km": linear("length", 1000),
	"in": linear("length", 0.0254),
	"ft": linear("length", 0.3048),
	"yd": linear("length", 0.9144),
	"mi": linear("length", 1609.344),
}

const usage = "usage: units convert <value> <from> <to>"

func format(v float64, precision int) string {
	scale := math.Pow(10, float64(precision))
	rounded := math.Round(v*scale) / scale
	if rounded == 0 {
		rounded = 0 // no "-0"
	}
	return strconv.FormatFloat(rounded, 'f', -1, 64)
}

func convert(value float64, from, to string) (float64, error) {
	source, target := table[from], table[to]
	base := source.toBase(value)
	return target.fromBase(base), nil
}

func runConvert(args []string, stdout io.Writer) error {
	precision := 4
	value, err := strconv.ParseFloat(args[0], 64)
	if err != nil {
		return fmt.Errorf("not a number: %q", args[0])
	}
	result, err := convert(value, args[1], args[2])
	if err != nil {
		return err
	}
	fmt.Fprintf(stdout, "%s %s = %s %s\n", format(value, precision), args[1], format(result, precision), args[2])
	return nil
}

// Run runs the command line `args` (without the program name) and returns its exit code.
func Run(args []string, stdout, stderr io.Writer) int {
	var err error
	switch {
	case len(args) > 0 && args[0] == "convert":
		err = runConvert(args[1:], stdout)
	default:
		fmt.Fprintln(stderr, usage)
		return 2
	}
	if err != nil {
		fmt.Fprintf(stderr, "error: %v\n", err)
		return 1
	}
	return 0
}
