# Sidequest: Unit converter

Build `units`, a small command-line converter. The module is `dojo/units`:

- the package `units` (the files at the top of the folder) holds the logic, and its entry
  point is fixed: `func Run(args []string, stdout, stderr io.Writer) int` returns the exit
  code and writes everything it prints to the two writers;
- `cmd/units/main.go` (already there) only calls `units.Run(os.Args[1:], os.Stdout,
  os.Stderr)` and exits with its code, so `go run ./cmd/units convert 10 km m` works.

The contract tests call `Run` with arguments and read what it wrote, exactly what a user would
see. Types for units and categories, a table of factors, errors as values: the design is
yours. The dojo also runs `go vet` and `gofmt` on your files.
