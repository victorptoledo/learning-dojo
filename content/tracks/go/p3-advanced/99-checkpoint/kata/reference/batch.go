// Package batch groups items added from many goroutines into batches and flushes them
// from one goroutine.
package batch

import (
	"context"
	"errors"
	"fmt"
	"sync"
	"sync/atomic"
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

// Batcher groups items into batches. Create it with Start.
type Batcher[T any] struct {
	cfg   Config
	flush FlushFunc[T]

	in   chan T        // unbuffered: Add returns nil only once the loop holds the item
	quit chan struct{} // closed by Close, never sent on
	done chan struct{} // closed when the loop has returned

	closeOnce sync.Once
	errs      []error // written only by the loop, read only after done is closed

	items, batches, failed atomic.Int64
}

// Start runs the batcher's loop on one goroutine until Close is called or ctx ends.
func Start[T any](ctx context.Context, cfg Config, flush FlushFunc[T]) *Batcher[T] {
	if cfg.Size < 1 {
		cfg.Size = 1
	}
	b := &Batcher[T]{
		cfg:   cfg,
		flush: flush,
		in:    make(chan T),
		quit:  make(chan struct{}),
		done:  make(chan struct{}),
	}
	go b.run(ctx)
	return b
}

// Add hands item to the batcher. It blocks while a flush runs, returns ctx.Err() if ctx
// ends first (the item is then not added), and ErrClosed once the batcher is closed.
func (b *Batcher[T]) Add(ctx context.Context, item T) error {
	select {
	case <-b.quit:
		return ErrClosed
	case <-b.done:
		return ErrClosed
	default:
	}
	select {
	case b.in <- item:
		return nil
	case <-b.quit:
		return ErrClosed
	case <-b.done:
		return ErrClosed
	case <-ctx.Done():
		return ctx.Err()
	}
}

// Close stops accepting items, flushes the last batch, waits for the loop to return and
// reports every flush error. It is safe to call more than once and from many goroutines.
func (b *Batcher[T]) Close() error {
	b.closeOnce.Do(func() { close(b.quit) })
	<-b.done
	return errors.Join(b.errs...)
}

// Stats can be read at any time, from any goroutine.
func (b *Batcher[T]) Stats() Stats {
	return Stats{Items: b.items.Load(), Batches: b.batches.Load(), Failed: b.failed.Load()}
}

func (b *Batcher[T]) run(ctx context.Context) {
	defer close(b.done)
	var (
		batch   []T
		timer   *time.Timer
		timeout <-chan time.Time
	)
	flush := func(ctx context.Context) {
		if timer != nil {
			timer.Stop()
			timer, timeout = nil, nil
		}
		if len(batch) == 0 {
			return
		}
		number := b.batches.Add(1)
		b.items.Add(int64(len(batch)))
		if err := b.flush(ctx, batch); err != nil {
			b.failed.Add(1)
			b.errs = append(b.errs, fmt.Errorf("batch %d: %w", number, err))
		}
		batch = nil // a new array for the next batch: the flushed one is not ours any more
	}
	for {
		select {
		case item := <-b.in:
			batch = append(batch, item)
			if len(batch) == 1 && b.cfg.Wait > 0 {
				timer = time.NewTimer(b.cfg.Wait)
				timeout = timer.C
			}
			if len(batch) >= b.cfg.Size {
				flush(ctx)
			}
		case <-timeout:
			flush(ctx)
		case <-b.quit:
			flush(context.WithoutCancel(ctx))
			return
		case <-ctx.Done():
			flush(context.WithoutCancel(ctx))
			return
		}
	}
}
