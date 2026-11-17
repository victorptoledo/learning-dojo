// Package scheduler runs jobs on a pool of workers.
//
// One mutex guards all state. dispatch, called with the lock held after every change,
// skips the jobs whose dependencies failed and starts the best ready jobs while workers
// are free; each started job runs on its own goroutine and reports back under the lock.
package scheduler

import (
	"context"
	"errors"
	"fmt"
	"sync"
	"time"
)

// Status is how a job ended.
type Status string

const (
	Succeeded Status = "succeeded"
	Failed    Status = "failed"
	Skipped   Status = "skipped"  // stage 3
	Canceled  Status = "canceled" // stage 5
)

// Job is a unit of work.
type Job struct {
	ID       string
	Run      func(ctx context.Context) error
	Retries  int
	Timeout  time.Duration
	After    []string
	Priority int
}

// Result is what happened to a job.
type Result struct {
	Status   Status
	Attempts int
	Err      error
}

type state int

const (
	pending state = iota
	running
	done
)

type entry struct {
	job    Job
	seq    int
	state  state
	result Result
}

// Scheduler runs jobs on a fixed number of workers.
type Scheduler struct {
	mu      sync.Mutex
	cond    *sync.Cond
	workers int
	busy    int
	entries map[string]*entry
	order   []*entry
	ctx     context.Context
	cancel  context.CancelFunc
}

// New makes a scheduler with `workers` workers (at least one).
func New(workers int) *Scheduler {
	ctx, cancel := context.WithCancel(context.Background())
	s := &Scheduler{workers: max(workers, 1), entries: map[string]*entry{}, ctx: ctx, cancel: cancel}
	s.cond = sync.NewCond(&s.mu)
	return s
}

// Submit adds a job; it starts as soon as a worker is free and it is ready.
func (s *Scheduler) Submit(job Job) error {
	s.mu.Lock()
	defer s.mu.Unlock()
	switch {
	case job.ID == "":
		return errors.New("a job needs an ID")
	case job.Run == nil:
		return fmt.Errorf("job %s has no Run", job.ID)
	case s.entries[job.ID] != nil:
		return fmt.Errorf("job %s was already submitted", job.ID)
	}
	for _, dependency := range job.After {
		if dependency == job.ID || s.entries[dependency] == nil {
			return fmt.Errorf("job %s depends on %q, which was not submitted before", job.ID, dependency)
		}
	}
	e := &entry{job: job, seq: len(s.order)}
	s.entries[job.ID] = e
	s.order = append(s.order, e)
	s.dispatch()
	return nil
}

// ready says whether a pending job can start: every dependency succeeded.
func (s *Scheduler) ready(e *entry) bool {
	for _, dependency := range e.job.After {
		if d := s.entries[dependency]; d.state != done || d.result.Status != Succeeded {
			return false
		}
	}
	return true
}

// better says whether a should start before b.
func better(a, b *entry) bool {
	if a.job.Priority != b.job.Priority {
		return a.job.Priority > b.job.Priority
	}
	return a.seq < b.seq
}

func (s *Scheduler) finish(e *entry, result Result) {
	e.state, e.result = done, result
	s.cond.Broadcast()
}

// dispatch must be called with the lock held.
func (s *Scheduler) dispatch() {
	// Skip what can never run; a skip may unlock more skips, so repeat until stable.
	for changed := true; changed; {
		changed = false
		for _, e := range s.order {
			if e.state != pending {
				continue
			}
			for _, dependency := range e.job.After {
				d := s.entries[dependency]
				if d.state == done && d.result.Status != Succeeded {
					s.finish(e, Result{Status: Skipped, Err: fmt.Errorf("dependency %s %s", dependency, d.result.Status)})
					changed = true
					break
				}
			}
		}
	}
	for s.busy < s.workers {
		var best *entry
		for _, e := range s.order {
			if e.state == pending && s.ready(e) && (best == nil || better(e, best)) {
				best = e
			}
		}
		if best == nil {
			return
		}
		best.state = running
		s.busy++
		go s.execute(best)
	}
}

// attempt runs the job once, turning a panic into an error.
func attempt(ctx context.Context, run func(context.Context) error) (err error) {
	defer func() {
		if recovered := recover(); recovered != nil {
			err = fmt.Errorf("panic: %v", recovered)
		}
	}()
	return run(ctx)
}

func (s *Scheduler) execute(e *entry) {
	var result Result
	for result.Attempts <= e.job.Retries {
		ctx, cancel := s.ctx, context.CancelFunc(func() {})
		if e.job.Timeout > 0 {
			ctx, cancel = context.WithTimeout(s.ctx, e.job.Timeout)
		}
		result.Attempts++
		result.Err = attempt(ctx, e.job.Run)
		cancel()
		if result.Err == nil || s.ctx.Err() != nil {
			break
		}
	}
	switch {
	case result.Err == nil:
		result.Status = Succeeded
	default:
		result.Status = Failed
	}
	s.mu.Lock()
	defer s.mu.Unlock()
	s.busy--
	s.finish(e, result)
	s.dispatch()
}

// Wait blocks until every submitted job is finished and returns their results.
func (s *Scheduler) Wait() map[string]Result {
	s.mu.Lock()
	defer s.mu.Unlock()
	for s.unfinished() {
		s.cond.Wait()
	}
	results := make(map[string]Result, len(s.entries))
	for id, e := range s.entries {
		results[id] = e.result
	}
	return results
}

func (s *Scheduler) unfinished() bool {
	for _, e := range s.order {
		if e.state != done {
			return true
		}
	}
	return false
}
