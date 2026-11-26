// Package shortlinks is a small link shortener service, written to run in production.
package shortlinks

import (
	"context"
	"errors"
	"log/slog"
	"net"
	"net/http"
	"time"
)

// Errors a Store returns.
var (
	ErrNotFound = errors.New("shortlinks: not found")
	ErrExists   = errors.New("shortlinks: code already used")
)

// Store keeps the links. Calls must respect ctx.
type Store interface {
	Save(ctx context.Context, code, target string) error // ErrExists if code is taken
	Lookup(ctx context.Context, code string) (string, error)
}

// Defaults for zero Config fields.
const (
	DefaultStoreTimeout = 2 * time.Second
	DefaultMaxBody      = 1024
)

// Config configures a Server. NewCode is required; the other fields have defaults.
type Config struct {
	NewCode      func() string // a new random code for a link
	NewRequestID func() string // nil: "req-1", "req-2", ...
	Logger       *slog.Logger  // nil: logs are discarded
	StoreTimeout time.Duration // per store call; 0 means DefaultStoreTimeout
	MaxBody      int64         // bytes in a POST body; 0 means DefaultMaxBody
}

// Server is an http.Handler. Create it with New. Add the fields you need.
type Server struct {
}

// New builds the server and its routes.
func New(cfg Config, store Store) *Server {
	panic("todo: write New")
}

func (s *Server) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	panic("todo: write ServeHTTP")
}

// RunOptions control the shutdown of Run.
type RunOptions struct {
	// DrainDelay is how long /readyz answers 503 while requests are still served, so a
	// load balancer stops sending new ones before the listener closes.
	DrainDelay time.Duration
	// ShutdownTimeout is how long in-flight requests get to finish after the drain.
	ShutdownTimeout time.Duration
}

// Run serves on ln until ctx ends, then drains and shuts down gracefully. It returns nil
// after a clean shutdown, the error of Shutdown if requests did not finish in time, or the
// error of Serve if serving failed.
func (s *Server) Run(ctx context.Context, ln net.Listener, opts RunOptions) error {
	panic("todo: write Run")
}
