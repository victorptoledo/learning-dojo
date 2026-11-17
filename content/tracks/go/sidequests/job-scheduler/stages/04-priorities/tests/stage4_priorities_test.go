package scheduler_test

import (
	"slices"
	"testing"

	"dojo/scheduler"
)

func TestTheHighestPriorityGoesFirst(t *testing.T) {
	s := scheduler.New(1)
	started := make(chan string, 1)
	release := make(chan struct{})
	submit(t, s, scheduler.Job{ID: "busy", Run: blocker(started, release, "busy")})
	<-started
	var j journal
	for _, job := range []struct {
		id       string
		priority int
	}{{"low", 1}, {"high", 5}, {"normal", 0}, {"mid", 3}, {"high-too", 5}} {
		submit(t, s, scheduler.Job{ID: job.id, Priority: job.priority, Run: j.job(job.id)})
	}
	close(release)
	wait(t, s)
	want := []string{"high", "high-too", "mid", "low", "normal"}
	if got := j.list(); !slices.Equal(got, want) {
		t.Errorf("ran %v, want %v", got, want)
	}
}

func TestPriorityDoesNotJumpOverDependencies(t *testing.T) {
	s := scheduler.New(1)
	started := make(chan string, 1)
	release := make(chan struct{})
	submit(t, s, scheduler.Job{ID: "busy", Run: blocker(started, release, "busy")})
	<-started
	var j journal
	submit(t, s, scheduler.Job{ID: "base", Priority: 0, Run: j.job("base")})
	submit(t, s, scheduler.Job{ID: "urgent", Priority: 9, After: []string{"base"}, Run: j.job("urgent")})
	submit(t, s, scheduler.Job{ID: "other", Priority: 1, Run: j.job("other")})
	close(release)
	wait(t, s)
	want := []string{"other", "base", "urgent"}
	if got := j.list(); !slices.Equal(got, want) {
		t.Errorf("ran %v, want %v: urgent is not ready before base, and other outranks base", got, want)
	}
}
