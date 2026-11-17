package backup_test

import (
	"path/filepath"
	"testing"
)

func TestRunCopiesTheWholeTree(t *testing.T) {
	src, dst := t.TempDir(), filepath.Join(t.TempDir(), "backup")
	files := map[string]string{
		"notes.txt":           "remember",
		"photos/cat.jpg":      "meow",
		"photos/2026/dog.jpg": "woof",
	}
	write(t, src, files)
	code, lines, errOut := run("run", src, dst)
	if code != 0 {
		t.Fatalf("exit code %d, stderr %q", code, errOut)
	}
	expectTree(t, tree(t, dst), files)
	expectLines(t, lines, []string{
		"copied notes.txt",
		"copied photos/2026/dog.jpg",
		"copied photos/cat.jpg",
		"summary: 3 copied, 0 unchanged",
	})
}

func TestTheDestinationFoldersAreCreated(t *testing.T) {
	src := t.TempDir()
	dst := filepath.Join(t.TempDir(), "deep", "down", "backup")
	write(t, src, map[string]string{"a": "1"})
	if code, _, errOut := run("run", src, dst); code != 0 {
		t.Fatalf("exit code %d, stderr %q", code, errOut)
	}
	expectTree(t, tree(t, dst), map[string]string{"a": "1"})
}

func TestBinaryAndEmptyFilesAreCopiedExactly(t *testing.T) {
	src, dst := t.TempDir(), t.TempDir()
	files := map[string]string{"empty": "", "bytes.bin": "\x00\xff\x10binary\n\r\n"}
	write(t, src, files)
	run("run", src, dst)
	expectTree(t, tree(t, dst), files)
}

func TestAnEmptySourceCopiesNothing(t *testing.T) {
	code, lines, _ := run("run", t.TempDir(), t.TempDir())
	if code != 0 {
		t.Fatalf("exit code %d", code)
	}
	expectLines(t, lines, []string{"summary: 0 copied, 0 unchanged"})
}
