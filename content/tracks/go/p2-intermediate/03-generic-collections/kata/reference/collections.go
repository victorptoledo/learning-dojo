// Package collections holds a generic set and lazy helpers over iterators.
package collections

import (
	"iter"
	"maps"
)

// Number is any type built on an integer or float type, named types included.
type Number interface {
	~int | ~int32 | ~int64 | ~float32 | ~float64
}

// Set is a set of comparable values. The zero value is an empty set ready to use.
type Set[T comparable] struct {
	items map[T]struct{}
}

// NewSet returns a set holding items.
func NewSet[T comparable](items ...T) *Set[T] {
	s := &Set[T]{}
	s.Add(items...)
	return s
}

// Add puts items into the set.
func (s *Set[T]) Add(items ...T) {
	if s.items == nil {
		s.items = make(map[T]struct{}, len(items))
	}
	for _, item := range items {
		s.items[item] = struct{}{}
	}
}

// Has reports whether item is in the set.
func (s *Set[T]) Has(item T) bool {
	_, ok := s.items[item]
	return ok
}

// Len is the number of items.
func (s *Set[T]) Len() int { return len(s.items) }

// All yields every item once, in no particular order.
func (s *Set[T]) All() iter.Seq[T] {
	return maps.Keys(s.items)
}

// Union returns a new set with the items of both sets.
func (s *Set[T]) Union(other *Set[T]) *Set[T] {
	out := NewSet[T]()
	for item := range s.All() {
		out.Add(item)
	}
	for item := range other.All() {
		out.Add(item)
	}
	return out
}

// Intersect returns a new set with the items present in both sets.
func (s *Set[T]) Intersect(other *Set[T]) *Set[T] {
	out := NewSet[T]()
	for item := range s.All() {
		if other.Has(item) {
			out.Add(item)
		}
	}
	return out
}

// Map yields f(v) for every v of seq, lazily.
func Map[T, U any](seq iter.Seq[T], f func(T) U) iter.Seq[U] {
	return func(yield func(U) bool) {
		for v := range seq {
			if !yield(f(v)) {
				return
			}
		}
	}
}

// Filter yields the values of seq for which keep returns true, lazily.
func Filter[T any](seq iter.Seq[T], keep func(T) bool) iter.Seq[T] {
	return func(yield func(T) bool) {
		for v := range seq {
			if keep(v) && !yield(v) {
				return
			}
		}
	}
}

// Take yields at most the first n values of seq and stops pulling from it.
func Take[T any](seq iter.Seq[T], n int) iter.Seq[T] {
	return func(yield func(T) bool) {
		if n <= 0 {
			return
		}
		taken := 0
		for v := range seq {
			if !yield(v) {
				return
			}
			taken++
			if taken == n {
				return
			}
		}
	}
}

// GroupBy puts items into groups by key, keeping their order inside each group.
func GroupBy[T any, K comparable](items []T, key func(T) K) map[K][]T {
	groups := make(map[K][]T)
	for _, item := range items {
		k := key(item)
		groups[k] = append(groups[k], item)
	}
	return groups
}

// Sum adds the values; it is 0 for no values.
func Sum[N Number](values ...N) N {
	var total N
	for _, v := range values {
		total += v
	}
	return total
}
