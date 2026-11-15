package pool_test

import (
	"slices"
	"strconv"
	"testing"

	"dojo/pool"
)

func TestMapReturnsResultsInJobOrder(t *testing.T) {
	jobs := []string{"1", "2", "3", "4", "5"}
	got, err := pool.Map(jobs, 2, strconv.Atoi)
	if err != nil {
		t.Fatalf("Map = %v, want nil", err)
	}
	if want := []int{1, 2, 3, 4, 5}; !slices.Equal(got, want) {
		t.Errorf("Map(%q, 2, strconv.Atoi) = %v, want %v", jobs, got, want)
	}
}
