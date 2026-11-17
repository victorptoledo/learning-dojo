package proxy_test

import (
	"context"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"

	"dojo/proxy"
)

func TestASlowUpstreamTimesOut(t *testing.T) {
	release := make(chan struct{})
	server := upstream(t, func(w http.ResponseWriter, r *http.Request) {
		select {
		case <-release:
		case <-r.Context().Done():
		}
	})
	defer close(release)
	handler := build(t, proxy.Config{Upstream: server.URL, Timeout: 100 * time.Millisecond})
	started := time.Now()
	res := send(handler, httptest.NewRequest(http.MethodGet, "/slow", nil), "")
	if elapsed := time.Since(started); elapsed > 2*time.Second {
		t.Errorf("the client waited %v", elapsed)
	}
	expectProblem(t, res, http.StatusGatewayTimeout)
}

func TestAFastUpstreamIsNotCut(t *testing.T) {
	server := upstream(t, func(w http.ResponseWriter, r *http.Request) {
		time.Sleep(20 * time.Millisecond)
		w.Write([]byte("in time"))
	})
	handler := build(t, proxy.Config{Upstream: server.URL, Timeout: 2 * time.Second})
	res := send(handler, httptest.NewRequest(http.MethodGet, "/", nil), "")
	if res.Code != http.StatusOK || res.Body.String() != "in time" {
		t.Errorf("got %d %q", res.Code, res.Body.String())
	}
}

func TestAClientThatLeavesCancelsTheUpstreamRequest(t *testing.T) {
	started, cancelled := make(chan struct{}), make(chan struct{})
	server := upstream(t, func(w http.ResponseWriter, r *http.Request) {
		close(started)
		select {
		case <-r.Context().Done():
			close(cancelled)
		case <-time.After(5 * time.Second):
		}
	})
	handler := build(t, proxy.Config{Upstream: server.URL})
	ctx, cancel := context.WithCancel(context.Background())
	req := httptest.NewRequest(http.MethodGet, "/long", nil).WithContext(ctx)
	done := make(chan struct{})
	go func() {
		send(handler, req, "")
		close(done)
	}()
	<-started
	cancel()
	select {
	case <-cancelled:
	case <-time.After(2 * time.Second):
		t.Fatal("the upstream never saw the cancellation")
	}
	<-done
}
