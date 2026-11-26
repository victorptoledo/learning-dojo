// Package shortlinks is a small link shortener service, written to run in production.
package shortlinks

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"log/slog"
	"net"
	"net/http"
	"net/url"
	"regexp"
	"strconv"
	"sync/atomic"
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
	codeAttempts        = 3
)

// Config configures a Server. NewCode is required; the other fields have defaults.
type Config struct {
	NewCode      func() string // a new random code for a link
	NewRequestID func() string // nil: "req-1", "req-2", ...
	Logger       *slog.Logger  // nil: logs are discarded
	StoreTimeout time.Duration // per store call; 0 means DefaultStoreTimeout
	MaxBody      int64         // bytes in a POST body; 0 means DefaultMaxBody
}

// Server is an http.Handler. Create it with New.
type Server struct {
	cfg      Config
	store    Store
	handler  http.Handler
	draining atomic.Bool
	requests atomic.Int64
}

// New builds the server and its routes.
func New(cfg Config, store Store) *Server {
	if cfg.Logger == nil {
		cfg.Logger = slog.New(slog.DiscardHandler)
	}
	if cfg.StoreTimeout <= 0 {
		cfg.StoreTimeout = DefaultStoreTimeout
	}
	if cfg.MaxBody <= 0 {
		cfg.MaxBody = DefaultMaxBody
	}
	s := &Server{cfg: cfg, store: store}
	if s.cfg.NewRequestID == nil {
		s.cfg.NewRequestID = func() string { return "req-" + strconv.FormatInt(s.requests.Add(1), 10) }
	}
	mux := http.NewServeMux()
	mux.HandleFunc("POST /links", s.create)
	mux.HandleFunc("GET /{code}", s.follow)
	mux.HandleFunc("GET /healthz", func(w http.ResponseWriter, _ *http.Request) { io.WriteString(w, "ok\n") })
	mux.HandleFunc("GET /readyz", s.ready)
	s.handler = s.requestID(s.accessLog(s.recover(mux)))
	return s
}

func (s *Server) ServeHTTP(w http.ResponseWriter, r *http.Request) { s.handler.ServeHTTP(w, r) }

// ---- handlers -------------------------------------------------------------------------

func (s *Server) create(w http.ResponseWriter, r *http.Request) {
	r.Body = http.MaxBytesReader(w, r.Body, s.cfg.MaxBody)
	var in struct {
		URL string `json:"url"`
	}
	dec := json.NewDecoder(r.Body)
	dec.DisallowUnknownFields()
	if err := dec.Decode(&in); err != nil {
		if _, ok := errors.AsType[*http.MaxBytesError](err); ok {
			writeError(w, http.StatusRequestEntityTooLarge, "body too large")
			return
		}
		writeError(w, http.StatusBadRequest, "body must be a JSON object with a url")
		return
	}
	target, err := url.Parse(in.URL)
	if err != nil || (target.Scheme != "http" && target.Scheme != "https") || target.Host == "" {
		writeError(w, http.StatusBadRequest, "url must be an absolute http or https URL")
		return
	}
	for range codeAttempts {
		code := s.cfg.NewCode()
		err := s.withStore(r.Context(), func(ctx context.Context) error { return s.store.Save(ctx, code, in.URL) })
		if errors.Is(err, ErrExists) {
			continue
		}
		if err != nil {
			s.storeFailed(w, r, err)
			return
		}
		w.Header().Set("Location", "/"+code)
		writeJSON(w, http.StatusCreated, map[string]string{"code": code, "url": in.URL})
		return
	}
	s.storeFailed(w, r, fmt.Errorf("no free code after %d attempts", codeAttempts))
}

func (s *Server) follow(w http.ResponseWriter, r *http.Request) {
	var target string
	err := s.withStore(r.Context(), func(ctx context.Context) error {
		var err error
		target, err = s.store.Lookup(ctx, r.PathValue("code"))
		return err
	})
	switch {
	case errors.Is(err, ErrNotFound):
		writeError(w, http.StatusNotFound, "no such link")
	case err != nil:
		s.storeFailed(w, r, err)
	default:
		http.Redirect(w, r, target, http.StatusFound)
	}
}

func (s *Server) ready(w http.ResponseWriter, _ *http.Request) {
	if s.draining.Load() {
		http.Error(w, "draining", http.StatusServiceUnavailable)
		return
	}
	io.WriteString(w, "ready\n")
}

// withStore gives one store call its own deadline, under the request's context.
func (s *Server) withStore(parent context.Context, call func(context.Context) error) error {
	ctx, cancel := context.WithTimeout(parent, s.cfg.StoreTimeout)
	defer cancel()
	return call(ctx)
}

// storeFailed logs the real cause and gives the client a generic answer.
func (s *Server) storeFailed(w http.ResponseWriter, r *http.Request, err error) {
	s.cfg.Logger.ErrorContext(r.Context(), "store failed", "request_id", requestIDFrom(r.Context()), "error", err)
	if errors.Is(err, context.DeadlineExceeded) {
		writeError(w, http.StatusGatewayTimeout, "store timeout")
		return
	}
	writeError(w, http.StatusInternalServerError, "internal error")
}

func writeJSON(w http.ResponseWriter, status int, v any) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	json.NewEncoder(w).Encode(v)
}

func writeError(w http.ResponseWriter, status int, message string) {
	writeJSON(w, status, map[string]string{"error": message})
}

// ---- middleware -----------------------------------------------------------------------

type ctxKey struct{}

func requestIDFrom(ctx context.Context) string {
	id, _ := ctx.Value(ctxKey{}).(string)
	return id
}

var validID = regexp.MustCompile(`^[A-Za-z0-9-]{1,64}$`)

func (s *Server) requestID(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		id := r.Header.Get("X-Request-ID")
		if !validID.MatchString(id) {
			id = s.cfg.NewRequestID()
		}
		w.Header().Set("X-Request-ID", id)
		next.ServeHTTP(w, r.WithContext(context.WithValue(r.Context(), ctxKey{}, id)))
	})
}

type statusWriter struct {
	http.ResponseWriter
	status int
}

func (w *statusWriter) WriteHeader(status int) {
	if w.status == 0 {
		w.status = status
	}
	w.ResponseWriter.WriteHeader(status)
}

func (w *statusWriter) Write(b []byte) (int, error) {
	if w.status == 0 {
		w.status = http.StatusOK
	}
	return w.ResponseWriter.Write(b)
}

func (w *statusWriter) Unwrap() http.ResponseWriter { return w.ResponseWriter }

func (s *Server) accessLog(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		start := time.Now()
		sw := &statusWriter{ResponseWriter: w}
		next.ServeHTTP(sw, r)
		if sw.status == 0 {
			sw.status = http.StatusOK
		}
		level := slog.LevelInfo
		if sw.status >= 500 {
			level = slog.LevelError
		}
		s.cfg.Logger.LogAttrs(r.Context(), level, "request",
			slog.String("request_id", requestIDFrom(r.Context())),
			slog.String("method", r.Method),
			slog.String("pattern", r.Pattern),
			slog.String("path", r.URL.Path), // never the query: it may carry tokens
			slog.Int("status", sw.status),
			slog.Duration("duration", time.Since(start)),
		)
	})
}

func (s *Server) recover(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		defer func() {
			if v := recover(); v != nil {
				if v == http.ErrAbortHandler {
					panic(v)
				}
				s.cfg.Logger.ErrorContext(r.Context(), "panic", "request_id", requestIDFrom(r.Context()), "value", fmt.Sprint(v))
				writeError(w, http.StatusInternalServerError, "internal error")
			}
		}()
		next.ServeHTTP(w, r)
	})
}

// ---- lifecycle ------------------------------------------------------------------------

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
	srv := &http.Server{
		Handler:           s,
		ReadHeaderTimeout: 5 * time.Second,
		ReadTimeout:       10 * time.Second,
		WriteTimeout:      15 * time.Second,
		IdleTimeout:       time.Minute,
		BaseContext:       func(net.Listener) context.Context { return context.WithoutCancel(ctx) },
	}
	served := make(chan error, 1)
	go func() { served <- srv.Serve(ln) }()

	select {
	case err := <-served:
		return fmt.Errorf("serve: %w", err)
	case <-ctx.Done():
	}
	s.draining.Store(true)
	s.cfg.Logger.Info("draining", "delay", opts.DrainDelay)
	time.Sleep(opts.DrainDelay)

	shutdownCtx, cancel := context.WithTimeout(context.Background(), opts.ShutdownTimeout)
	defer cancel()
	if err := srv.Shutdown(shutdownCtx); err != nil {
		srv.Close()
		<-served
		return fmt.Errorf("shutdown: %w", err)
	}
	if err := <-served; !errors.Is(err, http.ErrServerClosed) {
		return fmt.Errorf("serve: %w", err)
	}
	return nil
}
