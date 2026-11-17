// Command backup copies a folder to a backup folder, incrementally.
package main

import (
	"os"

	"dojo/backup"
)

func main() {
	os.Exit(backup.Run(os.Args[1:], os.Stdout, os.Stderr))
}
