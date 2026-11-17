package units_test

import "testing"

func TestConvertsKilometresToMetres(t *testing.T) {
	expectLine(t, "10 km = 10000 m", "convert", "10", "km", "m")
}

func TestConvertsBetweenImperialUnits(t *testing.T) {
	expectLine(t, "1 mi = 5280 ft", "convert", "1", "mi", "ft")
	expectLine(t, "3 ft = 1 yd", "convert", "3", "ft", "yd")
	expectLine(t, "12 in = 1 ft", "convert", "12", "in", "ft")
}

func TestConvertsBetweenMetricAndImperial(t *testing.T) {
	expectLine(t, "1 in = 2.54 cm", "convert", "1", "in", "cm")
	expectLine(t, "1 ft = 0.3048 m", "convert", "1", "ft", "m")
	expectLine(t, "5 km = 3.1069 mi", "convert", "5", "km", "mi")
}

func TestSmallResultsKeepFourDecimals(t *testing.T) {
	expectLine(t, "1 mm = 0.001 m", "convert", "1", "mm", "m")
	expectLine(t, "1 in = 0.0254 m", "convert", "1", "in", "m")
	expectLine(t, "1 mm = 0 km", "convert", "1", "mm", "km")
}

func TestDecimalValuesAreReadAndPrinted(t *testing.T) {
	expectLine(t, "2.5 m = 250 cm", "convert", "2.5", "m", "cm")
	expectLine(t, "0.1234 km = 123.4 m", "convert", "0.1234", "km", "m")
	expectLine(t, "1.2346 m = 1.2346 m", "convert", "1.23456", "m", "m")
}

func TestTheSameUnitIsTheSameValue(t *testing.T) {
	expectLine(t, "42 yd = 42 yd", "convert", "42", "yd", "yd")
}
