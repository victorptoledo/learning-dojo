// Package backup copies a folder to a backup folder, incrementally.
package backup

import (
	"bytes"
	"errors"
	"fmt"
	"io"
	"io/fs"
	"os"
	"path"
	"path/filepath"
	"sort"
	"strings"
)

var errUsage = errors.New("usage")

const usage = "usage: backup run <src> <dst> [--prune] | verify <src> <dst> | restore <dst> <target>"

// rule is one line of a .backupignore.
type rule struct {
	pattern  string
	negate   bool
	dirOnly  bool
	anchored bool
}

func loadRules(src string) ([]rule, error) {
	data, err := os.ReadFile(filepath.Join(src, ".backupignore"))
	if errors.Is(err, fs.ErrNotExist) {
		return nil, nil
	}
	if err != nil {
		return nil, err
	}
	var rules []rule
	for _, line := range strings.Split(string(data), "\n") {
		line = strings.TrimSpace(line)
		if line == "" || strings.HasPrefix(line, "#") {
			continue
		}
		var r rule
		if strings.HasPrefix(line, "!") {
			r.negate, line = true, line[1:]
		}
		if strings.HasSuffix(line, "/") {
			r.dirOnly, line = true, strings.TrimSuffix(line, "/")
		}
		if strings.Contains(line, "/") {
			r.anchored, line = true, strings.TrimPrefix(line, "/")
		}
		r.pattern = line
		rules = append(rules, r)
	}
	return rules, nil
}

// ignored says whether the path (relative, with /) is left out; the last matching rule wins.
func ignored(rules []rule, relative string, isDir bool) bool {
	out := false
	for _, r := range rules {
		if r.dirOnly && !isDir {
			continue
		}
		subject := path.Base(relative)
		if r.anchored {
			subject = relative
		}
		if ok, _ := path.Match(r.pattern, subject); ok {
			out = !r.negate
		}
	}
	return out
}

// files lists the regular files under root as relative slash paths, in walk (path) order.
func files(root string) ([]string, error) {
	rules, rulesErr := loadRules(root)
	if rulesErr != nil {
		return nil, rulesErr
	}
	var found []string
	err := filepath.WalkDir(root, func(full string, entry fs.DirEntry, err error) error {
		if err != nil {
			return err
		}
		relative, _ := filepath.Rel(root, full)
		relative = filepath.ToSlash(relative)
		if relative == "." {
			return nil
		}
		if ignored(rules, relative, entry.IsDir()) {
			if entry.IsDir() {
				return filepath.SkipDir
			}
			return nil
		}
		if entry.Type().IsRegular() {
			found = append(found, relative)
		}
		return nil
	})
	return found, err
}

// copyFile copies src to dst with its bytes, permission bits and modification time.
func copyFile(src, dst string) error {
	info, err := os.Stat(src)
	if err != nil {
		return err
	}
	if err := os.MkdirAll(filepath.Dir(dst), 0o755); err != nil {
		return err
	}
	in, err := os.Open(src)
	if err != nil {
		return err
	}
	defer in.Close()
	out, err := os.OpenFile(dst, os.O_WRONLY|os.O_CREATE|os.O_TRUNC, 0o600)
	if err != nil {
		return err
	}
	if _, err := io.Copy(out, in); err != nil {
		out.Close()
		return err
	}
	if err := out.Close(); err != nil {
		return err
	}
	if err := os.Chmod(dst, info.Mode().Perm()); err != nil {
		return err
	}
	return os.Chtimes(dst, info.ModTime(), info.ModTime())
}

func unchanged(src, dst string) bool {
	a, errA := os.Stat(src)
	b, errB := os.Stat(dst)
	return errA == nil && errB == nil && a.Size() == b.Size() && a.ModTime().Equal(b.ModTime())
}

func backupRun(src, dst string, prune bool, stdout io.Writer) error {
	info, err := os.Stat(src)
	if err != nil {
		return err
	}
	if !info.IsDir() {
		return fmt.Errorf("%s is not a folder", src)
	}
	list, err := files(src)
	if err != nil {
		return err
	}
	copied, same := 0, 0
	for _, relative := range list {
		from, to := filepath.Join(src, relative), filepath.Join(dst, filepath.FromSlash(relative))
		if unchanged(from, to) {
			same++
			continue
		}
		if err := copyFile(from, to); err != nil {
			return err
		}
		fmt.Fprintf(stdout, "copied %s\n", relative)
		copied++
	}
	summary := fmt.Sprintf("summary: %d copied, %d unchanged", copied, same)
	if prune {
		removed, err := pruneExtra(list, dst, stdout)
		if err != nil {
			return err
		}
		summary += fmt.Sprintf(", %d removed", removed)
	}
	fmt.Fprintln(stdout, summary)
	return nil
}

func pruneExtra(kept []string, dst string, stdout io.Writer) (int, error) {
	inSource := map[string]bool{}
	for _, relative := range kept {
		inSource[relative] = true
	}
	existing, err := files(dst)
	if err != nil {
		return 0, err
	}
	removed := 0
	for _, relative := range existing {
		if inSource[relative] {
			continue
		}
		if err := os.Remove(filepath.Join(dst, filepath.FromSlash(relative))); err != nil {
			return removed, err
		}
		fmt.Fprintf(stdout, "removed %s\n", relative)
		removed++
	}
	return removed, nil
}

func verify(src, dst string, stdout io.Writer) (bool, error) {
	left, err := files(src)
	if err != nil {
		return false, err
	}
	right, err := files(dst)
	if err != nil {
		return false, err
	}
	inRight := map[string]bool{}
	for _, relative := range right {
		inRight[relative] = true
	}
	inLeft := map[string]bool{}
	all := map[string]bool{}
	for _, relative := range left {
		inLeft[relative] = true
		all[relative] = true
	}
	for _, relative := range right {
		all[relative] = true
	}
	var names []string
	for name := range all {
		names = append(names, name)
	}
	sort.Strings(names)
	differences := 0
	for _, name := range names {
		switch {
		case !inRight[name]:
			fmt.Fprintf(stdout, "missing %s\n", name)
		case !inLeft[name]:
			fmt.Fprintf(stdout, "extra %s\n", name)
		default:
			a, errA := os.ReadFile(filepath.Join(src, name))
			b, errB := os.ReadFile(filepath.Join(dst, name))
			if errA == nil && errB == nil && bytes.Equal(a, b) {
				continue
			}
			fmt.Fprintf(stdout, "differs %s\n", name)
		}
		differences++
	}
	if differences == 0 {
		fmt.Fprintf(stdout, "ok: %d files match\n", len(left))
	}
	return differences == 0, nil
}

func restore(dst, target string, stdout io.Writer) error {
	entries, err := os.ReadDir(target)
	if err == nil && len(entries) > 0 {
		return errors.New("target is not empty")
	}
	list, err := files(dst)
	if err != nil {
		return err
	}
	for _, relative := range list {
		if err := copyFile(filepath.Join(dst, relative), filepath.Join(target, filepath.FromSlash(relative))); err != nil {
			return err
		}
	}
	fmt.Fprintf(stdout, "restored %d files\n", len(list))
	return nil
}

func dispatch(args []string, stdout io.Writer) (int, error) {
	if len(args) < 3 {
		return 2, errUsage
	}
	switch command, rest := args[0], args[3:]; {
	case command == "run" && (len(rest) == 0 || (len(rest) == 1 && rest[0] == "--prune")):
		return 0, backupRun(args[1], args[2], len(rest) == 1, stdout)
	case command == "verify" && len(rest) == 0:
		ok, err := verify(args[1], args[2], stdout)
		if err == nil && !ok {
			return 1, nil
		}
		return 0, err
	case command == "restore" && len(rest) == 0:
		return 0, restore(args[1], args[2], stdout)
	default:
		return 2, errUsage
	}
}

// Run runs the command line `args` (without the program name) and returns its exit code.
func Run(args []string, stdout, stderr io.Writer) int {
	code, err := dispatch(args, stdout)
	switch {
	case errors.Is(err, errUsage):
		fmt.Fprintln(stderr, usage)
		return 2
	case err != nil:
		fmt.Fprintf(stderr, "error: %v\n", err)
		return 1
	}
	return code
}
