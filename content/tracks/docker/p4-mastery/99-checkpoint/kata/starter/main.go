// Command catalog serves a product catalog. It uses only the Go standard library, so it
// builds with no network.
//
// Usage: catalog <command>
//
//	serve        run the HTTP server (refuses to start until the schema is migrated)
//	migrate      bring the schema in DATA_DIR to the current version; safe to run again
//	healthcheck  GET /health on the local server, exit 0 when it answers
//	version      print the version (set at build time with -ldflags "-X main.version=...")
//
// Environment:
//
//	PORT           port to listen on (default 8080)
//	DATA_DIR       where the schema and the data live (default /data)
//	LOG_FORMAT     "json" or "text" (default text)
//	DRAIN_TIMEOUT  how long serve waits for requests in flight after SIGTERM (default 25s)
//
// GET /debug/process reports the server's own PID, parent PID and user, for diagnostics.
package main

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"log/slog"
	"net/http"
	"os"
	"os/signal"
	"path/filepath"
	"strconv"
	"strings"
	"syscall"
	"time"
)

var version = "2.0.0"

// schemaVersion is the schema this build of the server expects.
const schemaVersion = 3

func main() {
	command := "serve"
	if len(os.Args) > 1 {
		command = os.Args[1]
	}
	logger := newLogger()
	switch command {
	case "serve":
		os.Exit(serve(logger))
	case "migrate":
		os.Exit(migrate(logger))
	case "healthcheck":
		os.Exit(probe(getenv("PORT", "8080")))
	case "version":
		fmt.Println(version)
	default:
		fmt.Fprintf(os.Stderr, "catalog: unknown command %q (serve, migrate, healthcheck, version)\n", command)
		os.Exit(2)
	}
}

func getenv(key, fallback string) string {
	if value := os.Getenv(key); value != "" {
		return value
	}
	return fallback
}

func newLogger() *slog.Logger {
	if os.Getenv("LOG_FORMAT") == "json" {
		return slog.New(slog.NewJSONHandler(os.Stdout, nil))
	}
	return slog.New(slog.NewTextHandler(os.Stdout, nil))
}

func schemaFile() string { return filepath.Join(getenv("DATA_DIR", "/data"), "schema_version") }

func currentSchema() int {
	data, err := os.ReadFile(schemaFile())
	if err != nil {
		return 0
	}
	n, err := strconv.Atoi(strings.TrimSpace(string(data)))
	if err != nil {
		return 0
	}
	return n
}

func migrate(logger *slog.Logger) int {
	from := currentSchema()
	if from >= schemaVersion {
		logger.Info("schema up to date", "version", from)
		return 0
	}
	if err := os.WriteFile(schemaFile(), []byte(strconv.Itoa(schemaVersion)+"\n"), 0o644); err != nil {
		logger.Error("cannot migrate the schema", "dir", getenv("DATA_DIR", "/data"), "error", err.Error())
		return 1
	}
	logger.Info("schema migrated", "from", from, "to", schemaVersion)
	return 0
}

func serve(logger *slog.Logger) int {
	if got := currentSchema(); got != schemaVersion {
		logger.Error("schema is not migrated: run catalog migrate first", "found", got, "want", schemaVersion)
		return 1
	}
	port := getenv("PORT", "8080")
	drain, err := time.ParseDuration(getenv("DRAIN_TIMEOUT", "25s"))
	if err != nil {
		logger.Error("bad DRAIN_TIMEOUT", "error", err.Error())
		return 1
	}

	mux := http.NewServeMux()
	mux.HandleFunc("GET /health", func(w http.ResponseWriter, _ *http.Request) {
		fmt.Fprintln(w, "ok")
	})
	mux.HandleFunc("GET /items", func(w http.ResponseWriter, _ *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		_ = json.NewEncoder(w).Encode([]map[string]any{
			{"sku": "KB-01", "name": "Keyboard", "price": 49.9},
			{"sku": "MS-02", "name": "Mouse", "price": 19.9},
		})
	})
	mux.HandleFunc("GET /debug/process", func(w http.ResponseWriter, _ *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		_ = json.NewEncoder(w).Encode(map[string]int{
			"pid": os.Getpid(), "ppid": os.Getppid(), "uid": os.Getuid(), "gid": os.Getgid(),
		})
	})
	server := &http.Server{Addr: ":" + port, Handler: mux, ReadHeaderTimeout: 5 * time.Second}

	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer stop()
	failed := make(chan error, 1)
	go func() {
		logger.Info("listening on :"+port, "version", version, "pid", os.Getpid())
		if err := server.ListenAndServe(); err != nil && !errors.Is(err, http.ErrServerClosed) {
			failed <- err
		}
	}()
	select {
	case err := <-failed:
		logger.Error("server stopped", "error", err.Error())
		return 1
	case <-ctx.Done():
	}
	logger.Info("draining", "timeout", drain.String())
	shutdown, cancel := context.WithTimeout(context.Background(), drain)
	defer cancel()
	if err := server.Shutdown(shutdown); err != nil {
		logger.Error("drain timed out", "error", err.Error())
		return 1
	}
	logger.Info("stopped")
	return 0
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
