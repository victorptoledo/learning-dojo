// Command guestbook is a small message board. It uses only the Go standard library, so
// it builds with no network. You package it; you do not need to change it.
//
// Environment:
//
//	PORT          port to listen on (default 8080)
//	BOARD_TITLE   title shown on GET / (default "Guestbook")
//	DATA_DIR      folder that holds messages.txt (default "data", next to the working
//	              directory); created when missing
//	WELCOME_FILE  copied into messages.txt when that file does not exist yet
//	              (default /usr/share/guestbook/welcome.txt; skipped when missing)
//
// Flags: -version prints "guestbook <version>" and exits.
//
// HTTP:
//
//	GET  /          the title and the number of messages
//	GET  /health    "ok"
//	GET  /messages  every message, one "name: text" per line
//	POST /messages  form fields name and text; answers 201
package main

import (
	"bufio"
	"context"
	"errors"
	"flag"
	"fmt"
	"io/fs"
	"log"
	"net/http"
	"os"
	"os/signal"
	"path/filepath"
	"strings"
	"sync"
	"syscall"
	"time"
)

const version = "1.0.0"

func main() {
	showVersion := flag.Bool("version", false, "print the version and exit")
	flag.Parse()
	if *showVersion {
		fmt.Println("guestbook " + version)
		return
	}
	log.SetOutput(os.Stdout)
	log.SetFlags(0)

	board, err := openBoard(getenv("DATA_DIR", "data"), getenv("WELCOME_FILE", "/usr/share/guestbook/welcome.txt"))
	if err != nil {
		log.Printf("guestbook: %v", err)
		os.Exit(1)
	}
	log.Printf("messages in %s", board.file)

	port := getenv("PORT", "8080")
	title := getenv("BOARD_TITLE", "Guestbook")
	mux := http.NewServeMux()
	mux.HandleFunc("GET /health", func(w http.ResponseWriter, _ *http.Request) {
		fmt.Fprintln(w, "ok")
	})
	mux.HandleFunc("GET /{$}", func(w http.ResponseWriter, _ *http.Request) {
		lines, _ := board.read()
		fmt.Fprintf(w, "%s\n%d messages\n", title, len(lines))
	})
	mux.HandleFunc("GET /messages", func(w http.ResponseWriter, _ *http.Request) {
		lines, err := board.read()
		if err != nil {
			http.Error(w, err.Error(), http.StatusInternalServerError)
			return
		}
		for _, line := range lines {
			fmt.Fprintln(w, line)
		}
	})
	mux.HandleFunc("POST /messages", func(w http.ResponseWriter, r *http.Request) {
		name := oneLine(r.FormValue("name"))
		text := oneLine(r.FormValue("text"))
		if name == "" || text == "" {
			http.Error(w, "name and text are required", http.StatusBadRequest)
			return
		}
		if err := board.add(name + ": " + text); err != nil {
			http.Error(w, err.Error(), http.StatusInternalServerError)
			return
		}
		w.WriteHeader(http.StatusCreated)
		fmt.Fprintln(w, "saved")
	})

	server := &http.Server{Addr: ":" + port, Handler: mux, ReadHeaderTimeout: 5 * time.Second}
	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer stop()
	go func() {
		log.Printf("listening on :%s", port)
		if err := server.ListenAndServe(); err != nil && !errors.Is(err, http.ErrServerClosed) {
			log.Printf("guestbook: %v", err)
			os.Exit(1)
		}
	}()
	<-ctx.Done()
	log.Print("shutting down")
	shutdown, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()
	_ = server.Shutdown(shutdown)
}

type board struct {
	mu   sync.Mutex
	file string
}

func openBoard(dir, welcome string) (*board, error) {
	if err := os.MkdirAll(dir, 0o755); err != nil {
		return nil, err
	}
	file, err := filepath.Abs(filepath.Join(dir, "messages.txt"))
	if err != nil {
		return nil, err
	}
	if _, err := os.Stat(file); errors.Is(err, fs.ErrNotExist) {
		first, err := os.ReadFile(welcome)
		if err != nil && !errors.Is(err, fs.ErrNotExist) {
			return nil, err
		}
		if err := os.WriteFile(file, first, 0o644); err != nil {
			return nil, err
		}
	}
	return &board{file: file}, nil
}

func (b *board) read() ([]string, error) {
	b.mu.Lock()
	defer b.mu.Unlock()
	file, err := os.Open(b.file)
	if err != nil {
		return nil, err
	}
	defer file.Close()
	var lines []string
	scanner := bufio.NewScanner(file)
	for scanner.Scan() {
		if line := strings.TrimSpace(scanner.Text()); line != "" {
			lines = append(lines, line)
		}
	}
	return lines, scanner.Err()
}

func (b *board) add(line string) error {
	b.mu.Lock()
	defer b.mu.Unlock()
	file, err := os.OpenFile(b.file, os.O_CREATE|os.O_APPEND|os.O_WRONLY, 0o644)
	if err != nil {
		return err
	}
	if _, err := fmt.Fprintln(file, line); err != nil {
		file.Close()
		return err
	}
	return file.Close()
}

func oneLine(text string) string {
	return strings.Join(strings.Fields(text), " ")
}

func getenv(key, fallback string) string {
	if value := os.Getenv(key); value != "" {
		return value
	}
	return fallback
}
