package scheduler_test

import (
	"context"
	"errors"
	"slices"
	"strings"
	"testing"

	"dojo/scheduler"
)

func TestJobsRunAfterTheirDependencies(t *testing.T) {
	s := scheduler.New(4)
	var j journal
	submit(t, s, scheduler.Job{ID: "build", Run: j.job("build")})
	submit(t, s, scheduler.Job{ID: "unit", After: []string{"build"}, Run: j.job("unit")})
	submit(t, s, scheduler.Job{ID: "lint", After: []string{"build"}, Run: j.job("lint")})
	submit(t, s, scheduler.Job{ID: "deploy", After: []string{"unit", "lint"}, Run: j.job("deploy")})
	results := wait(t, s)
	order := j.list()
	if len(order) != 4 || order[0] != "build" || order[3] != "deploy" {
		t.Errorf("ran in the order %v", order)
	}
	for id, r := range results {
		if r.Status != scheduler.Succeeded {
			t.Errorf("%s: %+v", id, r)
		}
	}
}

func TestAFailedDependencySkipsWhatFollows(t *testing.T) {
	s := scheduler.New(2)
	var j journal
	submit(t, s, scheduler.Job{ID: "build", Run: func(context.Context) error { return errors.New("compile error") }})
	submit(t, s, scheduler.Job{ID: "test", After: []string{"build"}, Run: j.job("test")})
	submit(t, s, scheduler.Job{ID: "deploy", After: []string{"test"}, Run: j.job("deploy")})
	submit(t, s, scheduler.Job{ID: "docs", Run: j.job("docs")})
	results := wait(t, s)
	if !slices.Equal(j.list(), []string{"docs"}) {
		t.Errorf("ran %v, want only docs", j.list())
	}
	for _, id := range []string{"test", "deploy"} {
		r := results[id]
		if r.Status != scheduler.Skipped || r.Attempts != 0 || r.Err == nil {
			t.Errorf("%s: %+v, want skipped with an error and no attempt", id, r)
		}
	}
	if !strings.Contains(results["test"].Err.Error(), "build") || !strings.Contains(results["deploy"].Err.Error(), "test") {
		t.Errorf("the errors should name the dependency: %v / %v", results["test"].Err, results["deploy"].Err)
	}
}

func TestWaitingJobsTakeNoWorker(t *testing.T) {
	s := scheduler.New(2)
	started := make(chan string, 3)
	release := make(chan struct{})
	submit(t, s, scheduler.Job{ID: "slow", Run: blocker(started, release, "slow")})
	submit(t, s, scheduler.Job{ID: "waits", After: []string{"slow"}, Run: blocker(started, release, "waits")})
	submit(t, s, scheduler.Job{ID: "free", Run: blocker(started, release, "free")})
	// While slow runs, the second worker must go to free: waits is not ready.
	first, second := <-started, <-started
	if !(first == "slow" && second == "free" || first == "free" && second == "slow") {
		t.Errorf("started %s and %s, want slow and free while waits waits", first, second)
	}
	close(release)
	if results := wait(t, s); results["waits"].Status != scheduler.Succeeded {
		t.Errorf("waits: %+v", results["waits"])
	}
}

func TestUnknownOrSelfDependenciesAreRefused(t *testing.T) {
	s := scheduler.New(1)
	ok := func(context.Context) error { return nil }
	submit(t, s, scheduler.Job{ID: "a", Run: ok})
	for _, job := range []scheduler.Job{
		{ID: "b", After: []string{"later"}, Run: ok},
		{ID: "c", After: []string{"c"}, Run: ok},
	} {
		if err := s.Submit(job); err == nil {
			t.Errorf("Submit(%s) was accepted", job.ID)
		}
	}
	wait(t, s)
}
