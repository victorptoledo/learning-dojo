#pragma once

#include <string>
#include <string_view>

namespace dojo {

// "Hello, Ada!" for "Ada"; surrounding spaces are ignored and a blank name greets "stranger".
std::string greet(std::string_view name);

// The first letter of each word, upper case: "grace brewster hopper" gives "GBH".
std::string initials(std::string_view full_name);

}  // namespace dojo
