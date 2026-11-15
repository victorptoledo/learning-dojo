// Package fetch calls a slow service for many URLs at once, with limits and cancellation.
package fetch

import (
	"context"
	"errors"
	"fmt"
	"sync"
	"time"
)

// Fetcher gets the body of a URL. It must return promptly once ctx is done.
type Fetcher func(ctx context.Context, url string) (string, error)

// ErrNoURLs is returned by First when it is given no URLs.
var ErrNoURLs = errors.New("no urls")

// All fetches every URL with at most limit calls at once (fewer than 1 means 1) and gives
// each call perCall to finish. bodies[i] is the body of urls[i]. The first failing call
// cancels the others, and All returns its error as "fetch URL: cause". If ctx ends first,
// All returns an error that wraps ctx's error.
func All(ctx context.Context, urls []string, limit int, perCall time.Duration, fetch Fetcher) ([]string, error) {
	ctx, cancel := context.WithCancelCause(ctx)
	defer cancel(nil)

	bodies := make([]string, len(urls))
	slots := make(chan struct{}, max(limit, 1))
	var wg sync.WaitGroup
	for i, url := range urls {
		select {
		case slots <- struct{}{}:
		case <-ctx.Done():
		}
		if ctx.Err() != nil {
			break
		}
		wg.Go(func() {
			defer func() { <-slots }()
			callCtx, cancelCall := context.WithTimeout(ctx, perCall)
			defer cancelCall()
			body, err := fetch(callCtx, url)
			if err != nil {
				cancel(fmt.Errorf("fetch %s: %w", url, err))
				return
			}
			bodies[i] = body
		})
	}
	wg.Wait()
	if err := context.Cause(ctx); err != nil {
		return nil, err
	}
	return bodies, nil
}

// First asks every URL at once and returns the first successful body, cancelling the other
// calls. When every call fails it returns errors.Join of their errors, each as
// "fetch URL: cause". With no URLs it returns ErrNoURLs.
func First(ctx context.Context, urls []string, fetch Fetcher) (string, error) {
	if len(urls) == 0 {
		return "", ErrNoURLs
	}
	ctx, cancel := context.WithCancel(ctx)
	defer cancel()

	type result struct {
		body string
		err  error
	}
	results := make(chan result, len(urls))
	for _, url := range urls {
		go func() {
			body, err := fetch(ctx, url)
			if err != nil {
				err = fmt.Errorf("fetch %s: %w", url, err)
			}
			results <- result{body, err}
		}()
	}
	var failures []error
	for range urls {
		r := <-results
		if r.err == nil {
			return r.body, nil
		}
		failures = append(failures, r.err)
	}
	return "", errors.Join(failures...)
}
