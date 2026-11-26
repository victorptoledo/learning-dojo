package readings_test

import (
	"testing"
	"testing/fstest"

	"dojo/readings"
)

func TestImportAddsUpADay(t *testing.T) {
	inbox := fstest.MapFS{
		"2026-01-12.csv": {Data: []byte("meter,time,kwh\nM1,2026-01-12 08:00,1.5\nM1,2026-01-12 09:00,0.25\n")},
	}
	report, err := readings.Import(inbox, readings.Options{})
	if err != nil {
		t.Fatalf("Import = %v, want nil", err)
	}
	want := readings.Day{Meter: "M1", Date: "2026-01-12", Energy: 1750, Readings: 2, Hours: 24}
	if len(report.Days) != 1 || report.Days[0] != want {
		t.Errorf("Days = %+v, want [%+v]", report.Days, want)
	}
}
