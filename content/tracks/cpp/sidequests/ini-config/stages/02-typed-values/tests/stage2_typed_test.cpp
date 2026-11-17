#include <gtest/gtest.h>

#include <cstdint>
#include <expected>
#include <string>
#include <vector>

#include "config.hpp"
#include "config_testing.hpp"

using dojo::Config;
using dojo::GetError;
using dojo_testing::parsed;

namespace {

const char* const kTyped = R"([server]
host = example.org
port = 8080
timeout = 2.5
debug = yes
verbose = Off
retries = -3
big = 99999999999
word = ten
nine = 9x
)";

}  // namespace

TEST(Stage2Typed, ReadsStringsAndNumbers) {
    const Config config = parsed(kTyped);
    EXPECT_EQ(config.get<std::string>("server", "host"), "example.org");
    EXPECT_EQ(config.get<int>("server", "port"), 8080);
    EXPECT_EQ(config.get<std::uint16_t>("server", "port"), 8080);
    EXPECT_EQ(config.get<int>("server", "retries"), -3);
    EXPECT_EQ(config.get<double>("server", "timeout"), 2.5);
    EXPECT_EQ(config.get<float>("server", "timeout"), 2.5F);
    EXPECT_EQ(config.get<std::int64_t>("server", "big"), 99999999999);
    EXPECT_EQ(config.get<double>("server", "port"), 8080.0);
}

TEST(Stage2Typed, ReadsBooleansInAnyCase) {
    const Config config = parsed(
        "[f]\na = true\nb = FALSE\nc = Yes\nd = no\ne = on\nf = OFF\n"
        "g = 1\nh = 0\n");
    for (const char* key : {"a", "c", "e", "g"}) {
        EXPECT_EQ(config.get<bool>("f", key), true) << key;
    }
    for (const char* key : {"b", "d", "f", "h"}) {
        EXPECT_EQ(config.get<bool>("f", key), false) << key;
    }
    EXPECT_EQ(parsed(kTyped).get<bool>("server", "debug"), true);
    EXPECT_EQ(parsed(kTyped).get<bool>("server", "verbose"), false);
}

TEST(Stage2Typed, MissingKeysAreMissing) {
    const Config config = parsed(kTyped);
    EXPECT_EQ(config.get<int>("server", "workers"), std::unexpected(GetError::Missing));
    EXPECT_EQ(config.get<std::string>("client", "host"), std::unexpected(GetError::Missing));
}

TEST(Stage2Typed, TextThatIsNotOfTheTypeIsABadValue) {
    const Config config = parsed(kTyped);
    EXPECT_EQ(config.get<int>("server", "word"), std::unexpected(GetError::BadValue));
    EXPECT_EQ(config.get<int>("server", "nine"), std::unexpected(GetError::BadValue));
    EXPECT_EQ(config.get<int>("server", "timeout"), std::unexpected(GetError::BadValue));
    EXPECT_EQ(config.get<unsigned>("server", "retries"), std::unexpected(GetError::BadValue));
    EXPECT_EQ(config.get<bool>("server", "port"), std::unexpected(GetError::BadValue));
    EXPECT_EQ(config.get<double>("server", "word"), std::unexpected(GetError::BadValue));
    EXPECT_EQ(parsed("[a]\nx =\n").get<int>("a", "x"), std::unexpected(GetError::BadValue));
}

TEST(Stage2Typed, NumbersThatDoNotFitAreOutOfRange) {
    const Config config = parsed(kTyped);
    EXPECT_EQ(config.get<int>("server", "big"), std::unexpected(GetError::OutOfRange));
    EXPECT_EQ(config.get<std::int8_t>("server", "port"), std::unexpected(GetError::OutOfRange));
    EXPECT_EQ(parsed("[a]\nx = 1e999\n").get<double>("a", "x"),
              std::unexpected(GetError::OutOfRange));
}

TEST(Stage2Typed, GetOrFallsBackOnAnyError) {
    const Config config = parsed(kTyped);
    EXPECT_EQ(config.get_or<int>("server", "port", 80), 8080);
    EXPECT_EQ(config.get_or<int>("server", "workers", 4), 4);
    EXPECT_EQ(config.get_or<int>("server", "word", 7), 7);
    EXPECT_EQ(config.get_or<std::string>("server", "user", "nobody"), "nobody");
    EXPECT_EQ(config.get_or<bool>("server", "debug", false), true);
}

TEST(Stage2Typed, TheConceptNamesTheReadableTypes) {
    EXPECT_TRUE(dojo::ConfigValue<int>);
    EXPECT_TRUE(dojo::ConfigValue<std::uint64_t>);
    EXPECT_TRUE(dojo::ConfigValue<double>);
    EXPECT_TRUE(dojo::ConfigValue<bool>);
    EXPECT_TRUE(dojo::ConfigValue<std::string>);
    EXPECT_FALSE(dojo::ConfigValue<char>);
    EXPECT_FALSE(dojo::ConfigValue<const char*>);
    EXPECT_FALSE(dojo::ConfigValue<std::vector<int>>);
    EXPECT_FALSE(dojo::ConfigValue<std::string_view>);
}
