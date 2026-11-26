#include "timefmt.hpp"

#include <charconv>
#include <cstdint>
#include <limits>
#include <optional>
#include <string_view>
#include <system_error>

namespace dojo {

namespace {

bool is_digit(char c) {
    return c >= '0' && c <= '9';
}

// A field of digits only, as an int (a fixed-width field always fits).
std::optional<int> digits(std::string_view text) {
    if (text.empty()) {
        return std::nullopt;
    }
    for (const char c : text) {
        if (!is_digit(c)) {
            return std::nullopt;
        }
    }
    int value = 0;
    std::from_chars(text.data(), text.data() + text.size(), value);
    return value;
}

}  // namespace

std::expected<std::chrono::seconds, ParseError> parse_duration(std::string_view text) {
    if (text.empty()) {
        return std::unexpected(ParseError::Empty);
    }
    constexpr std::string_view kUnits = "dhms";
    constexpr std::int64_t kSeconds[] = {86'400, 3'600, 60, 1};
    constexpr std::int64_t kMax = std::numeric_limits<std::int64_t>::max();
    std::int64_t total = 0;
    std::size_t next_unit = 0;  // the first unit still allowed
    const char* pos = text.data();
    const char* const end = text.data() + text.size();
    while (pos != end) {
        if (!is_digit(*pos)) {
            return std::unexpected(ParseError::BadNumber);  // from_chars would accept '-'
        }
        std::int64_t number = 0;
        const auto [stop, ec] = std::from_chars(pos, end, number);
        if (ec == std::errc::result_out_of_range) {
            return std::unexpected(ParseError::OutOfRange);
        }
        if (stop == end) {
            return std::unexpected(ParseError::BadUnit);
        }
        const std::size_t unit = kUnits.find(*stop);
        if (unit == std::string_view::npos) {
            return std::unexpected(ParseError::BadUnit);
        }
        if (unit < next_unit) {
            return std::unexpected(ParseError::UnitOrder);
        }
        // Check before multiplying and adding: signed overflow is undefined behaviour.
        if (number > kMax / kSeconds[unit] || number * kSeconds[unit] > kMax - total) {
            return std::unexpected(ParseError::OutOfRange);
        }
        total += number * kSeconds[unit];
        next_unit = unit + 1;
        pos = stop + 1;
    }
    return std::chrono::seconds{total};
}

std::expected<std::chrono::sys_seconds, ParseError> parse_timestamp(std::string_view text) {
    using namespace std::chrono;
    const bool utc = text.size() == 20 && text[19] == 'Z';
    const bool offset =
        text.size() == 25 && (text[19] == '+' || text[19] == '-') && text[22] == ':';
    if ((!utc && !offset) || text[4] != '-' || text[7] != '-' || text[10] != 'T' ||
        text[13] != ':' || text[16] != ':') {
        return std::unexpected(ParseError::BadFormat);
    }
    const auto y = digits(text.substr(0, 4));
    const auto mo = digits(text.substr(5, 2));
    const auto d = digits(text.substr(8, 2));
    const auto h = digits(text.substr(11, 2));
    const auto mi = digits(text.substr(14, 2));
    const auto s = digits(text.substr(17, 2));
    std::optional<int> oh = 0;
    std::optional<int> om = 0;
    if (offset) {
        oh = digits(text.substr(20, 2));
        om = digits(text.substr(23, 2));
    }
    if (!y || !mo || !d || !h || !mi || !s || !oh || !om) {
        return std::unexpected(ParseError::BadFormat);
    }
    const year_month_day date{year{*y}, month{static_cast<unsigned>(*mo)},
                              day{static_cast<unsigned>(*d)}};
    if (!date.ok()) {
        return std::unexpected(ParseError::BadDate);
    }
    if (*h > 23 || *mi > 59 || *s > 59 || *oh > 23 || *om > 59) {
        return std::unexpected(ParseError::BadTime);
    }
    const auto shift = hours{*oh} + minutes{*om};
    const sys_seconds wall = sys_days{date} + hours{*h} + minutes{*mi} + seconds{*s};
    return text[19] == '-' ? wall + shift : wall - shift;
}

std::string local_text(std::chrono::sys_seconds when, std::string_view zone) {
    const std::chrono::zoned_time local{std::chrono::locate_zone(zone), when};
    return std::format("{:%F %R %Z}", local);
}

}  // namespace dojo

std::format_context::iterator std::formatter<dojo::Elapsed>::format(
    const dojo::Elapsed& elapsed, std::format_context& ctx) const {
    std::int64_t total = elapsed.value.count();
    auto out = ctx.out();
    if (total < 0) {
        *out++ = '-';
        total = -total;
    }
    if (clock) {
        return std::format_to(out, "{:02}:{:02}:{:02}", total / 3'600, total / 60 % 60, total % 60);
    }
    if (total == 0) {
        return std::format_to(out, "0s");
    }
    const std::int64_t parts[] = {total / 86'400, total / 3'600 % 24, total / 60 % 60, total % 60};
    constexpr char kUnits[] = {'d', 'h', 'm', 's'};
    for (std::size_t i = 0; i < 4; ++i) {
        if (parts[i] != 0) {
            out = std::format_to(out, "{}{}", parts[i], kUnits[i]);
        }
    }
    return out;
}
