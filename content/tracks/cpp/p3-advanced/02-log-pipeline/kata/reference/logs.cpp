#include "logs.hpp"

#include <algorithm>
#include <charconv>
#include <numeric>
#include <ranges>
#include <set>

namespace dojo {

namespace {

template <typename T>
std::optional<T> to_number(std::string_view text) {
    T value{};
    const auto [stop, error] = std::from_chars(text.data(), text.data() + text.size(), value);
    if (text.empty() || error != std::errc{} || stop != text.data() + text.size()) {
        return std::nullopt;
    }
    return value;
}

std::optional<Level> to_level(std::string_view text) {
    if (text == "DEBUG") {
        return Level::Debug;
    }
    if (text == "INFO") {
        return Level::Info;
    }
    if (text == "WARN") {
        return Level::Warn;
    }
    if (text == "ERROR") {
        return Level::Error;
    }
    return std::nullopt;
}

// The next space-separated field of rest, removed from it.
std::string_view next_field(std::string_view& rest) {
    const auto space = rest.find(' ');
    const auto field = rest.substr(0, space);
    rest.remove_prefix(space == std::string_view::npos ? rest.size() : space + 1);
    return field;
}

}  // namespace

std::generator<std::string_view> lines(std::string_view text) {
    while (!text.empty()) {
        const auto newline = text.find('\n');
        std::string_view line = text.substr(0, newline);
        text.remove_prefix(newline == std::string_view::npos ? text.size() : newline + 1);
        if (line.ends_with('\r')) {
            line.remove_suffix(1);
        }
        if (line.find_first_not_of(" \t") != std::string_view::npos) {
            co_yield line;
        }
    }
}

std::optional<Entry> parse_entry(std::string_view line) {
    std::string_view rest = line;
    const auto timestamp = to_number<long>(next_field(rest));
    const auto level = to_level(next_field(rest));
    const auto service = next_field(rest);
    auto latency = next_field(rest);
    if (!timestamp || !level || service.empty() || !latency.ends_with("ms")) {
        return std::nullopt;
    }
    latency.remove_suffix(2);
    const auto latency_ms = to_number<int>(latency);
    if (!latency_ms) {
        return std::nullopt;
    }
    return Entry{*timestamp, *level, std::string(service), *latency_ms, std::string(rest)};
}

std::vector<Entry> parse_all(std::string_view text) {
    return lines(text) | std::views::transform(parse_entry) |
           std::views::filter([](const auto& entry) { return entry.has_value(); }) |
           std::views::transform([](const auto& entry) { return *entry; }) |
           std::ranges::to<std::vector>();
}

std::map<std::string, int> errors_by_service(const std::vector<Entry>& entries) {
    std::map<std::string, int> counts;
    for (const Entry& entry :
         entries | std::views::filter([](const Entry& e) { return e.level == Level::Error; })) {
        ++counts[entry.service];
    }
    return counts;
}

std::vector<Entry> slowest(const std::vector<Entry>& entries, std::size_t n) {
    std::vector<Entry> ranked = entries;
    const auto keep = std::min(n, ranked.size());
    std::ranges::partial_sort(ranked, ranked.begin() + static_cast<std::ptrdiff_t>(keep),
                              [](const Entry& a, const Entry& b) {
                                  if (a.latency_ms != b.latency_ms) {
                                      return a.latency_ms > b.latency_ms;
                                  }
                                  return a.timestamp < b.timestamp;
                              });
    ranked.resize(keep);
    return ranked;
}

std::vector<std::string> services_at_least(const std::vector<Entry>& entries, Level level) {
    auto services = entries |
                    std::views::filter([level](const Entry& e) { return e.level >= level; }) |
                    std::views::transform(&Entry::service) | std::ranges::to<std::set>();
    return {services.begin(), services.end()};
}

std::vector<double> moving_average(const std::vector<int>& values, std::size_t window) {
    if (window == 0 || window > values.size()) {
        return {};
    }
    return values | std::views::slide(static_cast<std::ptrdiff_t>(window)) |
           std::views::transform([window](auto run) {
               return std::accumulate(run.begin(), run.end(), 0.0) / static_cast<double>(window);
           }) |
           std::ranges::to<std::vector>();
}

}  // namespace dojo
