package shortlinks_test

import (
	"context"
	"net/http"
	"net/http/httptest"
	"strings"
	"sync"
	"testing"

	"dojo/shortlinks"
)

// memStore is a Store in memory.
type memStore struct {
	mu    sync.Mutex
	links map[string]string
}

func (m *memStore) Save(_ context.Context, code, target string) error {
	m.mu.Lock()
	defer m.mu.Unlock()
	if _, ok := m.links[code]; ok {
		return shortlinks.ErrExists
	}
	if m.links == nil {
		m.links = map[string]string{}
	}
	m.links[code] = target
	return nil
}

func (m *memStore) Lookup(_ context.Context, code string) (string, error) {
	m.mu.Lock()
	defer m.mu.Unlock()
	target, ok := m.links[code]
	if !ok {
		return "", shortlinks.ErrNotFound
	}
	return target, nil
}

func TestCreateThenFollow(t *testing.T) {
	s := shortlinks.New(shortlinks.Config{NewCode: func() string { return "go27" }}, &memStore{})

	rec := httptest.NewRecorder()
	s.ServeHTTP(rec, httptest.NewRequest("POST", "/links", strings.NewReader(`{"url": "https://go.dev/doc/"}`)))
	if rec.Code != http.StatusCreated {
		t.Fatalf("POST /links = %d %s, want 201", rec.Code, rec.Body)
	}

	rec = httptest.NewRecorder()
	s.ServeHTTP(rec, httptest.NewRequest("GET", "/go27", nil))
	if rec.Code != http.StatusFound || rec.Header().Get("Location") != "https://go.dev/doc/" {
		t.Errorf("GET /go27 = %d, Location %q, want 302 to https://go.dev/doc/", rec.Code, rec.Header().Get("Location"))
	}
}
