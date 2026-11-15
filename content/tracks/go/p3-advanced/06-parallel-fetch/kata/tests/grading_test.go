package fetch_test

// These tests run inside testing/synctest bubbles: sleeps and timeouts use a fake clock, and
// a test fails if goroutines are still blocked when it ends.

import (
	"context"
	"errors"
	"slices"
	"strings"
	"sync"
	"sync/atomic"
	"testing"
	"testing/synctest"
	"time"

	"dojo/fetch"
)

// service is a fake slow service. Each URL names how long it takes ("slow:300ms"), or fails
// ("fail:100ms"). It records how many calls run at once and how many were cancelled.
type service struct {
	active, peak, calls, cancelled atomic.Int64
	mu                             sync.Mutex
	finished                       []string
}

var errDown = errors.New("service down")

func (s *service) fetch(ctx context.Context, url string) (string, error) {
	s.calls.Add(1)
	now := s.active.Add(1)
	defer s.active.Add(-1)
	for {
		peak := s.peak.Load()
		if now <= peak || s.peak.CompareAndSwap(peak, now) {
			break
		}
	}
	kind, delay, _ := strings.Cut(url, ":")
	d, err := time.ParseDuration(delay)
	if err != nil {
		d = 10 * time.Millisecond
	}
	timer := time.NewTimer(d)
	defer timer.Stop()
	select {
	case <-timer.C:
	case <-ctx.Done():
		s.cancelled.Add(1)
		return "", ctx.Err()
	}
	s.mu.Lock()
	s.finished = append(s.finished, url)
	s.mu.Unlock()
	if kind == "fail" {
		return "", errDown
	}
	return "body of " + url, nil
}

func urls(n int, delay string) []string {
	out := make([]string, n)
	for i := range out {
		out[i] = "ok" + string(rune('a'+i)) + ":" + delay
	}
	return out
}

func TestAllRunsAtMostLimitCallsAtOnceButInParallel(t *testing.T) {
	synctest.Test(t, func(t *testing.T) {
		var s service
		start := time.Now()
		bodies, err := fetch.All(t.Context(), urls(9, "100ms"), 3, time.Second, s.fetch)
		if err != nil {
			t.Fatalf("All = %v", err)
		}
		if len(bodies) != 9 || bodies[0] != "body of oka:100ms" || bodies[8] != "body of oki:100ms" {
			t.Errorf("All bodies = %q, want 9 bodies in URL order", bodies)
		}
		if peak := s.peak.Load(); peak != 3 {
			t.Errorf("%d calls ran at once, want exactly 3 (the limit)", peak)
		}
		if elapsed := time.Since(start); elapsed != 300*time.Millisecond {
			t.Errorf("9 calls of 100ms with a limit of 3 took %v, want 300ms", elapsed)
		}
	})
}

func TestAllKeepsURLOrderWhenLaterCallsFinishFirst(t *testing.T) {
	synctest.Test(t, func(t *testing.T) {
		var s service
		list := []string{"ok:300ms", "ok:100ms", "ok:200ms"}
		bodies, err := fetch.All(t.Context(), list, 3, time.Second, s.fetch)
		if err != nil {
			t.Fatalf("All = %v", err)
		}
		want := []string{"body of ok:300ms", "body of ok:100ms", "body of ok:200ms"}
		if !slices.Equal(bodies, want) {
			t.Errorf("All = %q, want %q", bodies, want)
		}
	})
}

func TestAllWithALimitBelowOneRunsOneAtATime(t *testing.T) {
	synctest.Test(t, func(t *testing.T) {
		var s service
		if _, err := fetch.All(t.Context(), urls(3, "10ms"), 0, time.Second, s.fetch); err != nil {
			t.Fatalf("All = %v", err)
		}
		if peak := s.peak.Load(); peak != 1 {
			t.Errorf("limit 0 ran %d calls at once, want 1", peak)
		}
	})
}

func TestAllGivesEachCallItsOwnTimeout(t *testing.T) {
	synctest.Test(t, func(t *testing.T) {
		var s service
		start := time.Now()
		_, err := fetch.All(t.Context(), []string{"ok:50ms", "ok:5s", "ok:80ms"}, 3, 200*time.Millisecond, s.fetch)
		if !errors.Is(err, context.DeadlineExceeded) {
			t.Fatalf("All = %v, want an error wrapping context.DeadlineExceeded", err)
		}
		if !strings.Contains(err.Error(), "fetch ok:5s: ") {
			t.Errorf("error %q should start with \"fetch ok:5s: \"", err)
		}
		if elapsed := time.Since(start); elapsed != 200*time.Millisecond {
			t.Errorf("All returned after %v, want 200ms (the per-call timeout)", elapsed)
		}
	})
}

func TestAllCancelsTheOthersOnTheFirstFailure(t *testing.T) {
	synctest.Test(t, func(t *testing.T) {
		var s service
		list := []string{"ok:1s", "fail:100ms", "ok:2s", "ok:3s", "ok:4s"}
		start := time.Now()
		bodies, err := fetch.All(t.Context(), list, 3, 10*time.Second, s.fetch)
		if !errors.Is(err, errDown) {
			t.Fatalf("All = %v, want an error wrapping the failure", err)
		}
		if !strings.HasPrefix(err.Error(), "fetch fail:100ms: ") {
			t.Errorf("error = %q, want it to start with \"fetch fail:100ms: \"", err)
		}
		if bodies != nil {
			t.Errorf("All returned bodies %q with an error, want nil", bodies)
		}
		if elapsed := time.Since(start); elapsed != 100*time.Millisecond {
			t.Errorf("All returned after %v, want 100ms: the failure must cancel the other calls", elapsed)
		}
		if calls := s.calls.Load(); calls != 3 {
			t.Errorf("%d calls started, want 3: no new call may start after the failure", calls)
		}
		if cancelled := s.cancelled.Load(); cancelled != 2 {
			t.Errorf("%d running calls saw the cancellation, want 2", cancelled)
		}
	})
}

func TestAllStopsWhenTheParentContextEnds(t *testing.T) {
	synctest.Test(t, func(t *testing.T) {
		var s service
		ctx, cancel := context.WithTimeout(t.Context(), 250*time.Millisecond)
		defer cancel()
		start := time.Now()
		_, err := fetch.All(ctx, urls(10, "100ms"), 2, time.Second, s.fetch)
		if !errors.Is(err, context.DeadlineExceeded) {
			t.Fatalf("All = %v, want an error wrapping the parent's DeadlineExceeded", err)
		}
		if elapsed := time.Since(start); elapsed != 250*time.Millisecond {
			t.Errorf("All returned after %v, want 250ms", elapsed)
		}
		if calls := s.calls.Load(); calls != 6 {
			t.Errorf("%d calls started, want 6 (3 rounds of 2 before the deadline)", calls)
		}
	})
}

func TestFirstReturnsTheFastestAndCancelsTheRest(t *testing.T) {
	synctest.Test(t, func(t *testing.T) {
		var s service
		start := time.Now()
		body, err := fetch.First(t.Context(), []string{"ok:3s", "ok:120ms", "fail:50ms", "ok:2s"}, s.fetch)
		if err != nil || body != "body of ok:120ms" {
			t.Fatalf("First = %q, %v; want the body of ok:120ms", body, err)
		}
		if elapsed := time.Since(start); elapsed != 120*time.Millisecond {
			t.Errorf("First returned after %v, want 120ms", elapsed)
		}
		synctest.Wait()
		if cancelled := s.cancelled.Load(); cancelled != 2 {
			t.Errorf("%d slower calls were cancelled, want 2", cancelled)
		}
	})
}

func TestFirstJoinsEveryFailure(t *testing.T) {
	synctest.Test(t, func(t *testing.T) {
		var s service
		_, err := fetch.First(t.Context(), []string{"fail:30ms", "fail:10ms", "fail:20ms"}, s.fetch)
		if !errors.Is(err, errDown) {
			t.Fatalf("First = %v, want the failures", err)
		}
		joined, ok := err.(interface{ Unwrap() []error })
		if !ok || len(joined.Unwrap()) != 3 {
			t.Errorf("First error = %q, want errors.Join of 3 errors", err)
		}
		for _, url := range []string{"fail:10ms", "fail:20ms", "fail:30ms"} {
			if !strings.Contains(err.Error(), "fetch "+url+": ") {
				t.Errorf("First error %q should mention %q", err, "fetch "+url+": ")
			}
		}
	})
}

func TestFirstWithNoURLs(t *testing.T) {
	synctest.Test(t, func(t *testing.T) {
		var s service
		if _, err := fetch.First(t.Context(), nil, s.fetch); !errors.Is(err, fetch.ErrNoURLs) {
			t.Errorf("First(nil) = %v, want ErrNoURLs", err)
		}
	})
}
