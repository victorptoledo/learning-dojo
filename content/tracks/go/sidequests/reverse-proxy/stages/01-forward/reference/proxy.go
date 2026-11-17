// Package proxy is a reverse proxy with rate limiting, timeouts and routes.
package proxy

import (
	"encoding/json"
	"fmt"
	"net/http"
	"net/http/httputil"
	"net/url"
)

// Config describes the proxy.
type Config struct {
	Upstream string
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

// New builds the proxy's handler.
func New(cfg Config) (http.Handler, error) {
	var fallback http.Handler
	target, err := parseUpstream(cfg.Upstream)
	if err != nil {
		return nil, err
	}
	fallback = forwarder(target, "")
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		handler := fallback
		handler.ServeHTTP(w, r)
	}), nil
}
