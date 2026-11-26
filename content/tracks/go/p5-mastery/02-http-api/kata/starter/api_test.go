package api_test

import (
	"io"
	"log/slog"
	"net/http"
	"net/http/httptest"
	"testing"

	"dojo/api"
)

func TestHealthz(t *testing.T) {
	handler := api.NewHandler(&api.Store{}, slog.New(slog.NewTextHandler(io.Discard, nil)))
	rec := httptest.NewRecorder()
	handler.ServeHTTP(rec, httptest.NewRequest(http.MethodGet, "/healthz", nil))
	if rec.Code != http.StatusOK || rec.Body.String() != "ok" {
		t.Errorf("GET /healthz = %d %q, want 200 \"ok\"", rec.Code, rec.Body.String())
	}
}
