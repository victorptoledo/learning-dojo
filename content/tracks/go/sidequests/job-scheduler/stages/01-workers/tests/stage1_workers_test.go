package scheduler_test

import (
	"context"
	"errors"
	"fmt"
	"strings"
	"testing"
	"time"

	"dojo/scheduler"
)

func TestEveryJobRunsAndIsReported(t *testing.T) {
	s := scheduler.New(2)
	var j journal
	for i := range 5 {
		submit(t, s, scheduler.Job{ID: fmt.Sprint("job", i), Run: j.job(fmt.Sprint("job", i))})
	}
	results := wait(t, s)
	if len(results) != 5 || len(j.list()) != 5 {
		t.Fatalf("results %v, ran %v", results, j.list())
	}
	for id, result := range results {
		if result.Status != scheduler.Succeeded || result.Attempts != 1 || result.Err != nil {
			t.Errorf("%s: %+v, want succeeded after 1 attempt", id, result)
		}
	}
}

func TestNoMoreThanTheWorkersRunAtOnce(t *testing.T) {
	s := scheduler.New(3)
	var g gauge
	for i := range 12 {
		submit(t, s, scheduler.Job{ID: fmt.Sprint(i), Run: func(context.Context) error {
			g.enter()
			defer g.leave()
			time.Sleep(20 * time.Millisecond)
			return nil
		}})
	}
	wait(t, s)
	if g.peak() != 3 {
		t.Errorf("%d jobs ran at once, want exactly 3", g.peak())
	}
}

func TestSubmitDoesNotWaitForAWorker(t *testing.T) {
	s := scheduler.New(1)
	release := make(chan struct{})
	submit(t, s, scheduler.Job{ID: "busy", Run: blocker(nil, release, "busy")})
	done := make(chan error)
	go func() { done <- s.Submit(scheduler.Job{ID: "queued", Run: func(context.Context) error { return nil }}) }()
	select {
	case err := <-done:
		if err != nil {
			t.Fatal(err)
		}
	case <-time.After(2 * time.Second):
		t.Fatal("Submit blocked while the only worker was busy")
	}
	close(release)
	if results := wait(t, s); results["queued"].Status != scheduler.Succeeded {
		t.Errorf("queued: %+v", results["queued"])
	}
}

func TestAnErrorFailsTheJob(t *testing.T) {
	s := scheduler.New(2)
	boom := errors.New("disk full")
	submit(t, s, scheduler.Job{ID: "bad", Run: func(context.Context) error { return boom }})
	submit(t, s, scheduler.Job{ID: "good", Run: func(context.Context) error { return nil }})
	results := wait(t, s)
	if r := results["bad"]; r.Status != scheduler.Failed || !errors.Is(r.Err, boom) {
		t.Errorf("bad: %+v", r)
	}
	if results["good"].Status != scheduler.Succeeded {
		t.Errorf("good: %+v", results["good"])
	}
}

func TestAPanicFailsOnlyThatJob(t *testing.T) {
	s := scheduler.New(1)
	submit(t, s, scheduler.Job{ID: "panics", Run: func(context.Context) error { panic("nil map") }})
	submit(t, s, scheduler.Job{ID: "after", Run: func(context.Context) error { return nil }})
	results := wait(t, s)
	if r := results["panics"]; r.Status != scheduler.Failed || r.Err == nil || !strings.Contains(r.Err.Error(), "panic") {
		t.Errorf("panics: %+v", r)
	}
	if results["after"].Status != scheduler.Succeeded {
		t.Errorf("the worker did not survive the panic: %+v", results["after"])
	}
}

func TestBadJobsAreRefused(t *testing.T) {
	s := scheduler.New(1)
	ok := func(context.Context) error { return nil }
	submit(t, s, scheduler.Job{ID: "a", Run: ok})
	for _, job := range []scheduler.Job{{ID: "", Run: ok}, {ID: "b"}, {ID: "a", Run: ok}} {
		if err := s.Submit(job); err == nil {
			t.Errorf("Submit(%+v) was accepted", job)
		}
	}
	if results := wait(t, s); len(results) != 1 {
		t.Errorf("results %v, want only a", results)
	}
}

func TestWaitCanBeCalledAgain(t *testing.T) {
	s := scheduler.New(1)
	ok := func(context.Context) error { return nil }
	submit(t, s, scheduler.Job{ID: "first", Run: ok})
	wait(t, s)
	submit(t, s, scheduler.Job{ID: "second", Run: ok})
	if results := wait(t, s); len(results) != 2 || results["second"].Status != scheduler.Succeeded {
		t.Errorf("results %v", results)
	}
}
