#include "logs.hpp"

#include <stdexcept>

namespace dojo {

std::generator<std::string_view> lines(std::string_view text) {
    (void)text;
    throw std::logic_error("todo: write lines");
    co_return;
}

std::optional<Entry> parse_entry(std::string_view line) {
    (void)line;
    throw std::logic_error("todo: write parse_entry");
}

std::vector<Entry> parse_all(std::string_view text) {
    (void)text;
    throw std::logic_error("todo: write parse_all");
}

std::map<std::string, int> errors_by_service(const std::vector<Entry>& entries) {
    (void)entries;
    throw std::logic_error("todo: write errors_by_service");
}

std::vector<Entry> slowest(const std::vector<Entry>& entries, std::size_t n) {
    (void)entries;
    (void)n;
    throw std::logic_error("todo: write slowest");
}

std::vector<std::string> services_at_least(const std::vector<Entry>& entries, Level level) {
    (void)entries;
    (void)level;
    throw std::logic_error("todo: write services_at_least");
}

std::vector<double> moving_average(const std::vector<int>& values, std::size_t window) {
    (void)values;
    (void)window;
    throw std::logic_error("todo: write moving_average");
}

}  // namespace dojo
