// Command inventory is the stock API of the Docker sidequest "Release image". It uses only
// the Go standard library, so it builds with no network. You package it; you do not need
// to change it.
//
// Environment:
//
//	PORT       port to listen on (default 8080)
//	DATA_DIR   folder that holds stock.json (default /var/lib/inventory); the server stops
//	           at once when it cannot write there
//	LOG_LEVEL  debug, info (default) or warn; logs are JSON lines on standard output
//
// Flags:
//
//	-version      print "inventory <version>" and exit; the version is set at build time
//	              with -ldflags "-X main.version=2.3.0" and is "dev" otherwise
//	-healthcheck  GET /health on the local server, exit 0 when it answers 200
//	-preflight    print what the process can see and do, one key=value per line, and
//	              exit: pid, uid, gid, exe_writable, data_dir, data_writable,
//	              tmp_writable, ca_certs and zone
//
// HTTP:
//
//	GET /health       "ok"
//	GET /items        every item as JSON
//	PUT /items/{sku}  form field qty (an integer); answers 200 with the item
package main

import (
	"context"
	"crypto/x509"
	"errors"
	"flag"
	"fmt"
	"log/slog"
	"net/http"
	"os"
	"os/signal"
	"strconv"
	"syscall"
	"time"

	"example.com/inventory/internal/stock"
)

var version = "dev"

func main() {
	showVersion := flag.Bool("version", false, "print the version and exit")
	healthcheck := flag.Bool("healthcheck", false, "probe the local server and exit")
	preflight := flag.Bool("preflight", false, "report what the process can do and exit")
	flag.Parse()

	port := getenv("PORT", "8080")
	dataDir := getenv("DATA_DIR", "/var/lib/inventory")
	switch {
	case *showVersion:
		fmt.Println("inventory " + version)
		return
	case *healthcheck:
		os.Exit(probe(port))
	case *preflight:
		report(dataDir)
		return
	}

	logger := newLogger()
	store, err := stock.Open(dataDir)
	if err != nil {
		logger.Error("cannot use the data directory", "dir", dataDir, "error", err.Error())
		os.Exit(1)
	}

	mux := http.NewServeMux()
	mux.HandleFunc("GET /health", func(w http.ResponseWriter, _ *http.Request) {
		fmt.Fprintln(w, "ok")
	})
	mux.HandleFunc("GET /items", func(w http.ResponseWriter, _ *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		_ = store.WriteJSON(w)
	})
	mux.HandleFunc("PUT /items/{sku}", func(w http.ResponseWriter, r *http.Request) {
		qty, err := strconv.Atoi(r.FormValue("qty"))
		if err != nil || qty < 0 {
			http.Error(w, "qty must be a non-negative integer", http.StatusBadRequest)
			return
		}
		item, err := store.Set(r.PathValue("sku"), qty)
		if err != nil {
			logger.Error("cannot save", "error", err.Error())
			http.Error(w, "cannot save", http.StatusInternalServerError)
			return
		}
		w.Header().Set("Content-Type", "application/json")
		fmt.Fprintf(w, "{\"sku\":%q,\"qty\":%d}\n", item.SKU, item.Qty)
	})

	server := &http.Server{Addr: ":" + port, Handler: mux, ReadHeaderTimeout: 5 * time.Second}
	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer stop()
	go func() {
		logger.Info("listening", "port", port, "version", version, "data_dir", dataDir)
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

// report prints what the dojo checks about the running process.
func report(dataDir string) {
	exe, err := os.Executable()
	exeWritable := err == nil && syscall.Access(exe, 2) == nil
	fmt.Printf("pid=%d\n", os.Getpid())
	fmt.Printf("uid=%d\n", os.Getuid())
	fmt.Printf("gid=%d\n", os.Getgid())
	fmt.Printf("exe_writable=%t\n", exeWritable)
	fmt.Printf("data_dir=%s\n", dataDir)
	fmt.Printf("data_writable=%t\n", canWrite(dataDir))
	fmt.Printf("tmp_writable=%t\n", canWrite(os.TempDir()))
	certs := "missing"
	if pool, err := x509.SystemCertPool(); err == nil && !pool.Equal(x509.NewCertPool()) {
		certs = "ok"
	}
	fmt.Printf("ca_certs=%s\n", certs)
	zone := "missing"
	if _, err := time.LoadLocation("America/Sao_Paulo"); err == nil {
		zone = "ok"
	}
	fmt.Printf("zone=%s\n", zone)
}

func canWrite(dir string) bool {
	file, err := os.CreateTemp(dir, ".preflight-*")
	if err != nil {
		return false
	}
	file.Close()
	os.Remove(file.Name())
	return true
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

func newLogger() *slog.Logger {
	level := slog.LevelInfo
	switch os.Getenv("LOG_LEVEL") {
	case "debug":
		level = slog.LevelDebug
	case "warn":
		level = slog.LevelWarn
	}
	return slog.New(slog.NewJSONHandler(os.Stdout, &slog.HandlerOptions{Level: level}))
}

func getenv(key, fallback string) string {
	if value := os.Getenv(key); value != "" {
		return value
	}
	return fallback
}
