#include "greet.hpp"

#include <stdexcept>

namespace dojo {

std::string greet(std::string_view name) {
    (void)name;
    throw std::logic_error("todo: write greet");
}

std::string initials(std::string_view full_name) {
    (void)full_name;
    throw std::logic_error("todo: write initials");
}

}  // namespace dojo
