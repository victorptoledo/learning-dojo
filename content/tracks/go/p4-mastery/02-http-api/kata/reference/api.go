// Package api serves a small to-do list over HTTP with JSON.
package api

import (
	"encoding/json"
	"fmt"
	"log/slog"
	"maps"
	"net/http"
	"slices"
	"strconv"
	"strings"
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
	s.mu.Lock()
	defer s.mu.Unlock()
	if s.todos == nil {
		s.todos = make(map[int]Todo)
	}
	s.next++
	todo := Todo{ID: s.next, Title: title}
	s.todos[todo.ID] = todo
	return todo
}

// Get returns the todo with this id.
func (s *Store) Get(id int) (Todo, bool) {
	s.mu.Lock()
	defer s.mu.Unlock()
	todo, ok := s.todos[id]
	return todo, ok
}

// List returns every todo, sorted by id.
func (s *Store) List() []Todo {
	s.mu.Lock()
	defer s.mu.Unlock()
	list := make([]Todo, 0, len(s.todos))
	for _, id := range slices.Sorted(maps.Keys(s.todos)) {
		list = append(list, s.todos[id])
	}
	return list
}

// Update applies change to the todo with this id and returns the new value.
func (s *Store) Update(id int, change func(*Todo)) (Todo, bool) {
	s.mu.Lock()
	defer s.mu.Unlock()
	todo, ok := s.todos[id]
	if !ok {
		return Todo{}, false
	}
	change(&todo)
	s.todos[id] = todo
	return todo, true
}

// Delete removes the todo with this id and reports whether it existed.
func (s *Store) Delete(id int) bool {
	s.mu.Lock()
	defer s.mu.Unlock()
	_, ok := s.todos[id]
	delete(s.todos, id)
	return ok
}

const maxBody = 1 << 20

type server struct {
	store *Store
}

// NewHandler returns the API: GET /healthz, GET and POST /todos, and GET, PATCH and DELETE
// /todos/{id}, wrapped in Recover.
func NewHandler(store *Store, logger *slog.Logger) http.Handler {
	s := &server{store: store}
	mux := http.NewServeMux()
	mux.HandleFunc("GET /healthz", s.health)
	mux.HandleFunc("GET /todos", s.list)
	mux.HandleFunc("POST /todos", s.create)
	mux.HandleFunc("GET /todos/{id}", s.get)
	mux.HandleFunc("PATCH /todos/{id}", s.update)
	mux.HandleFunc("DELETE /todos/{id}", s.delete)
	return Recover(logger, mux)
}

// Recover turns a panic in next into a 500 with a JSON error and logs it.
func Recover(logger *slog.Logger, next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		defer func() {
			if v := recover(); v != nil {
				logger.Error("panic", "method", r.Method, "path", r.URL.Path, "panic", fmt.Sprint(v))
				writeError(w, http.StatusInternalServerError, "internal error")
			}
		}()
		next.ServeHTTP(w, r)
	})
}

func writeJSON(w http.ResponseWriter, status int, v any) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	_ = json.NewEncoder(w).Encode(v)
}

func writeError(w http.ResponseWriter, status int, message string) {
	writeJSON(w, status, map[string]string{"error": message})
}

func (s *server) health(w http.ResponseWriter, _ *http.Request) {
	w.Header().Set("Content-Type", "text/plain; charset=utf-8")
	fmt.Fprint(w, "ok")
}

func (s *server) list(w http.ResponseWriter, _ *http.Request) {
	writeJSON(w, http.StatusOK, s.store.List())
}

func (s *server) create(w http.ResponseWriter, r *http.Request) {
	var in struct {
		Title string `json:"title"`
	}
	if err := json.NewDecoder(http.MaxBytesReader(w, r.Body, maxBody)).Decode(&in); err != nil {
		writeError(w, http.StatusBadRequest, "invalid JSON body")
		return
	}
	title := strings.TrimSpace(in.Title)
	if title == "" {
		writeError(w, http.StatusBadRequest, "title is required")
		return
	}
	todo := s.store.Add(title)
	w.Header().Set("Location", fmt.Sprintf("/todos/%d", todo.ID))
	writeJSON(w, http.StatusCreated, todo)
}

// pathID reads the {id} wildcard; it answers 400 itself when the id is not a positive integer.
func pathID(w http.ResponseWriter, r *http.Request) (int, bool) {
	id, err := strconv.Atoi(r.PathValue("id"))
	if err != nil || id < 1 {
		writeError(w, http.StatusBadRequest, fmt.Sprintf("invalid id %q", r.PathValue("id")))
		return 0, false
	}
	return id, true
}

func notFound(w http.ResponseWriter, id int) {
	writeError(w, http.StatusNotFound, fmt.Sprintf("todo %d not found", id))
}

func (s *server) get(w http.ResponseWriter, r *http.Request) {
	id, ok := pathID(w, r)
	if !ok {
		return
	}
	todo, found := s.store.Get(id)
	if !found {
		notFound(w, id)
		return
	}
	writeJSON(w, http.StatusOK, todo)
}

func (s *server) update(w http.ResponseWriter, r *http.Request) {
	id, ok := pathID(w, r)
	if !ok {
		return
	}
	var in struct {
		Title *string `json:"title"`
		Done  *bool   `json:"done"`
	}
	if err := json.NewDecoder(http.MaxBytesReader(w, r.Body, maxBody)).Decode(&in); err != nil {
		writeError(w, http.StatusBadRequest, "invalid JSON body")
		return
	}
	if in.Title != nil && strings.TrimSpace(*in.Title) == "" {
		writeError(w, http.StatusBadRequest, "title cannot be empty")
		return
	}
	todo, found := s.store.Update(id, func(t *Todo) {
		if in.Title != nil {
			t.Title = strings.TrimSpace(*in.Title)
		}
		if in.Done != nil {
			t.Done = *in.Done
		}
	})
	if !found {
		notFound(w, id)
		return
	}
	writeJSON(w, http.StatusOK, todo)
}

func (s *server) delete(w http.ResponseWriter, r *http.Request) {
	id, ok := pathID(w, r)
	if !ok {
		return
	}
	if !s.store.Delete(id) {
		notFound(w, id)
		return
	}
	w.WriteHeader(http.StatusNoContent)
}
