package scheduler_test

import (
	"context"
	"errors"
	"testing"
	"time"

	"dojo/scheduler"
)

func TestShutdownLetsRunningJobsFinishAndCancelsTheRest(t *testing.T) {
	s := scheduler.New(1)
	started := make(chan string, 1)
	release := make(chan struct{})
	submit(t, s, scheduler.Job{ID: "running", Run: blocker(started, release, "running")})
	<-started
	submit(t, s, scheduler.Job{ID: "queued", Run: blocker(nil, release, "queued")})
	submit(t, s, scheduler.Job{ID: "dependent", After: []string{"running"}, Run: blocker(nil, release, "dependent")})

	done := make(chan error, 1)
	go func() { done <- s.Shutdown(context.Background()) }()
	time.Sleep(50 * time.Millisecond)
	close(release)
	select {
	case err := <-done:
		if err != nil {
			t.Errorf("Shutdown returned %v, want nil", err)
		}
	case <-time.After(5 * time.Second):
		t.Fatal("Shutdown did not return")
	}
	results := wait(t, s)
	if results["running"].Status != scheduler.Succeeded {
		t.Errorf("running: %+v, want it to finish", results["running"])
	}
	for _, id := range []string{"queued", "dependent"} {
		if results[id].Status != scheduler.Canceled || results[id].Attempts != 0 {
			t.Errorf("%s: %+v, want canceled without an attempt", id, results[id])
		}
	}
}

func TestSubmitAfterShutdownIsRefused(t *testing.T) {
	s := scheduler.New(1)
	if err := s.Shutdown(context.Background()); err != nil {
		t.Fatal(err)
	}
	err := s.Submit(scheduler.Job{ID: "late", Run: func(context.Context) error { return nil }})
	if !errors.Is(err, scheduler.ErrClosed) {
		t.Errorf("Submit returned %v, want ErrClosed", err)
	}
}

func TestADeadlineCancelsRunningJobs(t *testing.T) {
	s := scheduler.New(2)
	started := make(chan string, 2)
	submit(t, s, scheduler.Job{ID: "stubborn", Retries: 5, Run: blocker(started, nil, "stubborn")})
	submit(t, s, scheduler.Job{ID: "quick", Run: func(context.Context) error {
		started <- "quick"
		return nil
	}})
	<-started
	<-started
	ctx, cancel := context.WithTimeout(context.Background(), 100*time.Millisecond)
	defer cancel()
	began := time.Now()
	err := s.Shutdown(ctx)
	if !errors.Is(err, context.DeadlineExceeded) {
		t.Errorf("Shutdown returned %v, want the context's error", err)
	}
	if time.Since(began) > 3*time.Second {
		t.Error("Shutdown waited long after its deadline")
	}
	results := wait(t, s)
	if r := results["stubborn"]; r.Status != scheduler.Canceled || r.Attempts != 1 {
		t.Errorf("stubborn: %+v, want canceled after 1 attempt (no retries once shutting down)", r)
	}
	if results["quick"].Status != scheduler.Succeeded {
		t.Errorf("quick: %+v", results["quick"])
	}
}
