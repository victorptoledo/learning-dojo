package fetch_test

import (
	"context"
	"slices"
	"strings"
	"testing"
	"time"

	"dojo/fetch"
)

func TestAllReturnsBodiesInURLOrder(t *testing.T) {
	upper := func(ctx context.Context, url string) (string, error) {
		return strings.ToUpper(url), nil
	}
	got, err := fetch.All(t.Context(), []string{"a", "b", "c"}, 2, time.Second, upper)
	if err != nil {
		t.Fatalf("All = %v, want nil", err)
	}
	if want := []string{"A", "B", "C"}; !slices.Equal(got, want) {
		t.Errorf("All = %v, want %v", got, want)
	}
}
