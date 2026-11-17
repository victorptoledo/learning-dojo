package proxy_test

import (
	"net/http"
	"net/http/httptest"
	"sync"
	"sync/atomic"
	"testing"
	"time"

	"dojo/proxy"
)

// clock is a time the test moves by hand.
type clock struct {
	mu  sync.Mutex
	now time.Time
}

func (c *clock) Now() time.Time {
	c.mu.Lock()
	defer c.mu.Unlock()
	return c.now
}

func (c *clock) advance(d time.Duration) {
	c.mu.Lock()
	defer c.mu.Unlock()
	c.now = c.now.Add(d)
}

func counting(t *testing.T, hits *atomic.Int64) *httptest.Server {
	return upstream(t, func(w http.ResponseWriter, r *http.Request) {
		hits.Add(1)
		w.WriteHeader(http.StatusOK)
	})
}

func limited(t *testing.T, server *httptest.Server, c *clock, rate float64, burst int) http.Handler {
	return build(t, proxy.Config{Upstream: server.URL, RatePerSecond: rate, Burst: burst, Now: c.Now})
}

func get(handler http.Handler, client string) *httptest.ResponseRecorder {
	return send(handler, httptest.NewRequest(http.MethodGet, "/", nil), client+":5000")
}

func TestABurstThenTooManyRequests(t *testing.T) {
	var hits atomic.Int64
	c := &clock{now: time.Unix(1_000_000, 0)}
	handler := limited(t, counting(t, &hits), c, 1, 2)
	for i := range 2 {
		if res := get(handler, "192.0.2.10"); res.Code != http.StatusOK {
			t.Fatalf("request %d: status %d, want 200", i+1, res.Code)
		}
	}
	res := get(handler, "192.0.2.10")
	expectProblem(t, res, http.StatusTooManyRequests)
	if got := res.Header().Get("Retry-After"); got != "1" {
		t.Errorf("Retry-After %q, want 1", got)
	}
	if hits.Load() != 2 {
		t.Errorf("the upstream got %d requests, want 2: a refused request must not reach it", hits.Load())
	}
}

func TestTheBucketRefillsOverTime(t *testing.T) {
	var hits atomic.Int64
	c := &clock{now: time.Unix(1_000_000, 0)}
	handler := limited(t, counting(t, &hits), c, 2, 1)
	get(handler, "192.0.2.20")
	if res := get(handler, "192.0.2.20"); res.Code != http.StatusTooManyRequests {
		t.Fatalf("status %d, want 429", res.Code)
	}
	c.advance(250 * time.Millisecond)
	if res := get(handler, "192.0.2.20"); res.Code != http.StatusTooManyRequests {
		t.Errorf("half a token is not a token: status %d", res.Code)
	}
	c.advance(250 * time.Millisecond)
	if res := get(handler, "192.0.2.20"); res.Code != http.StatusOK {
		t.Errorf("after half a second at 2/s a token is back: status %d", res.Code)
	}
	c.advance(time.Hour)
	for i := range 2 {
		want := http.StatusOK
		if i == 1 {
			want = http.StatusTooManyRequests
		}
		if res := get(handler, "192.0.2.20"); res.Code != want {
			t.Errorf("after an hour, request %d: status %d, want %d (the bucket holds Burst tokens at most)", i+1, res.Code, want)
		}
	}
}

func TestRetryAfterRoundsUp(t *testing.T) {
	var hits atomic.Int64
	c := &clock{now: time.Unix(1_000_000, 0)}
	handler := limited(t, counting(t, &hits), c, 0.25, 1)
	get(handler, "192.0.2.30")
	c.advance(time.Second)
	if got := get(handler, "192.0.2.30").Header().Get("Retry-After"); got != "3" {
		t.Errorf("Retry-After %q, want 3 (3 of the 4 seconds a token takes are left)", got)
	}
}

func TestClientsHaveTheirOwnBuckets(t *testing.T) {
	var hits atomic.Int64
	c := &clock{now: time.Unix(1_000_000, 0)}
	handler := limited(t, counting(t, &hits), c, 1, 1)
	get(handler, "192.0.2.40")
	if res := get(handler, "192.0.2.41"); res.Code != http.StatusOK {
		t.Errorf("another client was limited: status %d", res.Code)
	}
}

func TestAForgedForwardedForDoesNotEscapeTheLimit(t *testing.T) {
	var hits atomic.Int64
	c := &clock{now: time.Unix(1_000_000, 0)}
	handler := limited(t, counting(t, &hits), c, 1, 1)
	get(handler, "192.0.2.50")
	req := httptest.NewRequest(http.MethodGet, "/", nil)
	req.Header.Set("X-Forwarded-For", "10.9.8.7")
	if res := send(handler, req, "192.0.2.50:6000"); res.Code != http.StatusTooManyRequests {
		t.Errorf("status %d, want 429: the client is known by its connection", res.Code)
	}
}

func TestNoRateMeansNoLimit(t *testing.T) {
	var hits atomic.Int64
	handler := build(t, proxy.Config{Upstream: counting(t, &hits).URL})
	for range 20 {
		get(handler, "192.0.2.60")
	}
	if hits.Load() != 20 {
		t.Errorf("%d of 20 requests went through", hits.Load())
	}
}

func TestConcurrentClientsShareNoTokens(t *testing.T) {
	var hits atomic.Int64
	c := &clock{now: time.Unix(1_000_000, 0)}
	handler := limited(t, counting(t, &hits), c, 1, 5)
	var wg sync.WaitGroup
	for range 40 {
		wg.Go(func() { get(handler, "192.0.2.70") })
	}
	wg.Wait()
	if hits.Load() != 5 {
		t.Errorf("%d requests went through at once, want exactly the burst of 5", hits.Load())
	}
}
