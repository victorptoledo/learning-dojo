// Package readings imports electricity meter readings from CSV files and writes a daily
// report as JSON.
package readings

import (
	"errors"
	"fmt"
	"io/fs"
	"time"
)

// Errors a RowError can wrap.
var (
	ErrMissingColumn = errors.New("missing column")
	ErrBadTime       = errors.New("bad time")
	ErrBadEnergy     = errors.New("bad energy")
)

// LocalLayout is the layout of a local reading time, read in Options.Zone.
const LocalLayout = "2006-01-02 15:04"

// Energy is an amount of energy in watt-hours, written as kilowatt-hours with three decimals.
type Energy int64

// ParseEnergy reads kilowatt-hours with at most three decimals ("12", "12.5", "0.001").
func ParseEnergy(s string) (Energy, error) {
	panic("todo: write ParseEnergy")
}

// String writes kilowatt-hours with three decimals: 12345 Wh is "12.345".
func (e Energy) String() string {
	panic("todo: write String")
}

// MarshalText makes Energy a JSON string such as "12.345".
func (e Energy) MarshalText() ([]byte, error) {
	panic("todo: write MarshalText")
}

// Options configure Import.
type Options struct {
	// Zone is where local reading times are read and where days begin and end.
	// nil means UTC.
	Zone *time.Location
}

// Day is the energy one meter used on one local calendar day. Add the struct tags.
type Day struct {
	Meter    string
	Date     string // in the zone, as 2006-01-02
	Energy   Energy
	Readings int
	Hours    int // how long that local day is: 23, 24 or 25
}

// Report is the result of an import. Add the struct tags.
type Report struct {
	Days       []Day
	Duplicates int
}

// RowError is a problem in one line of one file.
type RowError struct {
	File string
	Line int
	Err  error
}

func (e *RowError) Error() string { return fmt.Sprintf("%s:%d: %v", e.File, e.Line, e.Err) }
func (e *RowError) Unwrap() error { return e.Err }

// Import reads every .csv file at the root of fsys, in name order, and adds up the
// readings per meter and local day. Bad rows and bad files do not stop the import: they
// come back joined in the error, next to the report of everything that was good.
func Import(fsys fs.FS, opts Options) (Report, error) {
	panic("todo: write Import")
}

// WriteReport writes r to dir/report.json atomically: readers see the old file or the new
// one, never half of it, and no temporary file is left behind.
func WriteReport(dir string, r Report) error {
	panic("todo: write WriteReport")
}
