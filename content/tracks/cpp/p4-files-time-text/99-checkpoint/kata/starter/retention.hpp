#pragma once

#include <chrono>
#include <cstddef>
#include <expected>
#include <filesystem>
#include <string>
#include <system_error>
#include <vector>

namespace dojo {

// One backup file, named <name>-YYYYMMDD-HHMM.bak after the local wall time it was taken.
struct Backup {
    std::filesystem::path path;
    std::chrono::sys_seconds taken;  // the moment, in UTC
    std::chrono::local_days day;     // the local date in the name
};

enum class ScanError {
    NotADirectory,  // the folder does not exist, or is not a folder
    Unreadable,     // the folder cannot be listed
};

struct Scan {
    std::vector<Backup> backups;                 // newest first
    std::vector<std::filesystem::path> skipped;  // other regular files, sorted by path
};

// Reads the backups in `dir` (not in its sub-folders). The names hold local wall times in
// `zone`: a time that happens twice (when the clocks go back) is the earlier of the two, and a
// time that never happens (when the clocks go forward) makes the file skipped, like a regular
// file whose name is not a backup's. Sub-folders and other non-regular entries are ignored.
std::expected<Scan, ScanError> scan(const std::filesystem::path& dir,
                                    const std::chrono::time_zone* zone);

// How many backups to keep: the newest backup of each of the last `daily` local days that
// have backups, of each of the last `weekly` ISO weeks (Monday first, weeks counted by their
// ISO year), and of each of the last `monthly` months. 0 turns a rule off.
struct Policy {
    int daily = 7;
    int weekly = 4;
    int monthly = 6;
};

struct Decision {
    std::filesystem::path path;
    std::string reason;  // the rules that keep it, in the order "daily+weekly+monthly"
};

struct Plan {
    std::vector<Decision> keep;                 // newest first
    std::vector<std::filesystem::path> remove;  // newest first
};

// Decides which backups to keep. `backups` may be in any order.
Plan plan(std::vector<Backup> backups, const Policy& policy);

// The report written by apply, one line per file and a summary:
//   "keep   db-20261126-0300.bak  daily+weekly+monthly\n"
//   "remove db-20261101-0300.bak\n"
//   "2 kept, 1 removed\n"
// The action is padded to 6 columns, then one space; a kept file's name is padded to the
// longest kept file name, then two spaces and the reason. Only file names are shown.
std::string report(const Plan& plan);

// Removes the files in plan.remove (a file that is already gone is fine) and then replaces
// `dir / "retention.log"` with report(plan) atomically, through a temporary file in `dir`.
// Returns how many files it removed, or the first error (nothing more is removed after it,
// and the log is not written).
std::expected<std::size_t, std::error_code> apply(const std::filesystem::path& dir,
                                                  const Plan& plan);

}  // namespace dojo
