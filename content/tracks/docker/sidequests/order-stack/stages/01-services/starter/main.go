// Command orders is the order service of the Docker sidequest "Order stack". One binary
// plays every role, picked by the first argument. It uses only the Go standard library, so
// it builds with no network. You package and wire it; you do not need to change it.
//
//	orders api                 HTTP API on PORT (default 8080)
//	orders worker              takes jobs from the queue; touches HEARTBEAT_FILE every 2 s
//	orders migrate             waits for the database, applies the migrations and exits 0
//	orders migrate -plan       prints the migrations and exits, without a database
//	orders healthcheck api     exit 0 when the local API answers /health
//	orders healthcheck worker  exit 0 when the worker's heartbeat is less than 10 s old
//	orders version             prints "orders <version>"
//
// Environment:
//
//	PORT                    API port (default 8080)
//	DATABASE_HOST           PostgreSQL host (default "db"), port 5432
//	DATABASE_PASSWORD_FILE  file holding the database password; only its presence is logged
//	REDIS_ADDR              Redis address (default "cache:6379")
//	HEARTBEAT_FILE          worker heartbeat (default /tmp/orders-worker.alive)
//	LOG_LEVEL               debug, info (default) or warn
//
// HTTP: GET /health is "ok" while the process runs; GET /ready is 200 "ready" when the
// database and Redis accept connections and 503 otherwise.
package main

import (
	"context"
	"errors"
	"flag"
	"fmt"
	"log/slog"
	"net"
	"net/http"
	"os"
	"os/signal"
	"strings"
	"syscall"
	"time"
)

const version = "2.1.0"

var migrations = []string{"001_create_orders", "002_create_order_lines", "003_add_order_status"}

func main() {
	role := "api"
	args := os.Args[1:]
	if len(args) > 0 {
		role, args = args[0], args[1:]
	}
	logger := newLogger(role)
	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer stop()

	var err error
	switch role {
	case "api":
		err = runAPI(ctx, logger)
	case "worker":
		err = runWorker(ctx, logger)
	case "migrate":
		err = runMigrate(ctx, logger, args)
	case "healthcheck":
		os.Exit(healthcheck(args))
	case "version", "-version", "--version":
		fmt.Println("orders " + version)
	default:
		err = fmt.Errorf("unknown role %q (api, worker, migrate, healthcheck or version)", role)
	}
	if err != nil {
		logger.Error(err.Error())
		os.Exit(1)
	}
}

func runAPI(ctx context.Context, logger *slog.Logger) error {
	port := getenv("PORT", "8080")
	logger.Info("starting", "version", version, "db_password", passwordStatus())
	mux := http.NewServeMux()
	mux.HandleFunc("GET /health", func(w http.ResponseWriter, _ *http.Request) {
		fmt.Fprintln(w, "ok")
	})
	mux.HandleFunc("GET /ready", func(w http.ResponseWriter, _ *http.Request) {
		for name, addr := range map[string]string{"db": dbAddr(), "redis": redisAddr()} {
			if !reachable(addr) {
				http.Error(w, "waiting for "+name, http.StatusServiceUnavailable)
				return
			}
		}
		fmt.Fprintln(w, "ready")
	})
	server := &http.Server{Addr: ":" + port, Handler: mux, ReadHeaderTimeout: 5 * time.Second}
	failed := make(chan error, 1)
	go func() {
		logger.Info("listening", "port", port)
		if err := server.ListenAndServe(); err != nil && !errors.Is(err, http.ErrServerClosed) {
			failed <- err
		}
	}()
	select {
	case err := <-failed:
		return err
	case <-ctx.Done():
	}
	logger.Info("shutting down")
	shutdown, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()
	return server.Shutdown(shutdown)
}

func runWorker(ctx context.Context, logger *slog.Logger) error {
	file := heartbeatFile()
	logger.Info("starting", "version", version, "queue", redisAddr(), "db_password", passwordStatus())
	ticker := time.NewTicker(2 * time.Second)
	defer ticker.Stop()
	for {
		if err := os.WriteFile(file, []byte(time.Now().UTC().Format(time.RFC3339)), 0o644); err != nil {
			return fmt.Errorf("cannot write the heartbeat %s: %w", file, err)
		}
		if reachable(redisAddr()) {
			logger.Debug("queue empty")
		} else {
			logger.Warn("queue unreachable", "addr", redisAddr())
		}
		select {
		case <-ctx.Done():
			logger.Info("shutting down")
			return nil
		case <-ticker.C:
		}
	}
}

func runMigrate(ctx context.Context, logger *slog.Logger, args []string) error {
	flags := flag.NewFlagSet("migrate", flag.ContinueOnError)
	plan := flags.Bool("plan", false, "print the migrations and exit")
	if err := flags.Parse(args); err != nil {
		return err
	}
	if *plan {
		for _, name := range migrations {
			fmt.Println(name)
		}
		return nil
	}
	deadline := time.Now().Add(60 * time.Second)
	for !reachable(dbAddr()) {
		if time.Now().After(deadline) {
			return fmt.Errorf("database %s unreachable", dbAddr())
		}
		select {
		case <-ctx.Done():
			return ctx.Err()
		case <-time.After(time.Second):
		}
	}
	for _, name := range migrations {
		logger.Info("applied", "migration", name)
	}
	return nil
}

func healthcheck(args []string) int {
	if len(args) == 1 && args[0] == "worker" {
		info, err := os.Stat(heartbeatFile())
		if err != nil || time.Since(info.ModTime()) > 10*time.Second {
			return 1
		}
		return 0
	}
	if len(args) > 1 || (len(args) == 1 && args[0] != "api") {
		fmt.Fprintln(os.Stderr, "usage: orders healthcheck [api|worker]")
		return 2
	}
	client := http.Client{Timeout: 2 * time.Second}
	response, err := client.Get("http://localhost:" + getenv("PORT", "8080") + "/health")
	if err != nil {
		return 1
	}
	defer response.Body.Close()
	if response.StatusCode != http.StatusOK {
		return 1
	}
	return 0
}

func reachable(addr string) bool {
	conn, err := net.DialTimeout("tcp", addr, time.Second)
	if err != nil {
		return false
	}
	conn.Close()
	return true
}

func passwordStatus() string {
	path := os.Getenv("DATABASE_PASSWORD_FILE")
	if path == "" {
		return "not set"
	}
	data, err := os.ReadFile(path)
	if err != nil || strings.TrimSpace(string(data)) == "" {
		return "missing"
	}
	return "configured"
}

func dbAddr() string        { return net.JoinHostPort(getenv("DATABASE_HOST", "db"), "5432") }
func redisAddr() string     { return getenv("REDIS_ADDR", "cache:6379") }
func heartbeatFile() string { return getenv("HEARTBEAT_FILE", "/tmp/orders-worker.alive") }

func newLogger(role string) *slog.Logger {
	level := slog.LevelInfo
	switch getenv("LOG_LEVEL", "info") {
	case "debug":
		level = slog.LevelDebug
	case "warn":
		level = slog.LevelWarn
	}
	handler := slog.NewJSONHandler(os.Stdout, &slog.HandlerOptions{Level: level})
	return slog.New(handler).With("role", role)
}

func getenv(key, fallback string) string {
	if value := os.Getenv(key); value != "" {
		return value
	}
	return fallback
}
