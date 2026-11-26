package readings_test

import (
	"errors"
	"io/fs"
	"os"
	"path/filepath"
	"slices"
	"strings"
	"testing"
	"testing/fstest"
	"time"

	"dojo/readings"
)

func lisbon(t *testing.T) *time.Location {
	t.Helper()
	loc, err := time.LoadLocation("Europe/Lisbon")
	if err != nil {
		t.Fatalf("LoadLocation: %v", err)
	}
	return loc
}

func file(text string) *fstest.MapFile { return &fstest.MapFile{Data: []byte(text)} }

func rowErrors(err error) []string {
	var out []string
	var visit func(error)
	visit = func(err error) {
		if re, ok := err.(*readings.RowError); ok {
			out = append(out, re.Error())
			return
		}
		if joined, ok := err.(interface{ Unwrap() []error }); ok {
			for _, e := range joined.Unwrap() {
				visit(e)
			}
		}
	}
	visit(err)
	return out
}

func TestParseEnergyAndString(t *testing.T) {
	for in, want := range map[string]readings.Energy{"12": 12000, "12.3": 12300, "12.345": 12345, "0.001": 1, "0": 0, "007.5": 7500} {
		got, err := readings.ParseEnergy(in)
		if err != nil || got != want {
			t.Errorf("ParseEnergy(%q) = %d, %v, want %d", in, got, err, want)
		}
	}
	for _, in := range []string{"", "12.3456", "-1", "1,5", "1.", ".5", "1e3", "kwh", " 1", "99999999999999999999"} {
		if _, err := readings.ParseEnergy(in); !errors.Is(err, readings.ErrBadEnergy) {
			t.Errorf("ParseEnergy(%q) = %v, want ErrBadEnergy", in, err)
		}
	}
	for e, want := range map[readings.Energy]string{12345: "12.345", 5: "0.005", 1000: "1.000", 0: "0.000"} {
		if got := e.String(); got != want {
			t.Errorf("Energy(%d).String() = %q, want %q", int64(e), got, want)
		}
	}
}

func TestHeadersInAnyOrderWithABOM(t *testing.T) {
	inbox := fstest.MapFS{
		"a.csv": file("\uFEFFKWh, Meter ,TIME\n2,M1,2026-01-12 08:00\n"),
		"b.csv": file("time,note,meter,kwh\n2026-01-12 09:00,\"night, cold\",M1,0.5\n"),
	}
	report, err := readings.Import(inbox, readings.Options{})
	if err != nil {
		t.Fatalf("Import = %v, want nil", err)
	}
	want := []readings.Day{{Meter: "M1", Date: "2026-01-12", Energy: 2500, Readings: 2, Hours: 24}}
	if !slices.Equal(report.Days, want) {
		t.Errorf("Days = %+v, want %+v: find columns by name (any case, trimmed, BOM removed), ignore extra ones", report.Days, want)
	}
}

func TestOnlyCSVFilesAtTheRootInNameOrder(t *testing.T) {
	inbox := fstest.MapFS{
		"2.csv":         file("meter,time,kwh\nM1,2026-01-12 08:00,9\n"),
		"1.CSV":         file("meter,time,kwh\nM1,2026-01-12 08:00,1\n"),
		"notes.txt":     file("meter,time,kwh\nM1,2026-01-13 08:00,5\n"),
		"old/0.csv":     file("meter,time,kwh\nM1,2026-01-14 08:00,5\n"),
		"empty.csv":     file(""),
		"z-readme.json": file("{}"),
	}
	report, err := readings.Import(inbox, readings.Options{})
	if err != nil {
		t.Fatalf("Import = %v, want nil (an empty .csv has nothing to import)", err)
	}
	want := []readings.Day{{Meter: "M1", Date: "2026-01-12", Energy: 1000, Readings: 1, Hours: 24}}
	if !slices.Equal(report.Days, want) || report.Duplicates != 1 {
		t.Errorf("report = %+v, want days %+v and 1 duplicate: 1.CSV comes before 2.csv and wins, and only .csv files at the root count", report, want)
	}
}

func TestBadRowsAreReportedAndSkipped(t *testing.T) {
	inbox := fstest.MapFS{
		"day.csv": file(strings.Join([]string{
			"meter,time,kwh",
			"M1,2026-01-12 08:00,1",
			"M1,2026-01-12 09:00,lots",
			"M1,12/01/2026 10:00,1",
			"M1,2026-01-12 11:00",
			",2026-01-12 12:00,1",
			"M1,2026-01-12 13:00,2",
		}, "\n") + "\n"),
	}
	report, err := readings.Import(inbox, readings.Options{})
	want := []readings.Day{{Meter: "M1", Date: "2026-01-12", Energy: 3000, Readings: 2, Hours: 24}}
	if !slices.Equal(report.Days, want) {
		t.Errorf("Days = %+v, want %+v: a bad row must not stop the file", report.Days, want)
	}
	got := rowErrors(err)
	if len(got) != 4 {
		t.Fatalf("row errors = %q, want 4 (lines 3, 4, 5 and 6)", got)
	}
	for i, prefix := range []string{"day.csv:3: ", "day.csv:4: ", "day.csv:5: ", "day.csv:6: "} {
		if !strings.HasPrefix(got[i], prefix) {
			t.Errorf("row error %d = %q, want it to start with %q", i, got[i], prefix)
		}
	}
	if !errors.Is(err, readings.ErrBadEnergy) || !errors.Is(err, readings.ErrBadTime) {
		t.Errorf("error = %v, want it to wrap ErrBadEnergy and ErrBadTime", err)
	}
}

func TestAFileWithoutAColumnIsSkipped(t *testing.T) {
	inbox := fstest.MapFS{
		"a.csv": file("meter,when,kwh\nM1,2026-01-12 08:00,1\n"),
		"b.csv": file("meter,time,kwh\nM2,2026-01-12 08:00,1\n"),
	}
	report, err := readings.Import(inbox, readings.Options{})
	if !errors.Is(err, readings.ErrMissingColumn) {
		t.Fatalf("Import = %v, want it to wrap ErrMissingColumn", err)
	}
	if got := rowErrors(err); len(got) != 1 || !strings.HasPrefix(got[0], "a.csv:1: ") || !strings.Contains(got[0], `"time"`) {
		t.Errorf("row errors = %q, want one for a.csv line 1 naming the column \"time\"", got)
	}
	if len(report.Days) != 1 || report.Days[0].Meter != "M2" {
		t.Errorf("Days = %+v, want only M2: the other files are still imported", report.Days)
	}
}

func TestDaysAreLocalCalendarDays(t *testing.T) {
	inbox := fstest.MapFS{
		"r.csv": file(strings.Join([]string{
			"meter,time,kwh",
			"M1,2026-07-01T22:30:00Z,1", // 23:30 in Lisbon (UTC+1 in summer): 1 July
			"M1,2026-07-01T23:30:00Z,2", // 00:30 on 2 July in Lisbon
			"M1,2026-07-02T00:15:00+01:00,4",
			"M1,2026-07-02 00:45,8",     // local time in the zone
			"M1,2026-07-01T23:15:00Z,4", // the same instant as 00:15+01:00, written in UTC
			"M1,2026-07-02 00:15,4",     // and again, as local time
		}, "\n") + "\n"),
	}
	report, err := readings.Import(inbox, readings.Options{Zone: lisbon(t)})
	if err != nil {
		t.Fatalf("Import = %v, want nil", err)
	}
	want := []readings.Day{
		{Meter: "M1", Date: "2026-07-01", Energy: 1000, Readings: 1, Hours: 24},
		{Meter: "M1", Date: "2026-07-02", Energy: 14000, Readings: 3, Hours: 24},
	}
	if !slices.Equal(report.Days, want) {
		t.Errorf("Days = %+v, want %+v: group by the date in the zone, not in UTC", report.Days, want)
	}
	if report.Duplicates != 2 {
		t.Errorf("Duplicates = %d, want 2: one instant written three ways is one reading (compare instants, not their locations)", report.Duplicates)
	}
}

func TestDaylightSavingDays(t *testing.T) {
	inbox := fstest.MapFS{
		"r.csv": file(strings.Join([]string{
			"meter,time,kwh",
			"M1,2026-03-29 00:30,1",
			"M1,2026-03-29 01:30,1", // clocks jump from 01:00 to 02:00: this time never happens
			"M1,2026-03-29 02:30,1",
			"M1,2026-10-25T00:30:00Z,1", // 01:30 local, first time (UTC+1)
			"M1,2026-10-25T01:30:00Z,1", // 01:30 local again (UTC+0): a different instant
			"M1,2026-10-26 12:00,1",
		}, "\n") + "\n"),
	}
	report, err := readings.Import(inbox, readings.Options{Zone: lisbon(t)})
	if got := rowErrors(err); len(got) != 1 || !strings.HasPrefix(got[0], "r.csv:3: ") || !errors.Is(err, readings.ErrBadTime) {
		t.Errorf("row errors = %q, want one ErrBadTime on line 3: 01:30 does not exist in Lisbon on 2026-03-29", got)
	}
	want := []readings.Day{
		{Meter: "M1", Date: "2026-03-29", Energy: 2000, Readings: 2, Hours: 23},
		{Meter: "M1", Date: "2026-10-25", Energy: 2000, Readings: 2, Hours: 25},
		{Meter: "M1", Date: "2026-10-26", Energy: 1000, Readings: 1, Hours: 24},
	}
	if !slices.Equal(report.Days, want) {
		t.Errorf("Days = %+v, want %+v", report.Days, want)
	}
	if report.Duplicates != 0 {
		t.Errorf("Duplicates = %d, want 0: the two 01:30 readings of 25 October are an hour apart", report.Duplicates)
	}
}

func TestDuplicatesAcrossFilesAndMetersSorted(t *testing.T) {
	inbox := fstest.MapFS{
		"1.csv": file("meter,time,kwh\nM2,2026-01-12 08:00,1\nM1,2026-01-13 08:00,1\n"),
		"2.csv": file("meter,time,kwh\nM2,2026-01-12T08:00:00Z,7\nM1,2026-01-12 08:00,1\nM2,2026-01-12 08:00,7\n"),
	}
	report, err := readings.Import(inbox, readings.Options{})
	if err != nil {
		t.Fatalf("Import = %v, want nil", err)
	}
	want := []readings.Day{
		{Meter: "M1", Date: "2026-01-12", Energy: 1000, Readings: 1, Hours: 24},
		{Meter: "M1", Date: "2026-01-13", Energy: 1000, Readings: 1, Hours: 24},
		{Meter: "M2", Date: "2026-01-12", Energy: 1000, Readings: 1, Hours: 24},
	}
	if !slices.Equal(report.Days, want) {
		t.Errorf("Days = %+v, want %+v (sorted by meter, then date; the first reading of an instant wins)", report.Days, want)
	}
	if report.Duplicates != 2 {
		t.Errorf("Duplicates = %d, want 2: the same meter at the same instant, however it is written", report.Duplicates)
	}
}

func TestWriteReportIsAtomicAndExact(t *testing.T) {
	dir := t.TempDir()
	if err := os.WriteFile(filepath.Join(dir, "report.json"), []byte("old"), 0o644); err != nil {
		t.Fatal(err)
	}
	r := readings.Report{Days: []readings.Day{{Meter: "M1", Date: "2026-01-12", Energy: 1750, Readings: 2, Hours: 24}}, Duplicates: 1}
	if err := readings.WriteReport(dir, r); err != nil {
		t.Fatalf("WriteReport = %v, want nil", err)
	}
	got, err := os.ReadFile(filepath.Join(dir, "report.json"))
	if err != nil {
		t.Fatal(err)
	}
	want := `{
  "days": [
    {
      "meter": "M1",
      "date": "2026-01-12",
      "kwh": "1.750",
      "readings": 2,
      "hours": 24
    }
  ],
  "duplicates": 1
}
`
	if string(got) != want {
		t.Errorf("report.json =\n%s\nwant\n%s", got, want)
	}
	entries, _ := os.ReadDir(dir)
	if len(entries) != 1 {
		var names []string
		for _, e := range entries {
			names = append(names, e.Name())
		}
		t.Errorf("files in the folder = %q, want only report.json: the temporary file must be renamed, not left behind", names)
	}
}

func TestWriteReportOfNothing(t *testing.T) {
	dir := t.TempDir()
	if err := readings.WriteReport(dir, readings.Report{}); err != nil {
		t.Fatalf("WriteReport = %v, want nil", err)
	}
	got, _ := os.ReadFile(filepath.Join(dir, "report.json"))
	if want := "{\n  \"days\": [],\n  \"duplicates\": 0\n}\n"; string(got) != want {
		t.Errorf("report.json of an empty report =\n%s\nwant\n%s(a nil slice must still be written as [])", got, want)
	}
	report, err := readings.Import(fstest.MapFS{}, readings.Options{})
	if err != nil || report.Days == nil {
		t.Errorf("Import of an empty inbox = %+v, %v, want an empty, non-nil Days and no error", report, err)
	}
}

func TestWriteReportIntoAMissingFolder(t *testing.T) {
	missing := filepath.Join(t.TempDir(), "nope")
	err := readings.WriteReport(missing, readings.Report{})
	if !errors.Is(err, fs.ErrNotExist) {
		t.Errorf("WriteReport into a missing folder = %v, want it to wrap fs.ErrNotExist", err)
	}
}
