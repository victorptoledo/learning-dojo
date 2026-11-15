package api_test

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"log/slog"
	"net/http"
	"net/http/httptest"
	"slices"
	"strings"
	"sync"
	"testing"

	"dojo/api"
)

type client struct {
	t       *testing.T
	handler http.Handler
	logs    *bytes.Buffer
}

func newClient(t *testing.T) *client {
	t.Helper()
	logs := &bytes.Buffer{}
	logger := slog.New(slog.NewTextHandler(logs, nil))
	return &client{t: t, handler: api.NewHandler(&api.Store{}, logger), logs: logs}
}

func (c *client) do(method, path, body string) *httptest.ResponseRecorder {
	c.t.Helper()
	var reader io.Reader
	if body != "" {
		reader = strings.NewReader(body)
	}
	rec := httptest.NewRecorder()
	c.handler.ServeHTTP(rec, httptest.NewRequest(method, path, reader))
	return rec
}

func decode[T any](t *testing.T, rec *httptest.ResponseRecorder) T {
	t.Helper()
	var v T
	if err := json.Unmarshal(rec.Body.Bytes(), &v); err != nil {
		t.Fatalf("response body %q is not the expected JSON: %v", rec.Body.String(), err)
	}
	return v
}

func expect(t *testing.T, rec *httptest.ResponseRecorder, call string, status int) {
	t.Helper()
	if rec.Code != status {
		t.Fatalf("%s = %d %s, want %d", call, rec.Code, strings.TrimSpace(rec.Body.String()), status)
	}
}

func expectJSONError(t *testing.T, rec *httptest.ResponseRecorder, call string, status int) {
	t.Helper()
	expect(t, rec, call, status)
	if ct := rec.Header().Get("Content-Type"); ct != "application/json" {
		t.Errorf("%s Content-Type = %q, want application/json", call, ct)
	}
	if body := decode[map[string]string](t, rec); body["error"] == "" {
		t.Errorf("%s body = %q, want {\"error\": \"...\"}", call, rec.Body.String())
	}
}

func TestCreateReturns201WithLocationAndTheTodo(t *testing.T) {
	c := newClient(t)
	rec := c.do(http.MethodPost, "/todos", `{"title": "  learn ServeMux  "}`)
	expect(t, rec, "POST /todos", http.StatusCreated)
	if loc := rec.Header().Get("Location"); loc != "/todos/1" {
		t.Errorf("Location = %q, want /todos/1", loc)
	}
	if ct := rec.Header().Get("Content-Type"); ct != "application/json" {
		t.Errorf("Content-Type = %q, want application/json", ct)
	}
	if got := decode[api.Todo](t, rec); got != (api.Todo{ID: 1, Title: "learn ServeMux"}) {
		t.Errorf("created todo = %+v, want {ID:1 Title:learn ServeMux Done:false}", got)
	}
	second := decode[api.Todo](t, c.do(http.MethodPost, "/todos", `{"title": "write tests"}`))
	if second.ID != 2 {
		t.Errorf("second todo id = %d, want 2", second.ID)
	}
}

func TestCreateRejectsBadBodies(t *testing.T) {
	c := newClient(t)
	for _, body := range []string{`{"title": "   "}`, `{}`, `not json`, `{"title": 42}`} {
		expectJSONError(t, c.do(http.MethodPost, "/todos", body), "POST /todos "+body, http.StatusBadRequest)
	}
}

func TestListIsSortedByIDAndAnEmptyArrayAtFirst(t *testing.T) {
	c := newClient(t)
	empty := c.do(http.MethodGet, "/todos", "")
	expect(t, empty, "GET /todos", http.StatusOK)
	if strings.TrimSpace(empty.Body.String()) != "[]" {
		t.Errorf("GET /todos on an empty store = %q, want []", empty.Body.String())
	}
	for _, title := range []string{"a", "b", "c"} {
		c.do(http.MethodPost, "/todos", fmt.Sprintf(`{"title": %q}`, title))
	}
	list := decode[[]api.Todo](t, c.do(http.MethodGet, "/todos", ""))
	var titles []string
	for _, todo := range list {
		titles = append(titles, todo.Title)
	}
	if !slices.Equal(titles, []string{"a", "b", "c"}) {
		t.Errorf("GET /todos titles = %v, want [a b c]", titles)
	}
}

func TestGetOneTodo(t *testing.T) {
	c := newClient(t)
	c.do(http.MethodPost, "/todos", `{"title": "first"}`)
	rec := c.do(http.MethodGet, "/todos/1", "")
	expect(t, rec, "GET /todos/1", http.StatusOK)
	if got := decode[api.Todo](t, rec); got.Title != "first" {
		t.Errorf("GET /todos/1 = %+v, want the todo titled first", got)
	}
}

func TestUnknownAndInvalidIDs(t *testing.T) {
	c := newClient(t)
	for _, method := range []string{http.MethodGet, http.MethodPatch, http.MethodDelete} {
		body := ""
		if method == http.MethodPatch {
			body = `{"done": true}`
		}
		expectJSONError(t, c.do(method, "/todos/99", body), method+" /todos/99", http.StatusNotFound)
		expectJSONError(t, c.do(method, "/todos/abc", body), method+" /todos/abc", http.StatusBadRequest)
		expectJSONError(t, c.do(method, "/todos/0", body), method+" /todos/0", http.StatusBadRequest)
	}
}

func TestPatchChangesOnlyTheFieldsSent(t *testing.T) {
	c := newClient(t)
	c.do(http.MethodPost, "/todos", `{"title": "draft"}`)
	done := decode[api.Todo](t, c.do(http.MethodPatch, "/todos/1", `{"done": true}`))
	if done != (api.Todo{ID: 1, Title: "draft", Done: true}) {
		t.Errorf("after PATCH {done: true}: %+v, want title draft kept and done true", done)
	}
	renamed := decode[api.Todo](t, c.do(http.MethodPatch, "/todos/1", `{"title": "final"}`))
	if renamed != (api.Todo{ID: 1, Title: "final", Done: true}) {
		t.Errorf("after PATCH {title: final}: %+v, want done kept true", renamed)
	}
	expectJSONError(t, c.do(http.MethodPatch, "/todos/1", `{"title": ""}`), "PATCH with an empty title", http.StatusBadRequest)
	expectJSONError(t, c.do(http.MethodPatch, "/todos/1", `nope`), "PATCH with bad JSON", http.StatusBadRequest)
}

func TestDeleteAnswers204AndRemoves(t *testing.T) {
	c := newClient(t)
	c.do(http.MethodPost, "/todos", `{"title": "temporary"}`)
	rec := c.do(http.MethodDelete, "/todos/1", "")
	expect(t, rec, "DELETE /todos/1", http.StatusNoContent)
	if rec.Body.Len() != 0 {
		t.Errorf("DELETE body = %q, want empty", rec.Body.String())
	}
	expect(t, c.do(http.MethodGet, "/todos/1", ""), "GET after DELETE", http.StatusNotFound)
	expect(t, c.do(http.MethodDelete, "/todos/1", ""), "second DELETE", http.StatusNotFound)
}

func TestWrongMethodsGet405FromTheMux(t *testing.T) {
	c := newClient(t)
	rec := c.do(http.MethodPost, "/healthz", "")
	expect(t, rec, "POST /healthz", http.StatusMethodNotAllowed)
	if allow := rec.Header().Get("Allow"); !strings.Contains(allow, "GET") {
		t.Errorf("Allow header = %q, want it to list GET", allow)
	}
	expect(t, c.do(http.MethodPut, "/todos/1", `{}`), "PUT /todos/1", http.StatusMethodNotAllowed)
	expect(t, c.do(http.MethodGet, "/nowhere", ""), "GET /nowhere", http.StatusNotFound)
}

func TestRecoverTurnsPanicsInto500AndLogsThem(t *testing.T) {
	logs := &bytes.Buffer{}
	logger := slog.New(slog.NewTextHandler(logs, nil))
	boom := http.HandlerFunc(func(http.ResponseWriter, *http.Request) { panic("database exploded") })
	rec := httptest.NewRecorder()
	api.Recover(logger, boom).ServeHTTP(rec, httptest.NewRequest(http.MethodGet, "/boom", nil))
	expectJSONError(t, rec, "a panicking handler", http.StatusInternalServerError)
	if strings.Contains(rec.Body.String(), "database exploded") {
		t.Errorf("the response %q must not leak the panic value to the client", rec.Body.String())
	}
	if !strings.Contains(logs.String(), "database exploded") {
		t.Errorf("logs = %q, want the panic value logged", logs.String())
	}
}

func TestConcurrentCreatesGetUniqueIDs(t *testing.T) {
	c := newClient(t)
	var wg sync.WaitGroup
	ids := make([]int, 50)
	for i := range ids {
		wg.Go(func() {
			rec := httptest.NewRecorder()
			c.handler.ServeHTTP(rec, httptest.NewRequest(http.MethodPost, "/todos", strings.NewReader(`{"title": "x"}`)))
			var todo api.Todo
			_ = json.Unmarshal(rec.Body.Bytes(), &todo)
			ids[i] = todo.ID
		})
	}
	wg.Wait()
	slices.Sort(ids)
	for i, id := range ids {
		if id != i+1 {
			t.Fatalf("ids from 50 concurrent POSTs = %v, want 1..50 each once", ids)
		}
	}
	if list := decode[[]api.Todo](t, c.do(http.MethodGet, "/todos", "")); len(list) != 50 {
		t.Errorf("GET /todos after 50 POSTs has %d todos, want 50", len(list))
	}
}
