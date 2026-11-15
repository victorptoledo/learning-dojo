package warm_test

import (
	"net/http"
	"net/http/httptest"
	"testing"
	"testing/synctest"
)

func TestWarm(t *testing.T) {
	synctest.Test(t, func(t *testing.T) {})
	recorder := httptest.NewRecorder()
	http.NotFound(recorder, httptest.NewRequest(http.MethodGet, "/", nil))
	if recorder.Code != http.StatusNotFound {
		t.Fatal(recorder.Code)
	}
}
