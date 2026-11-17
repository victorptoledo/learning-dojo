package proxy_test

import (
	"io"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"

	"dojo/proxy"
)

type seen struct {
	method, path, query, body, header, forwarded string
}

func recording(t *testing.T, got *seen) *httptest.Server {
	return upstream(t, func(w http.ResponseWriter, r *http.Request) {
		body, _ := io.ReadAll(r.Body)
		*got = seen{r.Method, r.URL.Path, r.URL.RawQuery, string(body), r.Header.Get("X-Request-Id"), r.Header.Get("X-Forwarded-For")}
		w.Header().Set("X-Upstream", "yes")
		w.WriteHeader(http.StatusCreated)
		io.WriteString(w, "from upstream")
	})
}

func TestARequestGoesUpstreamUnchanged(t *testing.T) {
	var got seen
	server := recording(t, &got)
	handler := build(t, proxy.Config{Upstream: server.URL})
	req := httptest.NewRequest(http.MethodPost, "/orders/7?expand=items&x=1", strings.NewReader(`{"qty":2}`))
	req.Header.Set("X-Request-Id", "abc-123")
	res := send(handler, req, "203.0.113.7:4321")

	want := seen{"POST", "/orders/7", "expand=items&x=1", `{"qty":2}`, "abc-123", "203.0.113.7"}
	if got != want {
		t.Errorf("upstream saw %+v\nwant %+v", got, want)
	}
	if res.Code != http.StatusCreated || res.Body.String() != "from upstream" || res.Header().Get("X-Upstream") != "yes" {
		t.Errorf("client got %d %q headers %v", res.Code, res.Body.String(), res.Header())
	}
}

func TestForwardedForKeepsEarlierHops(t *testing.T) {
	var got seen
	server := recording(t, &got)
	handler := build(t, proxy.Config{Upstream: server.URL})
	req := httptest.NewRequest(http.MethodGet, "/", nil)
	req.Header.Set("X-Forwarded-For", "198.51.100.1")
	send(handler, req, "203.0.113.9:1000")
	if got.forwarded != "198.51.100.1, 203.0.113.9" {
		t.Errorf("X-Forwarded-For %q, want %q", got.forwarded, "198.51.100.1, 203.0.113.9")
	}
}

func TestUpstreamErrorsPassThrough(t *testing.T) {
	server := upstream(t, func(w http.ResponseWriter, r *http.Request) {
		http.Error(w, "no such order", http.StatusNotFound)
	})
	res := send(build(t, proxy.Config{Upstream: server.URL}), httptest.NewRequest(http.MethodGet, "/orders/9", nil), "")
	if res.Code != http.StatusNotFound || !strings.Contains(res.Body.String(), "no such order") {
		t.Errorf("got %d %q, want the upstream's 404", res.Code, res.Body.String())
	}
}

func TestAnUnreachableUpstreamIsBadGateway(t *testing.T) {
	server := httptest.NewServer(http.NotFoundHandler())
	address := server.URL
	server.Close()
	res := send(build(t, proxy.Config{Upstream: address}), httptest.NewRequest(http.MethodGet, "/", nil), "")
	expectProblem(t, res, http.StatusBadGateway)
}

func TestABadUpstreamIsRefused(t *testing.T) {
	for _, upstream := range []string{"", "not a url", "ftp://files.example.com", "/relative/path"} {
		if _, err := proxy.New(proxy.Config{Upstream: upstream}); err == nil {
			t.Errorf("proxy.New accepted the upstream %q", upstream)
		}
	}
}
