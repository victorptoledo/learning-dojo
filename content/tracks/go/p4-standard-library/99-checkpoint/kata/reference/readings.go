// Package readings imports electricity meter readings from CSV files and writes a daily
// report as JSON.
package readings

import (
	"cmp"
	"encoding/csv"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"io/fs"
	"os"
	"path"
	"path/filepath"
	"slices"
	"strconv"
	"strings"
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
	whole, frac, hasDot := strings.Cut(s, ".")
	if whole == "" || !allDigits(whole) || (hasDot && (frac == "" || len(frac) > 3 || !allDigits(frac))) {
		return 0, fmt.Errorf("%w: %q", ErrBadEnergy, s)
	}
	kwh, err := strconv.ParseInt(whole, 10, 64)
	if err != nil || kwh > (1<<62)/1000 {
		return 0, fmt.Errorf("%w: %q", ErrBadEnergy, s)
	}
	frac += strings.Repeat("0", 3-len(frac))
	wh, _ := strconv.ParseInt(frac, 10, 64)
	return Energy(kwh*1000 + wh), nil
}

func allDigits(s string) bool {
	for _, r := range s {
		if r < '0' || r > '9' {
			return false
		}
	}
	return true
}

// String writes kilowatt-hours with three decimals: 12345 Wh is "12.345".
func (e Energy) String() string {
	return fmt.Sprintf("%d.%03d", e/1000, e%1000)
}

// MarshalText makes Energy a JSON string such as "12.345".
func (e Energy) MarshalText() ([]byte, error) {
	return []byte(e.String()), nil
}

// Options configure Import.
type Options struct {
	// Zone is where local reading times are read and where days begin and end.
	// nil means UTC.
	Zone *time.Location
}

// Day is the energy one meter used on one local calendar day.
type Day struct {
	Meter    string `json:"meter"`
	Date     string `json:"date"` // in the zone, as 2006-01-02
	Energy   Energy `json:"kwh"`
	Readings int    `json:"readings"`
	Hours    int    `json:"hours"` // how long that local day is: 23, 24 or 25
}

// Report is the result of an import.
type Report struct {
	Days       []Day `json:"days"`
	Duplicates int   `json:"duplicates"`
}

// RowError is a problem in one line of one file.
type RowError struct {
	File string
	Line int
	Err  error
}

func (e *RowError) Error() string { return fmt.Sprintf("%s:%d: %v", e.File, e.Line, e.Err) }
func (e *RowError) Unwrap() error { return e.Err }

type key struct {
	meter string
	at    time.Time
}

type dayKey struct {
	meter, date string
}

// Import reads every .csv file at the root of fsys, in name order, and adds up the
// readings per meter and local day. Bad rows and bad files do not stop the import: they
// come back joined in the error, next to the report of everything that was good.
func Import(fsys fs.FS, opts Options) (Report, error) {
	zone := opts.Zone
	if zone == nil {
		zone = time.UTC
	}
	entries, err := fs.ReadDir(fsys, ".")
	if err != nil {
		return Report{Days: []Day{}}, fmt.Errorf("read inbox: %w", err)
	}
	imp := importer{zone: zone, seen: map[key]bool{}, days: map[dayKey]*Day{}}
	for _, entry := range entries { // ReadDir sorts by name
		if entry.IsDir() || !strings.EqualFold(path.Ext(entry.Name()), ".csv") {
			continue
		}
		if err := imp.file(fsys, entry.Name()); err != nil {
			imp.problems = append(imp.problems, err)
		}
	}
	report := Report{Days: make([]Day, 0, len(imp.days)), Duplicates: imp.duplicates}
	for _, day := range imp.days {
		report.Days = append(report.Days, *day)
	}
	slices.SortFunc(report.Days, func(a, b Day) int {
		return cmp.Or(cmp.Compare(a.Meter, b.Meter), cmp.Compare(a.Date, b.Date))
	})
	return report, errors.Join(imp.problems...)
}

type importer struct {
	zone       *time.Location
	seen       map[key]bool
	days       map[dayKey]*Day
	duplicates int
	problems   []error
}

// file imports one file. A returned error stops that file; row errors are kept.
func (imp *importer) file(fsys fs.FS, name string) error {
	f, err := fsys.Open(name)
	if err != nil {
		return fmt.Errorf("open %s: %w", name, err)
	}
	defer f.Close()

	r := csv.NewReader(f)
	r.TrimLeadingSpace = true
	header, err := r.Read()
	if errors.Is(err, io.EOF) {
		return nil // an empty file has nothing to import
	}
	if err != nil {
		return &RowError{File: name, Line: 1, Err: err}
	}
	cols := map[string]int{}
	for i, h := range header {
		if i == 0 {
			h = strings.TrimPrefix(h, "\uFEFF") // a byte order mark from a spreadsheet
		}
		cols[strings.ToLower(strings.TrimSpace(h))] = i
	}
	for _, want := range []string{"meter", "time", "kwh"} {
		if _, ok := cols[want]; !ok {
			return &RowError{File: name, Line: 1, Err: fmt.Errorf("%w %q", ErrMissingColumn, want)}
		}
	}
	for {
		record, err := r.Read()
		if errors.Is(err, io.EOF) {
			return nil
		}
		if err != nil {
			line := 0
			if pe, ok := errors.AsType[*csv.ParseError](err); ok {
				line = pe.StartLine
			}
			imp.problems = append(imp.problems, &RowError{File: name, Line: line, Err: err})
			if errors.Is(err, csv.ErrFieldCount) {
				continue // the reader can go on after a row with the wrong number of fields
			}
			return nil
		}
		line, _ := r.FieldPos(0)
		if err := imp.row(record, cols); err != nil {
			imp.problems = append(imp.problems, &RowError{File: name, Line: line, Err: err})
		}
	}
}

func (imp *importer) row(record []string, cols map[string]int) error {
	meter := strings.TrimSpace(record[cols["meter"]])
	if meter == "" {
		return errors.New("empty meter")
	}
	at, err := imp.parseTime(strings.TrimSpace(record[cols["time"]]))
	if err != nil {
		return err
	}
	energy, err := ParseEnergy(strings.TrimSpace(record[cols["kwh"]]))
	if err != nil {
		return err
	}
	k := key{meter: meter, at: at}
	if imp.seen[k] {
		imp.duplicates++
		return nil
	}
	imp.seen[k] = true

	local := at.In(imp.zone)
	dk := dayKey{meter: meter, date: local.Format(time.DateOnly)}
	day, ok := imp.days[dk]
	if !ok {
		y, m, d := local.Date()
		start := time.Date(y, m, d, 0, 0, 0, 0, imp.zone)
		end := time.Date(y, m, d+1, 0, 0, 0, 0, imp.zone)
		day = &Day{Meter: meter, Date: dk.date, Hours: int(end.Sub(start) / time.Hour)}
		imp.days[dk] = day
	}
	day.Energy += energy
	day.Readings++
	return nil
}

// parseTime reads an RFC 3339 instant, or a local time in the zone. A local time that
// does not exist there (skipped by a daylight saving change) is an error.
func (imp *importer) parseTime(s string) (time.Time, error) {
	if strings.Contains(s, "T") {
		t, err := time.Parse(time.RFC3339, s)
		if err != nil {
			return time.Time{}, fmt.Errorf("%w: %v", ErrBadTime, err)
		}
		return t.UTC(), nil
	}
	t, err := time.ParseInLocation(LocalLayout, s, imp.zone)
	if err != nil {
		return time.Time{}, fmt.Errorf("%w: %v", ErrBadTime, err)
	}
	if t.Format(LocalLayout) != s {
		return time.Time{}, fmt.Errorf("%w: %s does not exist in %s", ErrBadTime, s, imp.zone)
	}
	return t.UTC(), nil
}

// WriteReport writes r to dir/report.json atomically: readers see the old file or the new
// one, never half of it, and no temporary file is left behind.
func WriteReport(dir string, r Report) (err error) {
	if r.Days == nil {
		r.Days = []Day{}
	}
	tmp, err := os.CreateTemp(dir, ".report-*.json")
	if err != nil {
		return fmt.Errorf("write report: %w", err)
	}
	defer func() {
		if err != nil {
			tmp.Close()
			os.Remove(tmp.Name())
		}
	}()
	enc := json.NewEncoder(tmp)
	enc.SetIndent("", "  ")
	if err := enc.Encode(r); err != nil {
		return fmt.Errorf("write report: %w", err)
	}
	if err := tmp.Sync(); err != nil {
		return fmt.Errorf("write report: %w", err)
	}
	if err := tmp.Close(); err != nil {
		return fmt.Errorf("write report: %w", err)
	}
	if err := os.Rename(tmp.Name(), filepath.Join(dir, "report.json")); err != nil {
		return fmt.Errorf("write report: %w", err)
	}
	return nil
}
