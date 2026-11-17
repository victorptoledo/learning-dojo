package scheduler_test

import (
	"context"
	"errors"
	"fmt"
	"sync/atomic"
	"testing"
	"time"

	"dojo/scheduler"
)

// flaky fails its first `failures` attempts.
func flaky(failures int32, calls *atomic.Int32) func(context.Context) error {
	return func(context.Context) error {
		if calls.Add(1) <= failures {
			return fmt.Errorf("attempt %d failed", calls.Load())
		}
		return nil
	}
}

func TestAFlakyJobSucceedsOnARetry(t *testing.T) {
	s := scheduler.New(1)
	var calls atomic.Int32
	submit(t, s, scheduler.Job{ID: "flaky", Retries: 3, Run: flaky(2, &calls)})
	r := wait(t, s)["flaky"]
	if r.Status != scheduler.Succeeded || r.Attempts != 3 || r.Err != nil {
		t.Errorf("%+v, want succeeded after 3 attempts", r)
	}
}

func TestRetriesRunOut(t *testing.T) {
	s := scheduler.New(1)
	var calls atomic.Int32
	submit(t, s, scheduler.Job{ID: "doomed", Retries: 2, Run: flaky(10, &calls)})
	r := wait(t, s)["doomed"]
	if r.Status != scheduler.Failed || r.Attempts != 3 || r.Err == nil || r.Err.Error() != "attempt 3 failed" {
		t.Errorf("%+v, want failed after 3 attempts with the last error", r)
	}
	if calls.Load() != 3 {
		t.Errorf("Run was called %d times", calls.Load())
	}
}

func TestAPanicIsRetriedToo(t *testing.T) {
	s := scheduler.New(1)
	var calls atomic.Int32
	submit(t, s, scheduler.Job{ID: "j", Retries: 1, Run: func(context.Context) error {
		if calls.Add(1) == 1 {
			panic("first time")
		}
		return nil
	}})
	if r := wait(t, s)["j"]; r.Status != scheduler.Succeeded || r.Attempts != 2 {
		t.Errorf("%+v", r)
	}
}

func TestEachAttemptHasItsDeadline(t *testing.T) {
	s := scheduler.New(1)
	var calls atomic.Int32
	submit(t, s, scheduler.Job{ID: "slow", Retries: 1, Timeout: 50 * time.Millisecond, Run: func(ctx context.Context) error {
		calls.Add(1)
		select {
		case <-ctx.Done():
			return ctx.Err()
		case <-time.After(5 * time.Second):
			return nil
		}
	}})
	started := time.Now()
	r := wait(t, s)["slow"]
	if time.Since(started) > 3*time.Second {
		t.Error("the timeout did not cut the attempts short")
	}
	if r.Status != scheduler.Failed || r.Attempts != 2 || !errors.Is(r.Err, context.DeadlineExceeded) {
		t.Errorf("%+v, want failed after 2 attempts with context.DeadlineExceeded", r)
	}
}

func TestAJobWithinItsTimeoutSucceeds(t *testing.T) {
	s := scheduler.New(1)
	submit(t, s, scheduler.Job{ID: "quick", Timeout: time.Second, Run: func(ctx context.Context) error {
		if _, ok := ctx.Deadline(); !ok {
			return errors.New("no deadline on the context")
		}
		return nil
	}})
	if r := wait(t, s)["quick"]; r.Status != scheduler.Succeeded {
		t.Errorf("%+v", r)
	}
}

func TestRetryingKeepsTheWorkerBusy(t *testing.T) {
	s := scheduler.New(1)
	var g gauge
	var calls atomic.Int32
	run := func(fail bool) func(context.Context) error {
		return func(context.Context) error {
			g.enter()
			defer g.leave()
			time.Sleep(10 * time.Millisecond)
			if fail && calls.Add(1) < 3 {
				return errors.New("again")
			}
			return nil
		}
	}
	submit(t, s, scheduler.Job{ID: "retrying", Retries: 5, Run: run(true)})
	submit(t, s, scheduler.Job{ID: "other", Run: run(false)})
	wait(t, s)
	if g.peak() != 1 {
		t.Errorf("%d jobs ran at once on 1 worker", g.peak())
	}
}
