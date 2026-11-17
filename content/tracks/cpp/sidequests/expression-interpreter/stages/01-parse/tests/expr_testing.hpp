// Helpers shared by the contract tests of every stage.
#pragma once

#include <gtest/gtest.h>

#include <optional>
#include <string>
#include <string_view>

#include "expr.hpp"

namespace dojo_testing {

// The parenthesised tree of the text, or "parse error at N".
inline std::string tree(std::string_view text) {
    const auto expr = dojo::parse(text);
    if (!expr) {
        return "parse error at " + std::to_string(expr.error().position);
    }
    return dojo::to_string(*expr);
}

inline std::optional<dojo::Error> parse_error(std::string_view text) {
    const auto expr = dojo::parse(text);
    if (expr) {
        return std::nullopt;
    }
    return expr.error();
}

}  // namespace dojo_testing
