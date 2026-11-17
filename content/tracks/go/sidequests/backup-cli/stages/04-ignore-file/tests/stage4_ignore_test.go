package backup_test

import (
	"os"
	"path/filepath"
	"testing"
)

const rules = `# logs, except one
*.log
!keep.log

build/
/secret.txt
docs/*.tmp
`

func source(t *testing.T) string {
	src := t.TempDir()
	write(t, src, map[string]string{
		".backupignore":      rules,
		"app.go":             "package app",
		"debug.log":          "noise",
		"logs/old.log":       "noise",
		"logs/keep.log":      "kept",
		"build/app":          "binary",
		"tools/build/x":      "binary",
		"secret.txt":         "top secret",
		"notes/secret.txt":   "not the root one",
		"docs/draft.tmp":     "tmp",
		"docs/guide.md":      "guide",
		"docs/deeper/a.tmp":  "not docs/*.tmp",
		"buildings/plan.txt": "not a build folder",
	})
	return src
}

func TestIgnoredPathsAreNotCopied(t *testing.T) {
	src, dst := source(t), t.TempDir()
	code, lines, errOut := run("run", src, dst)
	if code != 0 {
		t.Fatalf("exit code %d, stderr %q", code, errOut)
	}
	expectTree(t, tree(t, dst), map[string]string{
		".backupignore":      rules,
		"app.go":             "package app",
		"logs/keep.log":      "kept",
		"notes/secret.txt":   "not the root one",
		"docs/guide.md":      "guide",
		"docs/deeper/a.tmp":  "not docs/*.tmp",
		"buildings/plan.txt": "not a build folder",
	})
	if len(lines) == 0 || lines[len(lines)-1] != "summary: 7 copied, 0 unchanged" {
		t.Errorf("printed %q", lines)
	}
}

func TestAFileInsideAnIgnoredFolderCannotComeBack(t *testing.T) {
	src, dst := t.TempDir(), t.TempDir()
	write(t, src, map[string]string{".backupignore": "cache/\n!cache/index.txt\n", "cache/index.txt": "x", "a": "1"})
	run("run", src, dst)
	if _, ok := tree(t, dst)["cache/index.txt"]; ok {
		t.Error("cache/index.txt was copied although its folder is ignored")
	}
}

func TestPruneAndVerifyLeaveIgnoredPathsAlone(t *testing.T) {
	src, dst := source(t), t.TempDir()
	run("run", src, dst)
	write(t, dst, map[string]string{"debug.log": "an old copy"})
	_, lines, _ := run("run", src, dst, "--prune")
	if _, ok := tree(t, dst)["debug.log"]; !ok {
		t.Errorf("--prune removed an ignored path: %q", lines)
	}
	os.Remove(filepath.Join(dst, "debug.log"))
	if code, lines, _ := run("verify", src, dst); code != 0 {
		t.Errorf("verify reported ignored paths: %q", lines)
	}
}
