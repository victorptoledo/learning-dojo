package backup_test

import (
	"os"
	"path/filepath"
	"testing"
	"time"
)

func TestVerifySaysOkWhenTheTreesMatch(t *testing.T) {
	src, dst := t.TempDir(), t.TempDir()
	write(t, src, map[string]string{"a": "1", "b/c": "2", "b/d": "3"})
	run("run", src, dst)
	code, lines, _ := run("verify", src, dst)
	if code != 0 {
		t.Fatalf("exit code %d, printed %q", code, lines)
	}
	expectLines(t, lines, []string{"ok: 3 files match"})
}

func TestVerifyListsEveryDifferenceInPathOrder(t *testing.T) {
	src, dst := t.TempDir(), t.TempDir()
	write(t, src, map[string]string{"a": "1", "b": "same size", "c": "3", "e": "5"})
	run("run", src, dst)
	// Same size and time on purpose: verify compares content, not metadata.
	info, _ := os.Stat(filepath.Join(dst, "b"))
	write(t, dst, map[string]string{"b": "SAME SIZE", "d": "extra"})
	os.Chtimes(filepath.Join(dst, "b"), info.ModTime(), info.ModTime())
	os.Remove(filepath.Join(dst, "c"))
	code, lines, _ := run("verify", src, dst)
	if code != 1 {
		t.Errorf("exit code %d, want 1", code)
	}
	expectLines(t, lines, []string{"differs b", "missing c", "extra d"})
}

func TestRestoreBringsBackAnIdenticalTree(t *testing.T) {
	src, dst := t.TempDir(), t.TempDir()
	target := filepath.Join(t.TempDir(), "restored")
	files := map[string]string{"notes.txt": "n", "bin/tool": "#!/bin/sh\n", "deep/a/b/c.txt": "c"}
	write(t, src, files)
	os.Chmod(filepath.Join(src, "bin/tool"), 0o750)
	old := time.Date(2021, 6, 7, 8, 9, 10, 0, time.UTC)
	os.Chtimes(filepath.Join(src, "notes.txt"), old, old)
	run("run", src, dst)

	code, lines, errOut := run("restore", dst, target)
	if code != 0 {
		t.Fatalf("exit code %d, stderr %q", code, errOut)
	}
	expectLines(t, lines, []string{"restored 3 files"})
	expectTree(t, tree(t, target), files)
	if info, _ := os.Stat(filepath.Join(target, "bin/tool")); info.Mode().Perm() != 0o750 {
		t.Errorf("bin/tool has mode %o, want 750", info.Mode().Perm())
	}
	if info, _ := os.Stat(filepath.Join(target, "notes.txt")); !info.ModTime().Equal(old) {
		t.Errorf("notes.txt time %v, want %v", info.ModTime(), old)
	}
	if code, lines, _ := run("verify", src, target); code != 0 {
		t.Errorf("the restored tree does not verify: %q", lines)
	}
}

func TestRestoreIntoAnEmptyFolderWorks(t *testing.T) {
	src, dst, target := t.TempDir(), t.TempDir(), t.TempDir()
	write(t, src, map[string]string{"a": "1"})
	run("run", src, dst)
	if code, _, errOut := run("restore", dst, target); code != 0 {
		t.Fatalf("exit code %d, stderr %q", code, errOut)
	}
	expectTree(t, tree(t, target), map[string]string{"a": "1"})
}

func TestRestoreRefusesAFolderThatIsNotEmpty(t *testing.T) {
	src, dst, target := t.TempDir(), t.TempDir(), t.TempDir()
	write(t, src, map[string]string{"a": "new"})
	run("run", src, dst)
	write(t, target, map[string]string{"precious": "do not touch"})
	code, _, errOut := run("restore", dst, target)
	if code != 1 || errOut != "error: target is not empty" {
		t.Errorf("code %d, stderr %q", code, errOut)
	}
	expectTree(t, tree(t, target), map[string]string{"precious": "do not touch"})
}
