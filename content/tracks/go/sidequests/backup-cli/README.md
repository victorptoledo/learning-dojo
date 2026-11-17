# Sidequest: Backup CLI

Build `backup`, a small cousin of `rsync`: it copies a folder into a backup folder, copies
only what changed the next time, and can prove and restore the copy. The module is
`dojo/backup`:

- the package `backup` (the files at the top of the folder) has the fixed entry point
  `func Run(args []string, stdout, stderr io.Writer) int`, which returns the exit code;
- `cmd/backup/main.go` (already there) calls it, so `go run ./cmd/backup run ~/notes /mnt/usb/notes`
  works.

The contract tests build folders in a temporary directory, call `Run`, and check the files
on disk, their permissions and times, and what was printed. Paths in the output are relative
to the folder being backed up, use `/`, and come sorted. The tests run on Linux.

The design is yours: `filepath.WalkDir`, `io.Copy`, `os.Chmod`, `os.Chtimes` and an
interface or two will take you far.
