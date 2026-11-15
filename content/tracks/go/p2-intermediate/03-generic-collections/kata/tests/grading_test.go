package collections_test

import (
	"iter"
	"maps"
	"slices"
	"strings"
	"testing"

	"dojo/collections"
)

// naturals yields 0, 1, 2, ... forever and counts how many values it produced.
func naturals(produced *int) iter.Seq[int] {
	return func(yield func(int) bool) {
		for i := 0; ; i++ {
			*produced++
			if !yield(i) {
				return
			}
		}
	}
}

func TestZeroValueSetIsUsable(t *testing.T) {
	var s collections.Set[int]
	if s.Len() != 0 || s.Has(1) {
		t.Fatalf("an empty Set reports Len() = %d, Has(1) = %v", s.Len(), s.Has(1))
	}
	s.Add(1, 2, 2)
	if s.Len() != 2 || !s.Has(2) {
		t.Errorf("after Add(1, 2, 2): Len() = %d, Has(2) = %v; want 2, true", s.Len(), s.Has(2))
	}
}

func TestAllYieldsEveryItemOnce(t *testing.T) {
	s := collections.NewSet(3, 1, 2, 3)
	got := slices.Sorted(s.All())
	if want := []int{1, 2, 3}; !slices.Equal(got, want) {
		t.Errorf("slices.Sorted(s.All()) = %v, want %v", got, want)
	}
}

func TestAllStopsWhenTheLoopBreaks(t *testing.T) {
	s := collections.NewSet("a", "b", "c")
	seen := 0
	for range s.All() {
		seen++
		break
	}
	if seen != 1 {
		t.Errorf("the loop body ran %d times, want 1", seen)
	}
}

func TestUnionAndIntersectReturnNewSets(t *testing.T) {
	a := collections.NewSet("go", "rust", "java")
	b := collections.NewSet("go", "java", "kotlin")
	union := slices.Sorted(a.Union(b).All())
	if want := []string{"go", "java", "kotlin", "rust"}; !slices.Equal(union, want) {
		t.Errorf("Union = %v, want %v", union, want)
	}
	both := slices.Sorted(a.Intersect(b).All())
	if want := []string{"go", "java"}; !slices.Equal(both, want) {
		t.Errorf("Intersect = %v, want %v", both, want)
	}
	if a.Len() != 3 || b.Len() != 3 {
		t.Errorf("Union and Intersect changed their inputs: Len %d and %d, want 3 and 3", a.Len(), b.Len())
	}
}

func TestMapAndFilterCompose(t *testing.T) {
	words := slices.Values([]string{"go", "gopher", "rust", "gc"})
	long := collections.Filter(words, func(w string) bool { return strings.HasPrefix(w, "g") })
	got := slices.Collect(collections.Map(long, strings.ToUpper))
	if want := []string{"GO", "GOPHER", "GC"}; !slices.Equal(got, want) {
		t.Errorf("Map(Filter(...)) = %v, want %v", got, want)
	}
}

func TestMapAndFilterAreLazy(t *testing.T) {
	produced := 0
	evens := collections.Filter(naturals(&produced), func(n int) bool { return n%2 == 0 })
	squares := collections.Map(evens, func(n int) int { return n * n })
	if produced != 0 {
		t.Fatalf("building the pipeline already produced %d values; iterators must be lazy", produced)
	}
	var got []int
	for v := range squares {
		got = append(got, v)
		if len(got) == 3 {
			break
		}
	}
	if want := []int{0, 4, 16}; !slices.Equal(got, want) {
		t.Errorf("first squares of evens = %v, want %v", got, want)
	}
	if produced != 5 {
		t.Errorf("the source produced %d values, want 5 (0 to 4)", produced)
	}
}

func TestTakeStopsPullingFromTheSource(t *testing.T) {
	produced := 0
	got := slices.Collect(collections.Take(naturals(&produced), 3))
	if want := []int{0, 1, 2}; !slices.Equal(got, want) {
		t.Errorf("Take(naturals, 3) = %v, want %v", got, want)
	}
	if produced != 3 {
		t.Errorf("the source produced %d values, want 3", produced)
	}
}

func TestTakeEdgeCases(t *testing.T) {
	produced := 0
	if got := slices.Collect(collections.Take(naturals(&produced), 0)); len(got) != 0 || produced != 0 {
		t.Errorf("Take(_, 0) = %v and produced %d values, want nothing and 0", got, produced)
	}
	short := slices.Collect(collections.Take(slices.Values([]int{7, 8}), 5))
	if want := []int{7, 8}; !slices.Equal(short, want) {
		t.Errorf("Take([7 8], 5) = %v, want %v", short, want)
	}
	for v := range collections.Take(slices.Values([]int{1, 2, 3}), 2) {
		_ = v
		break // Take must not call yield again after this
	}
}

func TestGroupByKeepsOrderInsideGroups(t *testing.T) {
	words := []string{"go", "rust", "c", "zig", "java", "d"}
	got := collections.GroupBy(words, func(w string) int { return len(w) })
	want := map[int][]string{2: {"go"}, 4: {"rust", "java"}, 1: {"c", "d"}, 3: {"zig"}}
	if !maps.EqualFunc(got, want, slices.Equal) {
		t.Errorf("GroupBy(by length) = %v, want %v", got, want)
	}
}

type cents int64

func TestSumWorksForNamedAndFloatTypes(t *testing.T) {
	if got := collections.Sum(cents(150), cents(250), cents(5)); got != 405 {
		t.Errorf("Sum of cents = %d, want 405", got)
	}
	if got := collections.Sum(0.5, 0.25); got != 0.75 {
		t.Errorf("Sum(0.5, 0.25) = %v, want 0.75", got)
	}
	if got := collections.Sum[int](); got != 0 {
		t.Errorf("Sum() = %d, want 0", got)
	}
}
