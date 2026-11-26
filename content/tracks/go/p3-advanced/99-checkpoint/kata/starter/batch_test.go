package batch_test

import (
	"context"
	"slices"
	"testing"

	"dojo/batch"
)

func TestFlushesFullBatches(t *testing.T) {
	var got [][]int
	b := batch.Start(context.Background(), batch.Config{Size: 2}, func(_ context.Context, items []int) error {
		got = append(got, items)
		return nil
	})
	for i := 1; i <= 4; i++ {
		if err := b.Add(context.Background(), i); err != nil {
			t.Fatalf("Add(%d) = %v, want nil", i, err)
		}
	}
	if err := b.Close(); err != nil {
		t.Fatalf("Close = %v, want nil", err)
	}
	want := [][]int{{1, 2}, {3, 4}}
	if !slices.EqualFunc(got, want, slices.Equal) {
		t.Errorf("batches = %v, want %v", got, want)
	}
}
