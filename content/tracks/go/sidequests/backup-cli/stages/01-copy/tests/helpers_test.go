package backup_test

import (
	"bytes"
	"io/fs"
	"os"
	"path/filepath"
	"slices"
	"strings"
	"testing"

	"dojo/backup"
)

// run calls backup.Run and returns the exit code, stdout lines and stderr (trimmed).
func run(args ...string) (int, []string, string) {
	var stdout, stderr bytes.Buffer
	code := backup.Run(args, &stdout, &stderr)
	out := strings.TrimSpace(stdout.String())
	var lines []string
	if out != "" {
		lines = strings.Split(out, "\n")
	}
	return code, lines, strings.TrimSpace(stderr.String())
}

// write creates `files` (relative path -> content) under dir.
func write(t *testing.T, dir string, files map[string]string) {
	t.Helper()
	for name, content := range files {
		path := filepath.Join(dir, filepath.FromSlash(name))
		if err := os.MkdirAll(filepath.Dir(path), 0o755); err != nil {
			t.Fatal(err)
		}
		if err := os.WriteFile(path, []byte(content), 0o644); err != nil {
			t.Fatal(err)
		}
	}
}

// tree returns every file under dir as relative path -> content.
func tree(t *testing.T, dir string) map[string]string {
	t.Helper()
	files := map[string]string{}
	err := filepath.WalkDir(dir, func(path string, entry fs.DirEntry, err error) error {
		if err != nil || entry.IsDir() {
			return err
		}
		data, err := os.ReadFile(path)
		if err != nil {
			return err
		}
		relative, _ := filepath.Rel(dir, path)
		files[filepath.ToSlash(relative)] = string(data)
		return nil
	})
	if err != nil && !os.IsNotExist(err) {
		t.Fatal(err)
	}
	return files
}

func expectLines(t *testing.T, got, want []string) {
	t.Helper()
	if !slices.Equal(got, want) {
		t.Errorf("printed\n  %q\nwant\n  %q", got, want)
	}
}

func expectTree(t *testing.T, got, want map[string]string) {
	t.Helper()
	if len(got) != len(want) {
		t.Errorf("files %v, want %v", keys(got), keys(want))
		return
	}
	for name, content := range want {
		if got[name] != content {
			t.Errorf("%s holds %q, want %q (files: %v)", name, got[name], content, keys(got))
		}
	}
}

func keys(files map[string]string) []string {
	var names []string
	for name := range files {
		names = append(names, name)
	}
	slices.Sort(names)
	return names
}
