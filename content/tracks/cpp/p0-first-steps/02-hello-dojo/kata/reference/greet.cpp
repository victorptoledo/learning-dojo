#include "greet.hpp"

#include <cctype>
#include <format>

namespace dojo {

namespace {

bool is_space(char c) {
    return std::isspace(static_cast<unsigned char>(c)) != 0;
}

std::string_view trim(std::string_view text) {
    while (!text.empty() && is_space(text.front())) {
        text.remove_prefix(1);
    }
    while (!text.empty() && is_space(text.back())) {
        text.remove_suffix(1);
    }
    return text;
}

}  // namespace

std::string greet(std::string_view name) {
    const std::string_view trimmed = trim(name);
    return std::format("Hello, {}!", trimmed.empty() ? "stranger" : trimmed);
}

std::string initials(std::string_view full_name) {
    std::string result;
    bool at_word_start = true;
    for (const char c : full_name) {
        if (is_space(c)) {
            at_word_start = true;
        } else if (at_word_start) {
            result += static_cast<char>(std::toupper(static_cast<unsigned char>(c)));
            at_word_start = false;
        }
    }
    return result;
}

}  // namespace dojo
