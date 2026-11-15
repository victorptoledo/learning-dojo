// Package pool runs work on a bounded number of goroutines.
package pool

import (
	"errors"
	"fmt"
	"sync"
)

// Map calls fn on every job with at most workers goroutines at a time (fewer than 1 means
// 1). results[i] is the value fn returned for jobs[i]. Every job runs even when some fail;
// the error joins "job i: cause" for each failing job, in job order.
func Map[In, Out any](jobs []In, workers int, fn func(In) (Out, error)) ([]Out, error) {
	workers = max(workers, 1)
	results := make([]Out, len(jobs))
	failures := make([]error, len(jobs))
	indexes := make(chan int)
	var wg sync.WaitGroup
	for range min(workers, len(jobs)) {
		wg.Go(func() {
			for i := range indexes {
				value, err := fn(jobs[i])
				if err != nil {
					failures[i] = fmt.Errorf("job %d: %w", i, err)
					continue
				}
				results[i] = value
			}
		})
	}
	for i := range jobs {
		indexes <- i
	}
	close(indexes)
	wg.Wait()
	return results, errors.Join(failures...)
}

// Generate sends nums, in order, on the returned channel and then closes it.
func Generate(nums ...int) <-chan int {
	out := make(chan int)
	go func() {
		defer close(out)
		for _, n := range nums {
			out <- n
		}
	}()
	return out
}

// Square reads in with workers goroutines (fewer than 1 means 1) and sends n*n for every
// n, in any order. The output is closed after every worker has finished.
func Square(in <-chan int, workers int) <-chan int {
	out := make(chan int)
	var wg sync.WaitGroup
	for range max(workers, 1) {
		wg.Go(func() {
			for n := range in {
				out <- n * n
			}
		})
	}
	go func() {
		wg.Wait()
		close(out)
	}()
	return out
}

// Sum adds every value received from in until it is closed.
func Sum(in <-chan int) int {
	total := 0
	for n := range in {
		total += n
	}
	return total
}
