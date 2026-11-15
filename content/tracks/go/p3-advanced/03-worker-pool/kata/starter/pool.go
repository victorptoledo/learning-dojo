// Package pool runs work on a bounded number of goroutines.
package pool

// Map calls fn on every job with at most workers goroutines at a time (fewer than 1 means
// 1). results[i] is the value fn returned for jobs[i]. Every job runs even when some fail;
// the error joins "job i: cause" for each failing job, in job order.
func Map[In, Out any](jobs []In, workers int, fn func(In) (Out, error)) ([]Out, error) {
	panic("todo: write Map")
}

// Generate sends nums, in order, on the returned channel and then closes it.
func Generate(nums ...int) <-chan int {
	panic("todo: write Generate")
}

// Square reads in with workers goroutines (fewer than 1 means 1) and sends n*n for every
// n, in any order. The output is closed after every worker has finished.
func Square(in <-chan int, workers int) <-chan int {
	panic("todo: write Square")
}

// Sum adds every value received from in until it is closed.
func Sum(in <-chan int) int {
	panic("todo: write Sum")
}
