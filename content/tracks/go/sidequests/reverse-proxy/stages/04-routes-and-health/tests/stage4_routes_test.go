package proxy_test

import (
	"net/http"
	"net/http/httptest"
	"testing"

	"dojo/proxy"
)

// named answers with its name and the path it received.
func named(t *testing.T, name string) *httptest.Server {
	return upstream(t, func(w http.ResponseWriter, r *http.Request) {
		w.Write([]byte(name + " " + r.URL.Path))
	})
}

func through(handler http.Handler, path string) *httptest.ResponseRecorder {
	return send(handler, httptest.NewRequest(http.MethodGet, path, nil), "")
}

func TestTheLongestPrefixWins(t *testing.T) {
	api, users, web := named(t, "api"), named(t, "users"), named(t, "web")
	handler := build(t, proxy.Config{
		Upstream: web.URL,
		Routes: []proxy.Route{
			{Prefix: "/api", Upstream: api.URL},
			{Prefix: "/api/users", Upstream: users.URL, StripPrefix: true},
		},
	})
	for path, want := range map[string]string{
		"/api/users/7":      "users /7",
		"/api/users":        "users /",
		"/api/orders":       "api /api/orders",
		"/api/usersettings": "api /api/usersettings",
		"/about":            "web /about",
		"/apiary":           "web /apiary",
	} {
		if got := through(handler, path).Body.String(); got != want {
			t.Errorf("%s went to %q, want %q", path, got, want)
		}
	}
}

func TestWithoutARouteOrDefaultItIsNotFound(t *testing.T) {
	api := named(t, "api")
	handler := build(t, proxy.Config{Routes: []proxy.Route{{Prefix: "/api", Upstream: api.URL}}})
	expectProblem(t, through(handler, "/elsewhere"), http.StatusNotFound)
	if got := through(handler, "/api/x").Body.String(); got != "api /api/x" {
		t.Errorf("got %q", got)
	}
}

func TestHealthzIsAnsweredLocally(t *testing.T) {
	api := named(t, "api")
	handler := build(t, proxy.Config{Upstream: api.URL})
	res := through(handler, "/healthz")
	if res.Code != http.StatusOK || res.Body.String() != `{"status":"ok"}` || res.Header().Get("Content-Type") != "application/json" {
		t.Errorf("got %d %q %q", res.Code, res.Body.String(), res.Header().Get("Content-Type"))
	}
}

func TestABadRouteUpstreamIsRefused(t *testing.T) {
	_, err := proxy.New(proxy.Config{Routes: []proxy.Route{{Prefix: "/api", Upstream: "::nope"}}})
	if err == nil {
		t.Error("proxy.New accepted a route with a bad upstream")
	}
}
