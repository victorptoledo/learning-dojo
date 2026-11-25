// Command quotes serves a quote of the day. It uses only the Go standard library, so it
// builds with no network; in the real project it also imports a module from the private
// Git server git.dojo.example, which is why the build needs Git credentials (a .netrc).
//
// Environment:
//
//	PORT            port to listen on (default 8080)
//	API_TOKEN_FILE  file holding the partner API token (default /run/secrets/api_token)
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
	"syscall"
	"time"
)

var version = "dev"

var quotes = []string{
	"Simple things should be simple, complex things should be possible.",
	"Make it work, make it right, make it fast.",
	"The cheapest, fastest and most reliable components are those that are not there.",
}

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

	logger := slog.New(slog.NewJSONHandler(os.Stdout, nil))
	logger.Info("starting", "version", version, "uid", os.Getuid(), "api_token", tokenStatus())

	mux := http.NewServeMux()
	mux.HandleFunc("GET /health", func(w http.ResponseWriter, _ *http.Request) {
		fmt.Fprintln(w, "ok")
	})
	mux.HandleFunc("GET /quote", func(w http.ResponseWriter, _ *http.Request) {
		fmt.Fprintln(w, quotes[time.Now().YearDay()%len(quotes)])
	})
	server := &http.Server{Addr: ":" + port, Handler: mux, ReadHeaderTimeout: 5 * time.Second}

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
