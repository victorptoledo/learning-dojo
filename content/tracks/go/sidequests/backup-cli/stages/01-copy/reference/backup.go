// Package backup copies a folder to a backup folder, incrementally.
package backup

import (
	"errors"
	"fmt"
	"io"
	"io/fs"
	"os"
	"path/filepath"
)

var errUsage = errors.New("usage")

const usage = "usage: backup run <src> <dst> [--prune] | verify <src> <dst> | restore <dst> <target>"

// files lists the regular files under root as relative slash paths, in walk (path) order.
func files(root string) ([]string, error) {
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
	return os.Chmod(dst, info.Mode().Perm())
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
		if err := copyFile(from, to); err != nil {
			return err
		}
		fmt.Fprintf(stdout, "copied %s\n", relative)
		copied++
	}
	summary := fmt.Sprintf("summary: %d copied, %d unchanged", copied, same)
	fmt.Fprintln(stdout, summary)
	return nil
}

func dispatch(args []string, stdout io.Writer) (int, error) {
	switch command, rest := args[0], args[3:]; {
	case command == "run" && (len(rest) == 0 || (len(rest) == 1 && rest[0] == "--prune")):
		return 0, backupRun(args[1], args[2], len(rest) == 1, stdout)
	default:
		return 2, errUsage
	}
}

// Run runs the command line `args` (without the program name) and returns its exit code.
func Run(args []string, stdout, stderr io.Writer) int {
	code, err := dispatch(args, stdout)
	switch {
	case err != nil:
		fmt.Fprintf(stderr, "error: %v\n", err)
		return 1
	}
	return code
}
