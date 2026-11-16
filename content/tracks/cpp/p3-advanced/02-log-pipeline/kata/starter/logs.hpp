#pragma once

#include <cstddef>
#include <generator>
#include <map>
#include <optional>
#include <string>
#include <string_view>
#include <vector>

namespace dojo {

enum class Level { Debug, Info, Warn, Error };

struct Entry {
    long timestamp = 0;
    Level level = Level::Info;
    std::string service;
    int latency_ms = 0;
    std::string message;

    bool operator==(const Entry&) const = default;
};

// Each line of the text without its '\n' (and a trailing '\r'), skipping blank lines.
// The views point into text, so text must outlive them.
std::generator<std::string_view> lines(std::string_view text);

// Reads "TIMESTAMP LEVEL SERVICE LATENCYms MESSAGE...", for example
// "1700000000 ERROR billing 250ms card declined". LEVEL is DEBUG, INFO, WARN or ERROR; the
// message is the rest of the line after one space (it may be empty). Nothing for any other
// line: missing fields, a bad number, a latency without "ms" or an unknown level.
std::optional<Entry> parse_entry(std::string_view line);

// Every well-formed entry of the text, in order: lines | parse | keep the good ones.
std::vector<Entry> parse_all(std::string_view text);

// How many ERROR entries each service has (services without errors are left out).
std::map<std::string, int> errors_by_service(const std::vector<Entry>& entries);

// The n slowest entries: highest latency first, the earlier timestamp first on a tie.
std::vector<Entry> slowest(const std::vector<Entry>& entries, std::size_t n);

// The services with at least one entry at `level` or above, each once, sorted.
std::vector<std::string> services_at_least(const std::vector<Entry>& entries, Level level);

// The mean of each run of `window` consecutive values: {1, 2, 3, 4} with window 2 gives
// {1.5, 2.5, 3.5}. Empty when window is 0 or larger than the number of values.
std::vector<double> moving_average(const std::vector<int>& values, std::size_t window);

}  // namespace dojo
