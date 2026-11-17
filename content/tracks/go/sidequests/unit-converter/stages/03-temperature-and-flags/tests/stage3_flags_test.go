package units_test

import "testing"

func TestConvertsTemperatures(t *testing.T) {
	expectLine(t, "100 C = 212 F", "convert", "100", "C", "F")
	expectLine(t, "0 C = 273.15 K", "convert", "0", "C", "K")
	expectLine(t, "98.6 F = 37 C", "convert", "98.6", "F", "C")
	expectLine(t, "300 K = 80.33 F", "convert", "300", "K", "F")
}

func TestNegativeValuesFollowADoubleDash(t *testing.T) {
	expectLine(t, "-40 C = -40 F", "convert", "--", "-40", "C", "F")
}

func TestBelowAbsoluteZeroIsAnError(t *testing.T) {
	expectFailure(t, 1, "error: below absolute zero", "convert", "--", "-300", "C", "K")
	expectFailure(t, 1, "error: below absolute zero", "convert", "--", "-1", "K", "C")
}

func TestTemperatureDoesNotMixWithLength(t *testing.T) {
	expectFailure(t, 1, "error: cannot convert temperature to length", "convert", "1", "C", "m")
}

func TestPrecisionSetsTheDecimals(t *testing.T) {
	expectLine(t, "1 in = 2.54 cm", "convert", "--precision", "2", "1", "in", "cm")
	expectLine(t, "5 km = 3.106856 mi", "convert", "--precision", "6", "5", "km", "mi")
	expectLine(t, "5 km = 3 mi", "convert", "--precision", "0", "5", "km", "mi")
	expectLine(t, "-40 F = -40 C", "convert", "--precision", "1", "--", "-40", "F", "C")
}

func TestABadPrecisionIsAUsageError(t *testing.T) {
	expectFailure(t, 2, "usage: units", "convert", "--precision", "eleven", "1", "m", "cm")
	expectFailure(t, 2, "usage: units", "convert", "--precision", "11", "1", "m", "cm")
	expectFailure(t, 2, "usage: units", "convert", "--colour", "1", "m", "cm")
}

func TestListShowsEveryUnit(t *testing.T) {
	expectLine(t, "length: cm ft in km m mi mm yd\nmass: g kg lb mg oz\ntemperature: C F K", "list")
}
