// Package proxy is a reverse proxy with rate limiting, timeouts and routes.
package proxy

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"math"
	"net"
	"net/http"
	"net/http/httputil"
	"net/url"
	"strconv"
	"strings"
	"sync"
	"time"
)

// Route sends the requests under Prefix to Upstream.
type Route struct {
	Prefix      string
	Upstream    string
	StripPrefix bool
}

// Config describes the proxy.
type Config struct {
	Upstream      string
	RatePerSecond float64
	Burst         int
	Now           func() time.Time
	Timeout       time.Duration
	Routes        []Route
}

// problem writes an RFC 9457 problem document.
func problem(w http.ResponseWriter, status int, detail string) {
	w.Header().Set("Content-Type", "application/problem+json")
	w.WriteHeader(status)
	json.NewEncoder(w).Encode(map[string]any{
		"type":   "about:blank",
		"title":  http.StatusText(status),
		"status": status,
		"detail": detail,
	})
}

func parseUpstream(raw string) (*url.URL, error) {
	target, err := url.Parse(raw)
	if err != nil {
		return nil, err
	}
	if (target.Scheme != "http" && target.Scheme != "https") || target.Host == "" {
		return nil, fmt.Errorf("upstream %q is not an absolute http(s) URL", raw)
	}
	return target, nil
}

// forwarder proxies to one upstream, optionally removing a path prefix.
func forwarder(target *url.URL, strip string) *httputil.ReverseProxy {
	return &httputil.ReverseProxy{
		Rewrite: func(pr *httputil.ProxyRequest) {
			if strip != "" {
				pr.Out.URL.Path = "/" + strings.TrimPrefix(strings.TrimPrefix(pr.Out.URL.Path, strip), "/")
				pr.Out.URL.RawPath = ""
			}
			pr.SetURL(target)
			// Rewrite starts without the inbound X-Forwarded-For: keep the earlier hops.
			pr.Out.Header["X-Forwarded-For"] = pr.In.Header["X-Forwarded-For"]
			pr.SetXForwarded()
		},
		ErrorHandler: func(w http.ResponseWriter, r *http.Request, err error) {
			if errors.Is(err, context.DeadlineExceeded) {
				problem(w, http.StatusGatewayTimeout, "the upstream did not answer in time")
				return
			}
			problem(w, http.StatusBadGateway, "the upstream cannot be reached")
		},
	}
}

type bucket struct {
	tokens float64
	last   time.Time
}

// limiter holds one token bucket per client IP.
type limiter struct {
	mu      sync.Mutex
	rate    float64
	burst   float64
	now     func() time.Time
	buckets map[string]*bucket
}

// take spends a token of `client`, or says how long until one is back.
func (l *limiter) take(client string) (bool, time.Duration) {
	l.mu.Lock()
	defer l.mu.Unlock()
	now := l.now()
	b, ok := l.buckets[client]
	if !ok {
		b = &bucket{tokens: l.burst, last: now}
		l.buckets[client] = b
	}
	b.tokens = math.Min(l.burst, b.tokens+now.Sub(b.last).Seconds()*l.rate)
	b.last = now
	if b.tokens >= 1 {
		b.tokens--
		return true, 0
	}
	return false, time.Duration((1 - b.tokens) / l.rate * float64(time.Second))
}

func clientIP(r *http.Request) string {
	host, _, err := net.SplitHostPort(r.RemoteAddr)
	if err != nil {
		return r.RemoteAddr
	}
	return host
}

type route struct {
	prefix  string
	handler http.Handler
}

// matches says whether path is the prefix itself or below it.
func (r route) matches(path string) bool {
	return path == r.prefix || strings.HasPrefix(path, strings.TrimSuffix(r.prefix, "/")+"/")
}

// New builds the proxy's handler.
func New(cfg Config) (http.Handler, error) {
	var fallback http.Handler
	if cfg.Upstream == "" && len(cfg.Routes) == 0 {
		return nil, fmt.Errorf("no upstream and no routes")
	}
	if cfg.Upstream != "" {
		target, err := parseUpstream(cfg.Upstream)
		if err != nil {
			return nil, err
		}
		fallback = forwarder(target, "")
	}
	var routes []route
	for _, r := range cfg.Routes {
		target, err := parseUpstream(r.Upstream)
		if err != nil {
			return nil, fmt.Errorf("route %s: %w", r.Prefix, err)
		}
		strip := ""
		if r.StripPrefix {
			strip = strings.TrimSuffix(r.Prefix, "/")
		}
		routes = append(routes, route{r.Prefix, forwarder(target, strip)})
	}
	var limits *limiter
	if cfg.RatePerSecond > 0 {
		now := cfg.Now
		if now == nil {
			now = time.Now
		}
		limits = &limiter{rate: cfg.RatePerSecond, burst: float64(cfg.Burst), now: now, buckets: map[string]*bucket{}}
	}
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.Method == http.MethodGet && r.URL.Path == "/healthz" {
			w.Header().Set("Content-Type", "application/json")
			w.Write([]byte(`{"status":"ok"}`))
			return
		}
		if limits != nil {
			if ok, wait := limits.take(clientIP(r)); !ok {
				w.Header().Set("Retry-After", strconv.Itoa(int(math.Ceil(wait.Seconds()))))
				problem(w, http.StatusTooManyRequests, "slow down")
				return
			}
		}
		handler := fallback
		best := -1
		for _, candidate := range routes {
			if candidate.matches(r.URL.Path) && len(candidate.prefix) > best {
				handler, best = candidate.handler, len(candidate.prefix)
			}
		}
		if handler == nil {
			problem(w, http.StatusNotFound, "no route for "+r.URL.Path)
			return
		}
		if cfg.Timeout > 0 {
			ctx, cancel := context.WithTimeout(r.Context(), cfg.Timeout)
			defer cancel()
			r = r.WithContext(ctx)
		}
		handler.ServeHTTP(w, r)
	}), nil
}
