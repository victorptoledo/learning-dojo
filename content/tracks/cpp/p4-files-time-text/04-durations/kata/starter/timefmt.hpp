#pragma once

#include <chrono>
#include <expected>
#include <format>
#include <string>
#include <string_view>

namespace dojo {

enum class ParseError {
    Empty,       // nothing to parse
    BadNumber,   // a unit without digits before it, or a sign or space
    BadUnit,     // digits without a unit after them, or a unit other than d, h, m, s
    UnitOrder,   // units out of order (d, h, m, s) or repeated
    OutOfRange,  // the total does not fit in std::chrono::seconds
    BadFormat,   // a timestamp that does not have the exact shape
    BadDate,     // a date that does not exist, such as February 29 in 2026
    BadTime,     // an hour, minute, second or offset out of range
};

// "45s", "1h30m", "2d4h5s": numbers followed by the units d, h, m and s, in that order, each
// at most once, with no spaces.
std::expected<std::chrono::seconds, ParseError> parse_duration(std::string_view text);

// A duration that knows how to print itself: {} gives the parse_duration form ("1h30m",
// "0s", "-5m") and {:clock} gives hours, minutes and seconds ("01:30:05", "26:00:00").
struct Elapsed {
    std::chrono::seconds value;
};

// "2026-03-29T01:30:00Z" or "2026-03-29T02:30:00+01:00" (an offset of +HH:MM or -HH:MM).
std::expected<std::chrono::sys_seconds, ParseError> parse_timestamp(std::string_view text);

// The moment as a wall clock in the named IANA zone, "2026-03-29 02:30 WEST". An unknown zone
// throws std::runtime_error.
std::string local_text(std::chrono::sys_seconds when, std::string_view zone);

}  // namespace dojo

template <>
struct std::formatter<dojo::Elapsed> {
    bool clock = false;

    // Reads the options between ':' and '}': nothing, or "clock". Anything else is a
    // std::format_error, which is a compile error for a format string written in the code.
    constexpr auto parse(std::format_parse_context& ctx) {
        // todo: set `clock` for "clock" and throw std::format_error for anything else. For
        // now every option is skipped, so the tests compile.
        auto it = ctx.begin();
        while (it != ctx.end() && *it != '}') {
            ++it;
        }
        return it;
    }

    std::format_context::iterator format(const dojo::Elapsed& elapsed,
                                         std::format_context& ctx) const;
};
