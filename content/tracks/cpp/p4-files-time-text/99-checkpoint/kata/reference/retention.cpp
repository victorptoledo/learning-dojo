#include "retention.hpp"

#include <algorithm>
#include <charconv>
#include <format>
#include <fstream>
#include <optional>
#include <set>
#include <string_view>
#include <utility>

namespace dojo {

namespace fs = std::filesystem;
using namespace std::chrono;

namespace {

std::optional<int> digits(std::string_view text) {
    if (text.empty() || !std::ranges::all_of(text, [](char c) { return c >= '0' && c <= '9'; })) {
        return std::nullopt;
    }
    int value = 0;
    std::from_chars(text.data(), text.data() + text.size(), value);
    return value;
}

// The local wall time in a name such as "db-20261126-0300.bak".
std::optional<local_seconds> wall_time(const fs::path& path) {
    if (path.extension() != ".bak") {
        return std::nullopt;
    }
    const std::string stem = path.stem().string();
    // "<name>-YYYYMMDD-HHMM": at least one character of name, then 14 characters.
    if (stem.size() < 15 || stem[stem.size() - 14] != '-' || stem[stem.size() - 5] != '-') {
        return std::nullopt;
    }
    const std::string_view tail = std::string_view(stem).substr(stem.size() - 13);
    const auto y = digits(tail.substr(0, 4));
    const auto mo = digits(tail.substr(4, 2));
    const auto d = digits(tail.substr(6, 2));
    const auto h = digits(tail.substr(9, 2));
    const auto mi = digits(tail.substr(11, 2));
    if (!y || !mo || !d || !h || !mi || *h > 23 || *mi > 59) {
        return std::nullopt;
    }
    const year_month_day date{year{*y}, month{static_cast<unsigned>(*mo)},
                              day{static_cast<unsigned>(*d)}};
    if (!date.ok()) {
        return std::nullopt;
    }
    return local_days{date} + hours{*h} + minutes{*mi};
}

// The ISO year and week of a date, as one number: 202648 for 2026-11-26.
int iso_week(local_days d) {
    const unsigned from_monday = weekday{d}.iso_encoding() - 1;  // Monday 0 ... Sunday 6
    const local_days thursday = d - days{from_monday} + days{3};
    const year iso_year = year_month_day{thursday}.year();
    const auto week = (thursday - local_days{iso_year / January / 1}).count() / 7 + 1;
    return static_cast<int>(iso_year) * 100 + static_cast<int>(week);
}

int month_key(local_days d) {
    const year_month_day ymd{d};
    return static_cast<int>(ymd.year()) * 100 +
           static_cast<int>(static_cast<unsigned>(ymd.month()));
}

}  // namespace

std::expected<Scan, ScanError> scan(const fs::path& dir, const time_zone* zone) {
    std::error_code ec;
    if (!fs::is_directory(dir, ec)) {
        return std::unexpected(ScanError::NotADirectory);
    }
    fs::directory_iterator it(dir, ec);
    if (ec) {
        return std::unexpected(ScanError::Unreadable);
    }
    Scan result;
    for (const fs::directory_entry& entry : it) {
        if (!entry.is_regular_file(ec)) {
            continue;
        }
        const auto wall = wall_time(entry.path());
        const local_info info = wall ? zone->get_info(*wall) : local_info{};
        if (!wall || info.result == local_info::nonexistent) {
            result.skipped.push_back(entry.path());
            continue;
        }
        result.backups.push_back(Backup{.path = entry.path(),
                                        .taken = zone->to_sys(*wall, choose::earliest),
                                        .day = floor<days>(*wall)});
    }
    std::ranges::sort(result.backups, std::ranges::greater{}, &Backup::taken);
    std::ranges::sort(result.skipped);
    return result;
}

Plan plan(std::vector<Backup> backups, const Policy& policy) {
    std::ranges::sort(backups, [](const Backup& a, const Backup& b) {
        return a.taken != b.taken ? a.taken > b.taken : a.path > b.path;
    });
    std::set<int> days_seen;
    std::set<int> weeks_seen;
    std::set<int> months_seen;
    Plan result;
    for (const Backup& backup : backups) {
        std::string reason;
        const auto add = [&reason](std::set<int>& seen, int key, int limit, std::string_view name) {
            if (std::cmp_less(seen.size(), limit) && seen.insert(key).second) {
                reason += reason.empty() ? "" : "+";
                reason += name;
            }
        };
        add(days_seen, static_cast<int>(backup.day.time_since_epoch().count()), policy.daily,
            "daily");
        add(weeks_seen, iso_week(backup.day), policy.weekly, "weekly");
        add(months_seen, month_key(backup.day), policy.monthly, "monthly");
        if (reason.empty()) {
            result.remove.push_back(backup.path);
        } else {
            result.keep.push_back(Decision{.path = backup.path, .reason = std::move(reason)});
        }
    }
    return result;
}

std::string report(const Plan& plan) {
    std::size_t width = 0;
    for (const Decision& d : plan.keep) {
        width = std::max(width, d.path.filename().string().size());
    }
    std::string out;
    for (const Decision& d : plan.keep) {
        std::format_to(std::back_inserter(out), "{:<6} {:<{}}  {}\n", "keep",
                       d.path.filename().string(), width, d.reason);
    }
    for (const fs::path& p : plan.remove) {
        std::format_to(std::back_inserter(out), "{:<6} {}\n", "remove", p.filename().string());
    }
    std::format_to(std::back_inserter(out), "{} kept, {} removed\n", plan.keep.size(),
                   plan.remove.size());
    return out;
}

std::expected<std::size_t, std::error_code> apply(const fs::path& dir, const Plan& plan) {
    std::size_t removed = 0;
    for (const fs::path& p : plan.remove) {
        std::error_code ec;
        if (fs::remove(p, ec)) {
            ++removed;
        } else if (ec) {
            return std::unexpected(ec);  // false without an error: it was already gone
        }
    }
    const fs::path log = dir / "retention.log";
    fs::path temp = log;
    temp += ".tmp";
    {
        std::ofstream out(temp, std::ios::binary | std::ios::trunc);
        const std::string text = report(plan);
        out.write(text.data(), static_cast<std::streamsize>(text.size()));
        out.close();
        if (!out) {
            std::error_code ignored;
            fs::remove(temp, ignored);
            return std::unexpected(std::make_error_code(std::errc::io_error));
        }
    }
    std::error_code ec;
    fs::rename(temp, log, ec);
    if (ec) {
        return std::unexpected(ec);
    }
    return removed;
}

}  // namespace dojo
