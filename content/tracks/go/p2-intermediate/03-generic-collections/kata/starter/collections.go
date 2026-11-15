// Package collections holds a generic set and lazy helpers over iterators.
package collections

import "iter"

// Number is any type built on an integer or float type, named types included.
// The ~ also accepts named types such as `type Cents int64`.
type Number interface {
	~int | ~int32 | ~int64 | ~float32 | ~float64
}

// Set is a set of comparable values. The zero value is an empty set ready to use.
type Set[T comparable] struct {
	items map[T]struct{}
}

// NewSet returns a set holding items.
func NewSet[T comparable](items ...T) *Set[T] {
	panic("todo: write NewSet")
}

// Add puts items into the set.
func (s *Set[T]) Add(items ...T) {
	panic("todo: write Add")
}

// Has reports whether item is in the set.
func (s *Set[T]) Has(item T) bool {
	panic("todo: write Has")
}

// Len is the number of items.
func (s *Set[T]) Len() int {
	panic("todo: write Len")
}

// All yields every item once, in no particular order.
func (s *Set[T]) All() iter.Seq[T] {
	panic("todo: write All")
}

// Union returns a new set with the items of both sets.
func (s *Set[T]) Union(other *Set[T]) *Set[T] {
	panic("todo: write Union")
}

// Intersect returns a new set with the items present in both sets.
func (s *Set[T]) Intersect(other *Set[T]) *Set[T] {
	panic("todo: write Intersect")
}

// Map yields f(v) for every v of seq, lazily.
func Map[T, U any](seq iter.Seq[T], f func(T) U) iter.Seq[U] {
	panic("todo: write Map")
}

// Filter yields the values of seq for which keep returns true, lazily.
func Filter[T any](seq iter.Seq[T], keep func(T) bool) iter.Seq[T] {
	panic("todo: write Filter")
}

// Take yields at most the first n values of seq and stops pulling from it.
func Take[T any](seq iter.Seq[T], n int) iter.Seq[T] {
	panic("todo: write Take")
}

// GroupBy puts items into groups by key, keeping their order inside each group.
func GroupBy[T any, K comparable](items []T, key func(T) K) map[K][]T {
	panic("todo: write GroupBy")
}

// Sum adds the values; it is 0 for no values.
func Sum[N Number](values ...N) N {
	panic("todo: write Sum")
}
