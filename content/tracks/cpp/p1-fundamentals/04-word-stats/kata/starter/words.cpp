#include "words.hpp"

#include <stdexcept>

namespace dojo {

std::vector<std::string> words(std::string_view text) {
    (void)text;
    throw std::logic_error("todo: write words");
}

std::map<std::string, int> count(const std::vector<std::string>& words) {
    (void)words;
    throw std::logic_error("todo: write count");
}

std::vector<WordCount> top(std::string_view text, std::size_t n) {
    (void)text;
    (void)n;
    throw std::logic_error("todo: write top");
}

std::vector<std::string> first_seen(std::string_view text) {
    (void)text;
    throw std::logic_error("todo: write first_seen");
}

std::map<std::size_t, std::vector<std::string>> by_length(std::string_view text) {
    (void)text;
    throw std::logic_error("todo: write by_length");
}

}  // namespace dojo
