// Helpers shared by the contract tests of every stage.
#pragma once

#include <gtest/gtest.h>

#include <string_view>

#include "config.hpp"

namespace dojo_testing {

// The Config of this text; the test fails if parse refuses it.
inline dojo::Config parsed(std::string_view text) {
    auto config = dojo::Config::parse(text);
    EXPECT_TRUE(config.has_value())
        << "parse refused:\n"
        << text << "\nerror on line " << (config ? std::size_t{0} : config.error().line);
    return config.value_or(dojo::Config{});
}

}  // namespace dojo_testing
