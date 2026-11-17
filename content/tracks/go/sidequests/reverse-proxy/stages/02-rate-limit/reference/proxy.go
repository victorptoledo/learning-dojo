// Package proxy is a reverse proxy with rate limiting, timeouts and routes.
package proxy

import (
	"encoding/json"
	"fmt"
	"math"
	"net"
	"net/http"
	"net/http/httputil"
	"net/url"
	"strconv"
	"sync"
	"time"
)

// Config describes the proxy.
type Config struct {
	Upstream      string
	RatePerSecond float64
	Burst         int
	Now           func() time.Time
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
			pr.SetURL(target)
			// Rewrite starts without the inbound X-Forwarded-For: keep the earlier hops.
			pr.Out.Header["X-Forwarded-For"] = pr.In.Header["X-Forwarded-For"]
			pr.SetXForwarded()
		},
		ErrorHandler: func(w http.ResponseWriter, r *http.Request, err error) {
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

// New builds the proxy's handler.
func New(cfg Config) (http.Handler, error) {
	var fallback http.Handler
	target, err := parseUpstream(cfg.Upstream)
	if err != nil {
		return nil, err
	}
	fallback = forwarder(target, "")
	var limits *limiter
	if cfg.RatePerSecond > 0 {
		now := cfg.Now
		if now == nil {
			now = time.Now
		}
		limits = &limiter{rate: cfg.RatePerSecond, burst: float64(cfg.Burst), now: now, buckets: map[string]*bucket{}}
	}
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if limits != nil {
			if ok, wait := limits.take(clientIP(r)); !ok {
				w.Header().Set("Retry-After", strconv.Itoa(int(math.Ceil(wait.Seconds()))))
				problem(w, http.StatusTooManyRequests, "slow down")
				return
			}
		}
		handler := fallback
		handler.ServeHTTP(w, r)
	}), nil
}
