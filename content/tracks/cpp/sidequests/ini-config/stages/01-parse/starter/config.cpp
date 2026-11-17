#include "config.hpp"

namespace dojo {

std::expected<Config, ParseError> Config::parse(std::string_view text) {
    (void)text;
    return std::unexpected(ParseError{.line = 1, .kind = ErrorKind::MissingEquals});
}

std::optional<std::string> Config::raw(std::string_view section, std::string_view key) const {
    (void)section;
    (void)key;
    return std::nullopt;
}

std::vector<std::string> Config::sections() const {
    return {};
}

std::vector<std::string> Config::keys(std::string_view section) const {
    (void)section;
    return {};
}

}  // namespace dojo
