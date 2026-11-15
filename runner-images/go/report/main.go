// Command dojo-report runs a compiled Go test binary through test2json, adds one go vet
// result and one gofmt result per source file, and prints everything in the `cargo test`
// format the dojo reads.
//
// Usage: dojo-report KATA.test (run from the kata folder)
//
// A panic stops a Go test binary, so the tests after it would never run. When that happens
// the binary runs again, skipping the tests that already have a result, until every test
// has one. Exits 0 when everything passed, 1 when something failed, 2 when no test ran.
package main

import (
	"bufio"
	"bytes"
	"encoding/json"
	"fmt"
	"go/format"
	"os"
	"os/exec"
	"path/filepath"
	"regexp"
	"slices"
	"strings"
)

const (
	maxMessageLines = 8
	maxRuns         = 40
	testTimeout     = "20s"
)

// event is one line of `go tool test2json` output.
type event struct {
	Action string
	Test   string
	Output string
}

type result struct {
	name    string
	passed  bool
	message string
}

// testState collects what one test printed and how it ended.
type testState struct {
	ended  string // "pass", "fail" or "skip"; empty while running
	output []string
}

var (
	fileLine  = regexp.MustCompile(`^\s*[\w.-]+\.go:\d+: `)
	recovered = regexp.MustCompile(`\s*\[recovered.*\]$`)
	kataFrame = regexp.MustCompile(`/target/kata/[\w./-]+\.go:\d+`)
)

func main() {
	if len(os.Args) != 2 {
		fmt.Fprintln(os.Stderr, "usage: dojo-report KATA.test")
		os.Exit(2)
	}
	tests, log := runTests(os.Args[1])
	if len(tests) == 0 {
		fmt.Println("error: no tests ran")
		fmt.Println(strings.Join(head(log, 40), "\n"))
		os.Exit(2)
	}
	results := append(tests, vetResult())
	results = append(results, gofmtResults()...)
	os.Exit(printCargo(results))
}

// runTests runs the binary until every top-level test has a result.
func runTests(binary string) ([]result, []string) {
	var results []result
	var log []string
	finished := map[string]bool{}
	for range maxRuns {
		states, order, pkgOutput, err := runOnce(binary, finished)
		log = append(log, pkgOutput...)
		if err != nil {
			log = append(log, err.Error())
			break
		}
		crashed := !completed(pkgOutput)
		results = append(results, collect(states, order, crashed, crashMessage(states, order, pkgOutput))...)
		if !crashed || len(order) == 0 {
			break
		}
		// Every test that started has its result now, also the one that crashed.
		for _, name := range order {
			top, _, _ := strings.Cut(name, "/")
			finished[top] = true
		}
	}
	return results, log
}

// runOnce runs the binary once, skipping the finished tests, and groups the events by test.
func runOnce(binary string, finished map[string]bool) (map[string]*testState, []string, []string, error) {
	args := []string{"tool", "test2json", "-t", binary, "-test.v=test2json", "-test.timeout=" + testTimeout}
	if len(finished) > 0 {
		names := make([]string, 0, len(finished))
		for name := range finished {
			names = append(names, regexp.QuoteMeta(name))
		}
		slices.Sort(names)
		args = append(args, "-test.skip=^("+strings.Join(names, "|")+")$")
	}
	cmd := exec.Command("go", args...)
	var stdout bytes.Buffer
	cmd.Stdout = &stdout
	cmd.Stderr = &stdout
	if err := cmd.Run(); err != nil {
		if _, exited := err.(*exec.ExitError); !exited {
			return nil, nil, nil, fmt.Errorf("cannot run the tests: %w", err)
		}
	}

	states := map[string]*testState{}
	var order, pkgOutput []string
	scanner := bufio.NewScanner(&stdout)
	scanner.Buffer(make([]byte, 0, 64*1024), 4*1024*1024)
	for scanner.Scan() {
		var e event
		if json.Unmarshal(scanner.Bytes(), &e) != nil {
			pkgOutput = append(pkgOutput, scanner.Text())
			continue
		}
		if e.Test == "" {
			if e.Action == "output" {
				pkgOutput = append(pkgOutput, strings.TrimRight(e.Output, "\n"))
			}
			continue
		}
		state, ok := states[e.Test]
		if !ok {
			state = &testState{}
			states[e.Test] = state
			order = append(order, e.Test)
		}
		switch e.Action {
		case "output":
			state.output = append(state.output, strings.TrimRight(e.Output, "\n"))
		case "pass", "fail", "skip":
			state.ended = e.Action
		}
	}
	return states, order, pkgOutput, nil
}

// completed: a test binary that ran to the end prints PASS or FAIL on a line of its own.
func completed(pkgOutput []string) bool {
	return slices.ContainsFunc(pkgOutput, func(line string) bool {
		line = strings.TrimSpace(line)
		return line == "PASS" || line == "FAIL"
	})
}

// crashMessage explains why the binary stopped early: a panic, a timeout or a fatal error.
func crashMessage(states map[string]*testState, order []string, pkgOutput []string) string {
	var all []string
	for _, name := range order {
		all = append(all, states[name].output...)
	}
	all = append(all, pkgOutput...)
	for _, line := range all {
		line = strings.TrimSpace(line)
		switch {
		case strings.HasPrefix(line, "panic: test timed out"):
			return "timed out after " + testTimeout + ": look for a deadlock, a goroutine nobody stops or an endless loop"
		case strings.HasPrefix(line, "panic: "):
			return recovered.ReplaceAllString(strings.TrimPrefix(line, "panic: "), "")
		case strings.HasPrefix(line, "fatal error: "):
			return strings.TrimPrefix(line, "fatal ")
		}
	}
	return "the test binary stopped early"
}

// collect turns the states of one run into results: leaf tests only, plus a parent that
// failed by itself.
func collect(states map[string]*testState, order []string, crashed bool, crash string) []result {
	var results []result
	for _, name := range order {
		state := states[name]
		if state.ended == "skip" {
			continue
		}
		children := slices.ContainsFunc(order, func(other string) bool {
			return strings.HasPrefix(other, name+"/")
		})
		childFailed := slices.ContainsFunc(order, func(other string) bool {
			return strings.HasPrefix(other, name+"/") && states[other].ended != "pass"
		})
		if children && (state.ended == "pass" || childFailed) {
			continue
		}
		switch state.ended {
		case "pass":
			results = append(results, result{name: name, passed: true})
		case "fail":
			results = append(results, result{name: name, message: failureMessage(state.output, crashed, crash)})
		default:
			results = append(results, result{name: name, message: crash})
		}
	}
	return results
}

// failureMessage is what a failing test reported: its t.Error lines, a data race, or the
// panic that stopped it.
func failureMessage(output []string, crashed bool, crash string) string {
	text := strings.Join(output, "\n")
	if strings.Contains(text, "panic: ") || (crashed && !strings.Contains(text, ".go:")) {
		return crash
	}
	if strings.Contains(text, "race detected during execution of test") || strings.Contains(text, "WARNING: DATA RACE") {
		lines := []string{"data race: two goroutines used the same memory without synchronization"}
		seen := map[string]bool{}
		for _, frame := range kataFrame.FindAllString(text, -1) {
			frame = strings.TrimPrefix(frame, "/target/kata/")
			if !seen[frame] && !strings.Contains(frame, "_test.go") {
				seen[frame] = true
				lines = append(lines, "at "+frame)
			}
		}
		return strings.Join(head(lines, maxMessageLines), "\n")
	}
	var lines []string
	for _, line := range output {
		trimmed := strings.TrimSpace(line)
		if trimmed == "" || strings.HasPrefix(trimmed, "=== ") || strings.HasPrefix(trimmed, "--- ") {
			continue
		}
		lines = append(lines, strings.TrimSpace(fileLine.ReplaceAllString(line, "")))
	}
	if len(lines) == 0 {
		return "the test failed"
	}
	return strings.Join(head(lines, maxMessageLines), "\n")
}

// vetResult runs go vet over the package, tests included.
func vetResult() result {
	out, err := exec.Command("go", "vet", "./...").CombinedOutput()
	if err == nil {
		return result{name: "go_vet", passed: true}
	}
	var lines []string
	for line := range strings.Lines(string(out)) {
		line = strings.TrimSpace(line)
		if line != "" && !strings.HasPrefix(line, "#") {
			lines = append(lines, strings.TrimPrefix(line, "./"))
		}
	}
	return result{name: "go_vet", message: strings.Join(head(lines, maxMessageLines), "\n")}
}

// gofmtResults checks that every source file, tests excluded, is formatted with gofmt.
func gofmtResults() []result {
	files, _ := filepath.Glob("*.go")
	slices.Sort(files)
	var results []result
	for _, file := range files {
		if strings.HasSuffix(file, "_test.go") {
			continue
		}
		name := "gofmt::" + strings.ReplaceAll(file, ".", "_")
		source, err := os.ReadFile(file)
		if err != nil {
			results = append(results, result{name: name, message: err.Error()})
			continue
		}
		formatted, err := format.Source(source)
		if err != nil || !bytes.Equal(formatted, source) {
			results = append(results, result{name: name, message: fmt.Sprintf(
				"%s is not formatted: run `gofmt -w %s` (VS Code's Go extension formats on save)", file, file)})
			continue
		}
		results = append(results, result{name: name, passed: true})
	}
	return results
}

// printCargo prints the results the way `cargo test` does and returns the exit code.
func printCargo(results []result) int {
	fmt.Printf("\nrunning %d tests\n", len(results))
	failed := 0
	for _, r := range results {
		status := "ok"
		if !r.passed {
			status = "FAILED"
			failed++
		}
		fmt.Printf("test %s ... %s\n", r.name, status)
	}
	passed := len(results) - failed
	if failed == 0 {
		fmt.Printf("\ntest result: ok. %d passed; 0 failed\n", passed)
		return 0
	}
	fmt.Print("\nfailures:\n\n")
	for _, r := range results {
		if r.passed {
			continue
		}
		fmt.Printf("---- %s stdout ----\n", r.name)
		fmt.Printf("thread '%s' panicked at %s:\n", r.name, r.name)
		fmt.Printf("%s\n\n", r.message)
	}
	fmt.Printf("failures:\n\ntest result: FAILED. %d passed; %d failed\n", passed, failed)
	return 1
}

func head(lines []string, n int) []string {
	if len(lines) > n {
		return lines[:n]
	}
	return lines
}
