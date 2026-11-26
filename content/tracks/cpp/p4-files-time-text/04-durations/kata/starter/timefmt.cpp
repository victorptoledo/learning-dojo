#include "timefmt.hpp"

#include <stdexcept>

namespace dojo {

std::expected<std::chrono::seconds, ParseError> parse_duration(std::string_view text) {
    (void)text;
    throw std::logic_error("todo: write parse_duration");
}

std::expected<std::chrono::sys_seconds, ParseError> parse_timestamp(std::string_view text) {
    (void)text;
    throw std::logic_error("todo: write parse_timestamp");
}

std::string local_text(std::chrono::sys_seconds when, std::string_view zone) {
    (void)when;
    (void)zone;
    throw std::logic_error("todo: write local_text");
}

}  // namespace dojo

std::format_context::iterator std::formatter<dojo::Elapsed>::format(
    const dojo::Elapsed& elapsed, std::format_context& ctx) const {
    (void)elapsed;
    (void)ctx;
    (void)clock;
    throw std::logic_error("todo: write the Elapsed formatter");
}
