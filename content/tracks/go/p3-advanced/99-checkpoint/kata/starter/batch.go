// Package batch groups items added from many goroutines into batches and flushes them
// from one goroutine.
package batch

import (
	"context"
	"errors"
	"time"
)

// ErrClosed is returned by Add after Close, or after the context given to Start ended.
var ErrClosed = errors.New("batch: closed")

// Config says when a batch is flushed.
type Config struct {
	// Size flushes a batch as soon as it holds this many items (fewer than 1 means 1).
	Size int
	// Wait flushes a partial batch this long after its first item was added. Zero or less
	// means only Size, Close and the end of the context flush.
	Wait time.Duration
}

// Stats counts what the batcher has flushed so far.
type Stats struct {
	Items   int64 // items handed to the flush function
	Batches int64 // calls to the flush function
	Failed  int64 // calls that returned an error
}

// FlushFunc receives each batch. The slice belongs to it: the batcher never touches it again.
type FlushFunc[T any] func(ctx context.Context, batch []T) error

// Batcher groups items into batches. Create it with Start. Add the fields you need.
type Batcher[T any] struct {
}

// Start runs the batcher's loop on one goroutine until Close is called or ctx ends.
func Start[T any](ctx context.Context, cfg Config, flush FlushFunc[T]) *Batcher[T] {
	panic("todo: write Start")
}

// Add hands item to the batcher. It blocks while a flush runs, returns ctx.Err() if ctx
// ends first (the item is then not added), and ErrClosed once the batcher is closed.
func (b *Batcher[T]) Add(ctx context.Context, item T) error {
	panic("todo: write Add")
}

// Close stops accepting items, flushes the last batch, waits for the loop to return and
// reports every flush error. It is safe to call more than once and from many goroutines.
func (b *Batcher[T]) Close() error {
	panic("todo: write Close")
}

// Stats can be read at any time, from any goroutine.
func (b *Batcher[T]) Stats() Stats {
	panic("todo: write Stats")
}
