// Command server is the small web service the Docker katas package into images.
// It uses only the Go standard library, so it builds with no network.
//
// Environment:
//
//	PORT            port to listen on (default 8080)
//	GREETING_NAME   who GET /hello greets (default "world")
//	DATA_DIR        when set, the server writes started.txt there and stops if it cannot
//	LOG_FILE        when set, logs go to this file instead of stdout
//	API_TOKEN_FILE  file holding the API token (default /run/secrets/api_token)
//
// Flags: -healthcheck asks the local server for /health and exits 0 when it answers,
// -version prints the version (set at build time with -ldflags "-X main.version=...").
package main

import (
	"context"
	"errors"
	"flag"
	"fmt"
	"log/slog"
	"net/http"
	"os"
	"os/signal"
	"path/filepath"
	"syscall"
	"time"
)

var version = "dev"

func main() {
	healthcheck := flag.Bool("healthcheck", false, "GET /health on the local server, exit 0 on 200")
	showVersion := flag.Bool("version", false, "print the version and exit")
	flag.Parse()

	port := getenv("PORT", "8080")
	if *showVersion {
		fmt.Println(version)
		return
	}
	if *healthcheck {
		os.Exit(probe(port))
	}

	logger, err := newLogger()
	if err != nil {
		fmt.Fprintln(os.Stderr, "server:", err)
		os.Exit(1)
	}
	if dir := os.Getenv("DATA_DIR"); dir != "" {
		file := filepath.Join(dir, "started.txt")
		if err := os.WriteFile(file, []byte(time.Now().UTC().Format(time.RFC3339)+"\n"), 0o644); err != nil {
			logger.Error("cannot write to the data directory", "dir", dir, "error", err.Error())
			os.Exit(1)
		}
	}
	logger.Info("api token", "status", tokenStatus())

	mux := http.NewServeMux()
	mux.HandleFunc("GET /health", func(w http.ResponseWriter, _ *http.Request) {
		fmt.Fprintln(w, "ok")
	})
	mux.HandleFunc("GET /hello", func(w http.ResponseWriter, _ *http.Request) {
		fmt.Fprintf(w, "hello, %s\n", getenv("GREETING_NAME", "world"))
	})
	server := &http.Server{
		Addr:              ":" + port,
		Handler:           logRequests(logger, mux),
		ReadHeaderTimeout: 5 * time.Second,
	}

	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer stop()
	go func() {
		logger.Info("listening on :" + port)
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

func getenv(key, fallback string) string {
	if value := os.Getenv(key); value != "" {
		return value
	}
	return fallback
}

func newLogger() (*slog.Logger, error) {
	out := os.Stdout
	if path := os.Getenv("LOG_FILE"); path != "" {
		file, err := os.OpenFile(path, os.O_CREATE|os.O_APPEND|os.O_WRONLY, 0o644)
		if err != nil {
			return nil, err
		}
		out = file
	}
	return slog.New(slog.NewJSONHandler(out, nil)), nil
}

func tokenStatus() string {
	data, err := os.ReadFile(getenv("API_TOKEN_FILE", "/run/secrets/api_token"))
	if err != nil || len(data) == 0 {
		return "missing"
	}
	return "configured"
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

func logRequests(logger *slog.Logger, next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		logger.Info("request", "method", r.Method, "path", r.URL.Path)
		next.ServeHTTP(w, r)
	})
}
