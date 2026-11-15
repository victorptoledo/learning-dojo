package collections_test

import (
	"testing"

	"dojo/collections"
)

func TestSetAddAndHas(t *testing.T) {
	s := collections.NewSet("go", "rust")
	s.Add("go", "zig")
	if s.Len() != 3 {
		t.Errorf("Len() = %d, want 3", s.Len())
	}
	if !s.Has("zig") || s.Has("java") {
		t.Errorf("Has(zig) = %v, Has(java) = %v; want true, false", s.Has("zig"), s.Has("java"))
	}
}
