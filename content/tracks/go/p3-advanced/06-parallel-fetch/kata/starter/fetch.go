// Package fetch calls a slow service for many URLs at once, with limits and cancellation.
package fetch

import (
	"context"
	"errors"
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
	panic("todo: write All")
}

// First asks every URL at once and returns the first successful body, cancelling the other
// calls. When every call fails it returns errors.Join of their errors, each as
// "fetch URL: cause". With no URLs it returns ErrNoURLs.
func First(ctx context.Context, urls []string, fetch Fetcher) (string, error) {
	panic("todo: write First")
}
