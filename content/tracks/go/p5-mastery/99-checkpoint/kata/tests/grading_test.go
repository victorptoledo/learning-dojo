package shortlinks_test

import (
	"bufio"
	"bytes"
	"context"
	"encoding/json"
	"errors"
	"io"
	"log/slog"
	"net"
	"net/http"
	"net/http/httptest"
	"strings"
	"sync"
	"testing"
	"time"

	"dojo/shortlinks"
)

// logBuffer collects JSON log records, safe for concurrent handlers.
type logBuffer struct {
	mu  sync.Mutex
	buf bytes.Buffer
}

func (l *logBuffer) Write(p []byte) (int, error) {
	l.mu.Lock()
	defer l.mu.Unlock()
	return l.buf.Write(p)
}

func (l *logBuffer) records(t *testing.T) []map[string]any {
	t.Helper()
	l.mu.Lock()
	defer l.mu.Unlock()
	var out []map[string]any
	sc := bufio.NewScanner(bytes.NewReader(l.buf.Bytes()))
	for sc.Scan() {
		var rec map[string]any
		if err := json.Unmarshal(sc.Bytes(), &rec); err != nil {
			t.Fatalf("log line %q is not JSON: %v", sc.Text(), err)
		}
		out = append(out, rec)
	}
	return out
}

func (l *logBuffer) raw() string {
	l.mu.Lock()
	defer l.mu.Unlock()
	return l.buf.String()
}

func (l *logBuffer) find(t *testing.T, msg string) []map[string]any {
	t.Helper()
	var out []map[string]any
	for _, rec := range l.records(t) {
		if rec["msg"] == msg {
			out = append(out, rec)
		}
	}
	return out
}

type fixture struct {
	srv   *shortlinks.Server
	store *memStore
	logs  *logBuffer
}

func newFixture(t *testing.T, cfg shortlinks.Config, store shortlinks.Store) fixture {
	t.Helper()
	logs := &logBuffer{}
	if cfg.NewCode == nil {
		codes := []string{"c1", "c2", "c3", "c4", "c5", "c6"}
		var mu sync.Mutex
		cfg.NewCode = func() string {
			mu.Lock()
			defer mu.Unlock()
			code := codes[0]
			codes = codes[1:]
			return code
		}
	}
	if cfg.Logger == nil {
		cfg.Logger = slog.New(slog.NewJSONHandler(logs, &slog.HandlerOptions{Level: slog.LevelDebug}))
	}
	ms, _ := store.(*memStore)
	if store == nil {
		ms = &memStore{}
		store = ms
	}
	return fixture{srv: shortlinks.New(cfg, store), store: ms, logs: logs}
}

func (f fixture) do(method, target, body string, header ...string) *httptest.ResponseRecorder {
	var r io.Reader
	if body != "" {
		r = strings.NewReader(body)
	}
	req := httptest.NewRequest(method, target, r)
	for i := 0; i+1 < len(header); i += 2 {
		req.Header.Set(header[i], header[i+1])
	}
	rec := httptest.NewRecorder()
	f.srv.ServeHTTP(rec, req)
	return rec
}

func jsonError(t *testing.T, rec *httptest.ResponseRecorder) string {
	t.Helper()
	if ct := rec.Header().Get("Content-Type"); ct != "application/json" {
		t.Errorf("Content-Type = %q, want application/json", ct)
	}
	var body struct {
		Error string `json:"error"`
	}
	if err := json.Unmarshal(rec.Body.Bytes(), &body); err != nil || body.Error == "" {
		t.Errorf("body %q is not {\"error\": \"...\"}", rec.Body)
	}
	return body.Error
}

func TestCreateAnswers201WithLocationAndJSON(t *testing.T) {
	f := newFixture(t, shortlinks.Config{}, nil)
	rec := f.do("POST", "/links", `{"url": "https://go.dev/blog/"}`)
	if rec.Code != http.StatusCreated {
		t.Fatalf("POST /links = %d %s, want 201", rec.Code, rec.Body)
	}
	if loc := rec.Header().Get("Location"); loc != "/c1" {
		t.Errorf("Location = %q, want /c1", loc)
	}
	var body map[string]string
	if err := json.Unmarshal(rec.Body.Bytes(), &body); err != nil || body["code"] != "c1" || body["url"] != "https://go.dev/blog/" {
		t.Errorf("body = %s, want {\"code\":\"c1\",\"url\":\"https://go.dev/blog/\"}", rec.Body)
	}
	if ct := rec.Header().Get("Content-Type"); ct != "application/json" {
		t.Errorf("Content-Type = %q, want application/json", ct)
	}
}

func TestCreateRejectsBadInput(t *testing.T) {
	f := newFixture(t, shortlinks.Config{}, nil)
	for _, body := range []string{
		``,
		`not json`,
		`{"url": "https://go.dev", "admin": true}`,
		`{"url": 42}`,
		`{"url": "ftp://files.example.com/x"}`,
		`{"url": "/relative/path"}`,
		`{"url": "https://"}`,
		`{"url": "javascript:alert(1)"}`,
	} {
		rec := f.do("POST", "/links", body)
		if rec.Code != http.StatusBadRequest {
			t.Errorf("POST /links %s = %d, want 400", body, rec.Code)
			continue
		}
		jsonError(t, rec)
	}
	if len(f.store.links) != 0 {
		t.Errorf("store has %v, want nothing saved from bad input", f.store.links)
	}
}

func TestBodyLimit(t *testing.T) {
	f := newFixture(t, shortlinks.Config{}, nil)
	long := `{"url": "https://example.com/` + strings.Repeat("a", 2000) + `"}`
	rec := f.do("POST", "/links", long)
	if rec.Code != http.StatusRequestEntityTooLarge {
		t.Errorf("POST /links with a %d-byte body = %d, want 413 (default limit %d)", len(long), rec.Code, shortlinks.DefaultMaxBody)
	} else {
		jsonError(t, rec)
	}
	fits := `{"url": "https://example.com/` + strings.Repeat("a", 900) + `"}`
	if rec := f.do("POST", "/links", fits); rec.Code != http.StatusCreated {
		t.Errorf("POST /links with a %d-byte body = %d, want 201", len(fits), rec.Code)
	}
	small := newFixture(t, shortlinks.Config{MaxBody: 40}, nil)
	if rec := small.do("POST", "/links", `{"url": "https://example.com/abcdefghij"}`); rec.Code != http.StatusRequestEntityTooLarge {
		t.Errorf("with MaxBody 40, a 43-byte body = %d, want 413", rec.Code)
	}
}

func TestCodeCollisionsAreRetried(t *testing.T) {
	store := &memStore{links: map[string]string{"c1": "https://taken.example", "c2": "https://taken.example"}}
	f := newFixture(t, shortlinks.Config{}, store)
	rec := f.do("POST", "/links", `{"url": "https://go.dev"}`)
	if rec.Code != http.StatusCreated || rec.Header().Get("Location") != "/c3" {
		t.Fatalf("POST /links with c1 and c2 taken = %d, Location %q, want 201 at /c3", rec.Code, rec.Header().Get("Location"))
	}

	calls := 0
	always := newFixture(t, shortlinks.Config{NewCode: func() string { calls++; return "c1" }}, store)
	rec = always.do("POST", "/links", `{"url": "https://go.dev"}`)
	if rec.Code != http.StatusInternalServerError {
		t.Errorf("POST /links when every code is taken = %d, want 500", rec.Code)
	}
	if calls != 3 {
		t.Errorf("NewCode called %d times, want 3 attempts", calls)
	}
}

func TestFollowAndRoutes(t *testing.T) {
	f := newFixture(t, shortlinks.Config{}, &memStore{links: map[string]string{"go": "https://go.dev"}})
	if rec := f.do("GET", "/go", ""); rec.Code != http.StatusFound || rec.Header().Get("Location") != "https://go.dev" {
		t.Errorf("GET /go = %d, Location %q, want 302 to https://go.dev", rec.Code, rec.Header().Get("Location"))
	}
	rec := f.do("GET", "/nope", "")
	if rec.Code != http.StatusNotFound {
		t.Errorf("GET /nope = %d, want 404", rec.Code)
	} else {
		jsonError(t, rec)
	}
	if rec := f.do("DELETE", "/go", ""); rec.Code != http.StatusMethodNotAllowed {
		t.Errorf("DELETE /go = %d, want 405 from the mux's method patterns", rec.Code)
	}
	if rec := f.do("GET", "/healthz", ""); rec.Code != http.StatusOK {
		t.Errorf("GET /healthz = %d, want 200", rec.Code)
	}
	if rec := f.do("GET", "/readyz", ""); rec.Code != http.StatusOK {
		t.Errorf("GET /readyz = %d, want 200 while not draining", rec.Code)
	}
}

// slowStore blocks every call until its context ends, and remembers the deadline it got.
type slowStore struct {
	mu       sync.Mutex
	deadline time.Duration
}

func (s *slowStore) wait(ctx context.Context) error {
	if d, ok := ctx.Deadline(); ok {
		s.mu.Lock()
		s.deadline = time.Until(d)
		s.mu.Unlock()
	}
	<-ctx.Done()
	return ctx.Err()
}

func (s *slowStore) Save(ctx context.Context, _, _ string) error { return s.wait(ctx) }
func (s *slowStore) Lookup(ctx context.Context, _ string) (string, error) {
	return "", s.wait(ctx)
}

func TestStoreCallsHaveATimeout(t *testing.T) {
	store := &slowStore{}
	f := newFixture(t, shortlinks.Config{StoreTimeout: 50 * time.Millisecond}, store)
	start := time.Now()
	rec := f.do("GET", "/go", "")
	if elapsed := time.Since(start); elapsed > time.Second {
		t.Fatalf("GET took %v with a 50ms store timeout", elapsed)
	}
	if rec.Code != http.StatusGatewayTimeout {
		t.Errorf("GET with a store that times out = %d, want 504", rec.Code)
	} else {
		jsonError(t, rec)
	}
	if store.deadline <= 0 || store.deadline > 50*time.Millisecond {
		t.Errorf("the store got a deadline %v away, want at most 50ms", store.deadline)
	}

	// The store call must also end when the client goes away.
	ctx, cancel := context.WithCancel(context.Background())
	req := httptest.NewRequestWithContext(ctx, "GET", "/go", nil)
	done := make(chan struct{})
	go func() {
		defer close(done)
		f.srv.ServeHTTP(httptest.NewRecorder(), req)
	}()
	time.Sleep(10 * time.Millisecond)
	cancel()
	select {
	case <-done:
	case <-time.After(30 * time.Millisecond):
		t.Error("the handler kept waiting after the request context was cancelled: derive the store context from r.Context()")
		<-done
	}
}

type brokenStore struct{}

func (brokenStore) Save(context.Context, string, string) error {
	return errors.New("pq: password=hunter2 rejected")
}
func (brokenStore) Lookup(context.Context, string) (string, error) {
	return "", errors.New("pq: password=hunter2 rejected")
}

func TestStoreErrorsAreLoggedNotLeaked(t *testing.T) {
	f := newFixture(t, shortlinks.Config{}, brokenStore{})
	rec := f.do("GET", "/go", "", "X-Request-ID", "abc-123")
	if rec.Code != http.StatusInternalServerError {
		t.Fatalf("GET with a broken store = %d, want 500", rec.Code)
	}
	if msg := jsonError(t, rec); strings.Contains(msg, "hunter2") || strings.Contains(rec.Body.String(), "pq:") {
		t.Errorf("body %q leaks the store error to the client", rec.Body)
	}
	var found bool
	for _, r := range f.logs.records(t) {
		if msg, _ := r["error"].(string); r["level"] == "ERROR" && strings.Contains(msg, "pq:") && r["request_id"] == "abc-123" {
			found = true
		}
	}
	if !found {
		t.Errorf("logs = %s, want an ERROR record with the store error in \"error\" and request_id abc-123", f.logs.raw())
	}
}

func TestRequestIDs(t *testing.T) {
	n := 0
	f := newFixture(t, shortlinks.Config{NewRequestID: func() string { n++; return "gen-" + string(rune('0'+n)) }}, nil)
	if got := f.do("GET", "/healthz", "", "X-Request-ID", "from-lb-7").Header().Get("X-Request-ID"); got != "from-lb-7" {
		t.Errorf("X-Request-ID = %q, want the incoming from-lb-7", got)
	}
	if got := f.do("GET", "/healthz", "").Header().Get("X-Request-ID"); got != "gen-1" {
		t.Errorf("X-Request-ID without one = %q, want a generated gen-1", got)
	}
	for _, bad := range []string{"has space", strings.Repeat("x", 65), "a\"b", "<script>"} {
		if got := f.do("GET", "/healthz", "", "X-Request-ID", bad).Header().Get("X-Request-ID"); got == bad || !strings.HasPrefix(got, "gen-") {
			t.Errorf("X-Request-ID %q came back as %q, want it replaced by a generated one (only 1 to 64 letters, digits and -)", bad, got)
		}
	}
	plain := newFixture(t, shortlinks.Config{}, nil)
	first := plain.do("GET", "/healthz", "").Header().Get("X-Request-ID")
	second := plain.do("GET", "/healthz", "").Header().Get("X-Request-ID")
	if first != "req-1" || second != "req-2" {
		t.Errorf("default request IDs = %q, %q, want req-1, req-2", first, second)
	}
}

func TestAccessLog(t *testing.T) {
	f := newFixture(t, shortlinks.Config{}, &memStore{links: map[string]string{"go": "https://go.dev"}})
	f.do("GET", "/go?token=s3cr3t", "", "X-Request-ID", "r-1")
	f.do("GET", "/missing", "")
	recs := f.logs.find(t, "request")
	if len(recs) != 2 {
		t.Fatalf("got %d \"request\" log records, want one per request: %s", len(recs), f.logs.raw())
	}
	r := recs[0]
	want := map[string]any{"level": "INFO", "request_id": "r-1", "method": "GET", "pattern": "GET /{code}", "path": "/go", "status": float64(302)}
	for k, v := range want {
		if r[k] != v {
			t.Errorf("access log %q = %v, want %v", k, r[k], v)
		}
	}
	if _, ok := r["duration"]; !ok {
		t.Errorf("access log has no duration: %v", r)
	}
	if recs[1]["status"] != float64(404) {
		t.Errorf("second access log status = %v, want 404", recs[1]["status"])
	}
	if strings.Contains(f.logs.raw(), "s3cr3t") {
		t.Errorf("logs contain the query string token: log the path, never the query")
	}
}

type panicStore struct{ memStore }

func (p *panicStore) Lookup(ctx context.Context, code string) (string, error) {
	if code == "boom" {
		panic("index out of range")
	}
	return p.memStore.Lookup(ctx, code)
}

func TestPanicsBecome500(t *testing.T) {
	store := &panicStore{memStore{links: map[string]string{"go": "https://go.dev"}}}
	f := newFixture(t, shortlinks.Config{}, store)
	rec := f.do("GET", "/boom", "", "X-Request-ID", "p-1")
	if rec.Code != http.StatusInternalServerError {
		t.Fatalf("GET /boom = %d, want 500 after a panic", rec.Code)
	}
	jsonError(t, rec)
	panics := f.logs.find(t, "panic")
	if len(panics) != 1 || panics[0]["level"] != "ERROR" || panics[0]["request_id"] != "p-1" {
		t.Errorf("panic log records = %v, want one ERROR \"panic\" record with request_id p-1", panics)
	}
	access := f.logs.find(t, "request")
	if len(access) != 1 || access[0]["status"] != float64(500) || access[0]["level"] != "ERROR" {
		t.Errorf("access log = %v, want status 500 at level ERROR: recover inside the access log, so it sees the 500", access)
	}
	if rec := f.do("GET", "/go", ""); rec.Code != http.StatusFound {
		t.Errorf("GET /go after a panic = %d, want 302", rec.Code)
	}
}

// gateStore holds Lookup until release is closed.
type gateStore struct {
	started chan struct{}
	release chan struct{}
}

func (g *gateStore) Save(context.Context, string, string) error { return nil }
func (g *gateStore) Lookup(ctx context.Context, _ string) (string, error) {
	close(g.started)
	select {
	case <-g.release:
		return "https://go.dev", nil
	case <-ctx.Done():
		return "", ctx.Err()
	}
}

func listen(t *testing.T) (net.Listener, string) {
	t.Helper()
	ln, err := net.Listen("tcp", "127.0.0.1:0")
	if err != nil {
		t.Fatal(err)
	}
	return ln, "http://" + ln.Addr().String()
}

var client = &http.Client{
	Timeout:       5 * time.Second,
	CheckRedirect: func(*http.Request, []*http.Request) error { return http.ErrUseLastResponse },
}

func get(url string) (int, error) {
	resp, err := client.Get(url)
	if err != nil {
		return 0, err
	}
	resp.Body.Close()
	return resp.StatusCode, nil
}

func TestRunDrainsThenShutsDownGracefully(t *testing.T) {
	store := &gateStore{started: make(chan struct{}), release: make(chan struct{})}
	f := newFixture(t, shortlinks.Config{StoreTimeout: 5 * time.Second}, store)
	ln, base := listen(t)
	ctx, cancel := context.WithCancel(context.Background())
	ran := make(chan error, 1)
	go func() {
		ran <- f.srv.Run(ctx, ln, shortlinks.RunOptions{DrainDelay: time.Second, ShutdownTimeout: 5 * time.Second})
	}()

	if code, err := get(base + "/readyz"); err != nil || code != 200 {
		t.Fatalf("GET /readyz before shutdown = %d, %v, want 200", code, err)
	}
	inFlight := make(chan int, 1)
	go func() {
		code, _ := get(base + "/slow")
		inFlight <- code
	}()
	<-store.started
	cancel() // SIGTERM
	signalled := time.Now()

	draining := false
	for time.Since(signalled) < 900*time.Millisecond {
		code, err := get(base + "/readyz")
		if err == nil && code == http.StatusServiceUnavailable {
			draining = true
			break
		}
		time.Sleep(10 * time.Millisecond)
	}
	if !draining {
		t.Errorf("GET /readyz never answered 503 during the drain: mark the server as draining as soon as ctx ends")
	}
	if time.Since(signalled) < 900*time.Millisecond {
		if code, err := get(base + "/healthz"); err != nil || code != http.StatusOK {
			t.Errorf("GET /healthz while draining = %d, %v, want 200: the server still serves during the drain", code, err)
		}
	}
	for deadline := time.Now().Add(4 * time.Second); time.Now().Before(deadline); time.Sleep(20 * time.Millisecond) {
		if _, err := get(base + "/healthz"); err != nil {
			break // the drain is over and Shutdown closed the listener; /slow is still running
		}
	}
	if elapsed := time.Since(signalled); elapsed < 900*time.Millisecond {
		t.Errorf("the listener closed %v after the signal, want it open for the 1s DrainDelay", elapsed)
	}
	close(store.release)
	if code := <-inFlight; code != http.StatusFound {
		t.Errorf("the in-flight request got %d, want 302: Shutdown must let it finish, and cancelling Run's context must not cancel requests", code)
	}
	select {
	case err := <-ran:
		if err != nil {
			t.Errorf("Run = %v, want nil after a clean shutdown", err)
		}
	case <-time.After(3 * time.Second):
		t.Fatal("Run did not return after shutdown")
	}
	if _, err := get(base + "/healthz"); err == nil {
		t.Error("the listener still accepts connections after Run returned")
	}
}

func TestRunReportsAShutdownTimeout(t *testing.T) {
	store := &gateStore{started: make(chan struct{}), release: make(chan struct{})}
	defer close(store.release)
	f := newFixture(t, shortlinks.Config{StoreTimeout: 5 * time.Second}, store)
	ln, base := listen(t)
	ctx, cancel := context.WithCancel(context.Background())
	ran := make(chan error, 1)
	go func() { ran <- f.srv.Run(ctx, ln, shortlinks.RunOptions{ShutdownTimeout: 100 * time.Millisecond}) }()
	go get(base + "/stuck")
	<-store.started
	cancel()
	select {
	case err := <-ran:
		if !errors.Is(err, context.DeadlineExceeded) {
			t.Errorf("Run with a request stuck past the shutdown timeout = %v, want it to wrap context.DeadlineExceeded", err)
		}
	case <-time.After(2 * time.Second):
		t.Fatal("Run did not return after the shutdown timeout")
	}
}

func TestRunReturnsServeErrors(t *testing.T) {
	f := newFixture(t, shortlinks.Config{}, nil)
	ln, _ := listen(t)
	ln.Close()
	done := make(chan error, 1)
	go func() { done <- f.srv.Run(context.Background(), ln, shortlinks.RunOptions{}) }()
	select {
	case err := <-done:
		if err == nil || !errors.Is(err, net.ErrClosed) {
			t.Errorf("Run on a closed listener = %v, want the Serve error (net.ErrClosed)", err)
		}
	case <-time.After(2 * time.Second):
		t.Fatal("Run on a closed listener did not return: return Serve's error instead of waiting for ctx")
	}
}
