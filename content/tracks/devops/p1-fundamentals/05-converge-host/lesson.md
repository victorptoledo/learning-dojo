# Converge a Host

Build the heart of a configuration management tool: a bash script that reads a desired
state (directories, rendered files, lines, absent files), changes only what differs, and has
a `--check` mode that detects drift without touching anything. Press **Enter** to create the
kata folder and open it, edit `converge.sh` and save. The tests run it twice on throwaway
folders: the second run must change nothing.
