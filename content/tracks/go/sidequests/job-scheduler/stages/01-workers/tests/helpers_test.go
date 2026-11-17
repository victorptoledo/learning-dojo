package scheduler_test

import (
	"context"
	"sync"
	"testing"
	"time"

	"dojo/scheduler"
)

// wait calls s.Wait, failing the test if it takes more than 10 seconds.
func wait(t *testing.T, s *scheduler.Scheduler) map[string]scheduler.Result {
	t.Helper()
	done := make(chan map[string]scheduler.Result, 1)
	go func() { done <- s.Wait() }()
	select {
	case results := <-done:
		return results
	case <-time.After(10 * time.Second):
		t.Fatal("Wait did not return within 10 s")
		return nil
	}
}

func submit(t *testing.T, s *scheduler.Scheduler, job scheduler.Job) {
	t.Helper()
	if err := s.Submit(job); err != nil {
		t.Fatalf("Submit(%s): %v", job.ID, err)
	}
}

// gauge counts the jobs running now and the most at once.
type gauge struct {
	mu        sync.Mutex
	now, most int
}

func (g *gauge) enter() {
	g.mu.Lock()
	defer g.mu.Unlock()
	g.now++
	g.most = max(g.most, g.now)
}

func (g *gauge) leave() {
	g.mu.Lock()
	defer g.mu.Unlock()
	g.now--
}

func (g *gauge) peak() int {
	g.mu.Lock()
	defer g.mu.Unlock()
	return g.most
}

// journal records the order in which jobs ran.
type journal struct {
	mu    sync.Mutex
	order []string
}

func (j *journal) job(id string) func(context.Context) error {
	return func(context.Context) error {
		j.mu.Lock()
		defer j.mu.Unlock()
		j.order = append(j.order, id)
		return nil
	}
}

func (j *journal) list() []string {
	j.mu.Lock()
	defer j.mu.Unlock()
	return append([]string(nil), j.order...)
}

// blocker is a job that runs until release is closed, telling `started` when it begins.
func blocker(started chan<- string, release <-chan struct{}, id string) func(context.Context) error {
	return func(ctx context.Context) error {
		if started != nil {
			started <- id
		}
		select {
		case <-release:
			return nil
		case <-ctx.Done():
			return ctx.Err()
		}
	}
}
