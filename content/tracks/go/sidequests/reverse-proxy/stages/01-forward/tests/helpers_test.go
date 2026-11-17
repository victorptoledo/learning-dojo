package proxy_test

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"

	"dojo/proxy"
)

// upstream starts a fake service; it is closed when the test ends.
func upstream(t *testing.T, handler http.HandlerFunc) *httptest.Server {
	t.Helper()
	server := httptest.NewServer(handler)
	t.Cleanup(server.Close)
	return server
}

func build(t *testing.T, cfg proxy.Config) http.Handler {
	t.Helper()
	handler, err := proxy.New(cfg)
	if err != nil {
		t.Fatalf("proxy.New(%+v): %v", cfg, err)
	}
	return handler
}

// send runs one request through the proxy, from the client at remoteAddr.
func send(handler http.Handler, req *http.Request, remoteAddr string) *httptest.ResponseRecorder {
	if remoteAddr != "" {
		req.RemoteAddr = remoteAddr
	}
	recorder := httptest.NewRecorder()
	handler.ServeHTTP(recorder, req)
	return recorder
}

// expectProblem checks a problem details answer with `status`.
func expectProblem(t *testing.T, recorder *httptest.ResponseRecorder, status int) {
	t.Helper()
	if recorder.Code != status {
		t.Fatalf("status %d, want %d (body %q)", recorder.Code, status, recorder.Body.String())
	}
	if got := recorder.Header().Get("Content-Type"); got != "application/problem+json" {
		t.Errorf("Content-Type %q, want application/problem+json", got)
	}
	var body struct {
		Type   string `json:"type"`
		Title  string `json:"title"`
		Status int    `json:"status"`
	}
	if err := json.Unmarshal(recorder.Body.Bytes(), &body); err != nil {
		t.Fatalf("the body is not JSON: %v (%q)", err, recorder.Body.String())
	}
	if body.Type != "about:blank" || body.Title != http.StatusText(status) || body.Status != status {
		t.Errorf("problem %+v, want type about:blank, title %q, status %d", body, http.StatusText(status), status)
	}
}
