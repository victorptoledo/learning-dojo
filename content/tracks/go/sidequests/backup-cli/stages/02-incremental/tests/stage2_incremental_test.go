package backup_test

import (
	"os"
	"path/filepath"
	"strings"
	"testing"
	"time"
)

func TestASecondRunCopiesNothing(t *testing.T) {
	src, dst := t.TempDir(), t.TempDir()
	write(t, src, map[string]string{"a.txt": "a", "sub/b.txt": "b"})
	run("run", src, dst)
	code, lines, _ := run("run", src, dst)
	if code != 0 {
		t.Fatalf("exit code %d", code)
	}
	expectLines(t, lines, []string{"summary: 0 copied, 2 unchanged"})
}

func TestOnlyChangedFilesAreCopiedAgain(t *testing.T) {
	src, dst := t.TempDir(), t.TempDir()
	write(t, src, map[string]string{"a.txt": "a", "b.txt": "b", "c.txt": "c"})
	run("run", src, dst)
	write(t, src, map[string]string{"b.txt": "bigger now", "new.txt": "n"})
	// Same size, later time: still a change.
	write(t, src, map[string]string{"c.txt": "C"})
	later := time.Now().Add(time.Hour)
	if err := os.Chtimes(filepath.Join(src, "c.txt"), later, later); err != nil {
		t.Fatal(err)
	}
	_, lines, _ := run("run", src, dst)
	expectLines(t, lines, []string{"copied b.txt", "copied c.txt", "copied new.txt", "summary: 3 copied, 1 unchanged"})
	expectTree(t, tree(t, dst), map[string]string{"a.txt": "a", "b.txt": "bigger now", "c.txt": "C", "new.txt": "n"})
}

func TestPermissionsAndTimesAreKept(t *testing.T) {
	src, dst := t.TempDir(), t.TempDir()
	write(t, src, map[string]string{"run.sh": "#!/bin/sh\n", "key": "secret"})
	if err := os.Chmod(filepath.Join(src, "run.sh"), 0o755); err != nil {
		t.Fatal(err)
	}
	if err := os.Chmod(filepath.Join(src, "key"), 0o600); err != nil {
		t.Fatal(err)
	}
	old := time.Date(2020, 1, 2, 3, 4, 5, 0, time.UTC)
	if err := os.Chtimes(filepath.Join(src, "key"), old, old); err != nil {
		t.Fatal(err)
	}
	run("run", src, dst)
	for name, mode := range map[string]os.FileMode{"run.sh": 0o755, "key": 0o600} {
		info, err := os.Stat(filepath.Join(dst, name))
		if err != nil {
			t.Fatal(err)
		}
		if info.Mode().Perm() != mode {
			t.Errorf("%s has mode %o, want %o", name, info.Mode().Perm(), mode)
		}
	}
	info, _ := os.Stat(filepath.Join(dst, "key"))
	if !info.ModTime().Equal(old) {
		t.Errorf("key was modified at %v, want %v", info.ModTime(), old)
	}
}

func TestPruneRemovesWhatTheSourceLost(t *testing.T) {
	src, dst := t.TempDir(), t.TempDir()
	write(t, src, map[string]string{"keep.txt": "k", "old/draft.txt": "d", "gone.txt": "g"})
	run("run", src, dst)
	os.Remove(filepath.Join(src, "gone.txt"))
	os.RemoveAll(filepath.Join(src, "old"))
	write(t, src, map[string]string{"fresh.txt": "f"})
	_, lines, _ := run("run", src, dst, "--prune")
	expectLines(t, lines, []string{
		"copied fresh.txt",
		"removed gone.txt",
		"removed old/draft.txt",
		"summary: 1 copied, 1 unchanged, 2 removed",
	})
	expectTree(t, tree(t, dst), map[string]string{"keep.txt": "k", "fresh.txt": "f"})
}

func TestWithoutPruneNothingIsRemoved(t *testing.T) {
	src, dst := t.TempDir(), t.TempDir()
	write(t, src, map[string]string{"a": "1"})
	write(t, dst, map[string]string{"only-in-backup": "x"})
	_, lines, _ := run("run", src, dst)
	expectLines(t, lines, []string{"copied a", "summary: 1 copied, 0 unchanged"})
	if _, ok := tree(t, dst)["only-in-backup"]; !ok {
		t.Error("a file was removed without --prune")
	}
}

func TestBadCommandLinesAreUsageErrors(t *testing.T) {
	src := t.TempDir()
	for _, args := range [][]string{{}, {"run"}, {"run", src}, {"copy", src, src}, {"run", src, src, "--force"}} {
		code, lines, errOut := run(args...)
		if code != 2 || len(lines) != 0 || !strings.HasPrefix(errOut, "usage: backup") {
			t.Errorf("backup %q: code %d, stdout %q, stderr %q; want code 2 and usage", args, code, lines, errOut)
		}
	}
}

func TestAMissingSourceIsAnError(t *testing.T) {
	code, _, errOut := run("run", filepath.Join(t.TempDir(), "nope"), t.TempDir())
	if code != 1 || !strings.HasPrefix(errOut, "error: ") {
		t.Errorf("code %d, stderr %q; want code 1 and error: ...", code, errOut)
	}
}
