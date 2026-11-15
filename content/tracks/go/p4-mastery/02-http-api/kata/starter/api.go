// Package api serves a small to-do list over HTTP with JSON.
package api

import (
	"encoding/json"
	"log/slog"
	"net/http"
	"sync"
)

// Todo is one item of the list.
type Todo struct {
	ID    int    `json:"id"`
	Title string `json:"title"`
	Done  bool   `json:"done"`
}

// Store keeps todos in memory. It is safe for concurrent use; its zero value is ready.
type Store struct {
	mu    sync.Mutex // guards next and todos
	next  int
	todos map[int]Todo
}

// Add stores a new todo with the next id (1, 2, 3, ...) and returns it.
func (s *Store) Add(title string) Todo {
	panic("todo: write Store.Add")
}

// Get returns the todo with this id.
func (s *Store) Get(id int) (Todo, bool) {
	panic("todo: write Store.Get")
}

// List returns every todo, sorted by id.
func (s *Store) List() []Todo {
	panic("todo: write Store.List")
}

// Update applies change to the todo with this id and returns the new value.
func (s *Store) Update(id int, change func(*Todo)) (Todo, bool) {
	panic("todo: write Store.Update")
}

// Delete removes the todo with this id and reports whether it existed.
func (s *Store) Delete(id int) bool {
	panic("todo: write Store.Delete")
}

// NewHandler returns the API: GET /healthz, GET and POST /todos, and GET, PATCH and DELETE
// /todos/{id}, wrapped in Recover.
func NewHandler(store *Store, logger *slog.Logger) http.Handler {
	panic("todo: write NewHandler and its handlers")
}

// Recover turns a panic in next into a 500 with a JSON error and logs it.
func Recover(logger *slog.Logger, next http.Handler) http.Handler {
	panic("todo: write Recover")
}

// writeJSON answers with status and v encoded as JSON.
func writeJSON(w http.ResponseWriter, status int, v any) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	_ = json.NewEncoder(w).Encode(v)
}

// writeError answers with status and {"error": message}.
func writeError(w http.ResponseWriter, status int, message string) {
	writeJSON(w, status, map[string]string{"error": message})
}
