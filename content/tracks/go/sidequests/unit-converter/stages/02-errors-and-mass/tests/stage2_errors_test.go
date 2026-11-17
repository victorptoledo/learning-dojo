package units_test

import "testing"

func TestConvertsMass(t *testing.T) {
	expectLine(t, "2 kg = 2000 g", "convert", "2", "kg", "g")
	expectLine(t, "1 lb = 16 oz", "convert", "1", "lb", "oz")
	expectLine(t, "1 kg = 2.2046 lb", "convert", "1", "kg", "lb")
	expectLine(t, "500 mg = 0.5 g", "convert", "500", "mg", "g")
}

func TestBadCommandLinesAreUsageErrors(t *testing.T) {
	expectFailure(t, 2, "usage: units")
	expectFailure(t, 2, "usage: units", "teleport", "1", "m", "km")
	expectFailure(t, 2, "usage: units", "convert", "1", "m")
	expectFailure(t, 2, "usage: units", "convert", "1", "m", "km", "extra")
}

func TestAnUnknownUnitIsNamed(t *testing.T) {
	expectFailure(t, 1, `error: unknown unit "parsec"`, "convert", "1", "parsec", "m")
	expectFailure(t, 1, `error: unknown unit "furlong"`, "convert", "1", "m", "furlong")
	expectFailure(t, 1, `error: unknown unit "KM"`, "convert", "1", "KM", "m")
}

func TestAValueThatIsNotANumber(t *testing.T) {
	expectFailure(t, 1, `error: not a number: "ten"`, "convert", "ten", "m", "km")
}

func TestCategoriesDoNotMix(t *testing.T) {
	expectFailure(t, 1, "error: cannot convert length to mass", "convert", "1", "km", "kg")
	expectFailure(t, 1, "error: cannot convert mass to length", "convert", "1", "lb", "ft")
}
