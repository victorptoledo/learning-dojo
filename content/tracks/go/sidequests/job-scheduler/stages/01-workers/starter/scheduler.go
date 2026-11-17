// Package scheduler runs jobs on a pool of workers. See README.md.
package scheduler

import (
	"context"
	"errors"
)

// Status is how a job ended.
type Status string

const (
	Succeeded Status = "succeeded"
	Failed    Status = "failed"
	Skipped   Status = "skipped"  // stage 3
	Canceled  Status = "canceled" // stage 5
)

// Job is a unit of work. Later stages add fields.
type Job struct {
	ID  string
	Run func(ctx context.Context) error
}

// Result is what happened to a job.
type Result struct {
	Status   Status
	Attempts int
	Err      error
}

// Scheduler runs jobs on a fixed number of workers.
type Scheduler struct {
}

// New makes a scheduler with `workers` workers.
func New(workers int) *Scheduler {
	return &Scheduler{}
}

// Submit adds a job; it starts as soon as it can.
func (s *Scheduler) Submit(job Job) error {
	return errors.New("todo: submit")
}

// Wait blocks until every submitted job is finished and returns their results.
func (s *Scheduler) Wait() map[string]Result {
	return nil
}
