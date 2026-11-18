// Command shortener is a small URL shortener. It uses only the Go standard library, so it
// builds with no network. You package it; you do not need to change it.
//
// Environment:
//
//	PORT  port to listen on (default 8080)
//
// Flags:
//
//	-version      print "shortener <version>" and exit; the version is set at build time
//	              with -ldflags "-X main.version=1.2.3" and is "dev" otherwise
//	-healthcheck  GET /health on the local server and exit 0 when it answers 200, 1 if not
//
// HTTP:
//
//	GET  /health   "ok"
//	POST /links    form field url (http or https); answers 201 with the code
//	GET  /{code}   302 to the URL of that code, 404 when unknown
//	GET  /stats    "links <count>"
package main

import (
	"context"
	"crypto/sha256"
	"encoding/base64"
	"errors"
	"flag"
	"fmt"
	"log/slog"
	"net/http"
	"net/url"
	"os"
	"os/signal"
	"sync"
	"syscall"
	"time"
)

var version = "dev"

func main() {
	showVersion := flag.Bool("version", false, "print the version and exit")
	healthcheck := flag.Bool("healthcheck", false, "probe the local server and exit")
	flag.Parse()

	port := getenv("PORT", "8080")
	if *showVersion {
		fmt.Println("shortener " + version)
		return
	}
	if *healthcheck {
		os.Exit(probe(port))
	}

	logger := slog.New(slog.NewJSONHandler(os.Stdout, nil))
	links := &store{byCode: map[string]string{}}
	mux := http.NewServeMux()
	mux.HandleFunc("GET /health", func(w http.ResponseWriter, _ *http.Request) {
		fmt.Fprintln(w, "ok")
	})
	mux.HandleFunc("GET /stats", func(w http.ResponseWriter, _ *http.Request) {
		fmt.Fprintf(w, "links %d\n", links.count())
	})
	mux.HandleFunc("POST /links", func(w http.ResponseWriter, r *http.Request) {
		target, err := url.Parse(r.FormValue("url"))
		if err != nil || (target.Scheme != "http" && target.Scheme != "https") || target.Host == "" {
			http.Error(w, "url must be an http or https URL", http.StatusBadRequest)
			return
		}
		w.WriteHeader(http.StatusCreated)
		fmt.Fprintln(w, links.add(target.String()))
	})
	mux.HandleFunc("GET /{code}", func(w http.ResponseWriter, r *http.Request) {
		target, ok := links.get(r.PathValue("code"))
		if !ok {
			http.NotFound(w, r)
			return
		}
		http.Redirect(w, r, target, http.StatusFound)
	})

	server := &http.Server{Addr: ":" + port, Handler: mux, ReadHeaderTimeout: 5 * time.Second}
	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer stop()
	go func() {
		logger.Info("listening", "port", port, "version", version)
		if err := server.ListenAndServe(); err != nil && !errors.Is(err, http.ErrServerClosed) {
			logger.Error("server stopped", "error", err.Error())
			os.Exit(1)
		}
	}()
	<-ctx.Done()
	logger.Info("shutting down")
	shutdown, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()
	_ = server.Shutdown(shutdown)
}

type store struct {
	mu     sync.Mutex
	byCode map[string]string
}

// add returns the code of the URL: the same URL always gets the same code.
func (s *store) add(target string) string {
	sum := sha256.Sum256([]byte(target))
	code := base64.RawURLEncoding.EncodeToString(sum[:])[:7]
	s.mu.Lock()
	defer s.mu.Unlock()
	s.byCode[code] = target
	return code
}

func (s *store) get(code string) (string, bool) {
	s.mu.Lock()
	defer s.mu.Unlock()
	target, ok := s.byCode[code]
	return target, ok
}

func (s *store) count() int {
	s.mu.Lock()
	defer s.mu.Unlock()
	return len(s.byCode)
}

func probe(port string) int {
	client := http.Client{Timeout: 2 * time.Second}
	response, err := client.Get("http://localhost:" + port + "/health")
	if err != nil {
		return 1
	}
	defer response.Body.Close()
	if response.StatusCode != http.StatusOK {
		return 1
	}
	return 0
}

func getenv(key, fallback string) string {
	if value := os.Getenv(key); value != "" {
		return value
	}
	return fallback
}
