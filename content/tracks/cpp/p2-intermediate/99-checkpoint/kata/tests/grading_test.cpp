#include <gtest/gtest.h>

#include <stdexcept>
#include <string>
#include <type_traits>
#include <utility>

#include "config.hpp"

using dojo::Config;
using dojo::ConfigError;
using dojo::ErrorKind;
using dojo::Transaction;

namespace {

Config sample() {
    auto parsed = Config::parse(
        "# service settings\n"
        "name = billing\n"
        "port=8080\n"
        "\n"
        "  ratio =  0.25  \n"
        "debug = true\n"
        "empty =\n"
        "url = http://x.test/?a=b\n");
    if (!parsed) {
        throw std::runtime_error("the sample does not parse");
    }
    return std::move(*parsed);
}

}  // namespace

TEST(Parse, ReadsKeysAndTrimmedValues) {
    const Config config = sample();
    EXPECT_EQ(config.size(), 6U);
    EXPECT_EQ(config.raw("name"), "billing");
    EXPECT_EQ(config.raw("ratio"), "0.25");
    EXPECT_EQ(config.raw("empty"), "");
    EXPECT_EQ(config.raw("url"), "http://x.test/?a=b");
    EXPECT_EQ(config.raw("nope"), std::nullopt);
}

TEST(Parse, ToleratesWindowsLineEnds) {
    const auto config = Config::parse("a = 1\r\nb = 2\r\n");
    ASSERT_TRUE(config.has_value());
    EXPECT_EQ(config->raw("b"), "2");
}

TEST(Parse, LineWithoutEqualsIsASyntaxError) {
    const auto config = Config::parse("a = 1\n\njust words\n");
    ASSERT_FALSE(config.has_value());
    EXPECT_EQ(config.error(), (ConfigError{ErrorKind::Syntax, 3, ""}));
}

TEST(Parse, BadKeysAreSyntaxErrors) {
    EXPECT_EQ(Config::parse(" = 1").error(), (ConfigError{ErrorKind::Syntax, 1, ""}));
    EXPECT_EQ(Config::parse("ok = 1\nBad Key = 2").error(),
              (ConfigError{ErrorKind::Syntax, 2, ""}));
}

TEST(Parse, DuplicateKeysAreReported) {
    const auto config = Config::parse("a = 1\nb = 2\na = 3\n");
    ASSERT_FALSE(config.has_value());
    EXPECT_EQ(config.error(), (ConfigError{ErrorKind::DuplicateKey, 3, "a"}));
}

TEST(Get, ReadsEachType) {
    const Config config = sample();
    EXPECT_EQ(config.get<int>("port"), 8080);
    EXPECT_DOUBLE_EQ(*config.get<double>("ratio"), 0.25);
    EXPECT_EQ(config.get<bool>("debug"), true);
    EXPECT_EQ(config.get<std::string>("name"), "billing");
    EXPECT_EQ(config.get<std::string>("empty"), "");
}

TEST(Get, MissingKey) {
    EXPECT_EQ(sample().get<int>("timeout").error(),
              (ConfigError{ErrorKind::MissingKey, 0, "timeout"}));
}

TEST(Get, WrongType) {
    const Config config = sample();
    EXPECT_EQ(config.get<int>("name").error(), (ConfigError{ErrorKind::WrongType, 0, "name"}));
    EXPECT_EQ(config.get<int>("ratio").error().kind, ErrorKind::WrongType);
    EXPECT_EQ(config.get<bool>("port").error().kind, ErrorKind::WrongType);
    EXPECT_EQ(config.get<double>("empty").error().kind, ErrorKind::WrongType);
}

TEST(Get, IntMustFit) {
    const auto config = Config::parse("big = 99999999999\n");
    EXPECT_EQ(config->get<int>("big").error().kind, ErrorKind::WrongType);
}

TEST(GetOr, FallsBack) {
    const Config config = sample();
    EXPECT_EQ(config.get_or("port", 1), 8080);
    EXPECT_EQ(config.get_or("timeout", 30), 30);
    EXPECT_EQ(config.get_or("name", 7), 7);
    EXPECT_EQ(config.get_or<std::string>("region", "eu"), "eu");
}

TEST(Transaction, RollsBackWhenItGoesOutOfScope) {
    Config config = sample();
    {
        Transaction change(config);
        change.set("port", "9090");
        change.set("timeout", "5");
        change.erase("debug");
        EXPECT_EQ(config.raw("port"), "9090");
        EXPECT_FALSE(config.contains("debug"));
    }
    EXPECT_EQ(config.raw("port"), "8080");
    EXPECT_FALSE(config.contains("timeout"));
    EXPECT_EQ(config.raw("debug"), "true");
}

TEST(Transaction, CommitKeepsTheChanges) {
    Config config = sample();
    {
        Transaction change(config);
        change.set("port", "9090");
        change.erase("name");
        change.commit();
    }
    EXPECT_EQ(config.raw("port"), "9090");
    EXPECT_FALSE(config.contains("name"));
}

TEST(Transaction, RollbackUndoesTheLastChangeFirst) {
    Config config = sample();
    Transaction change(config);
    change.set("port", "1");
    change.set("port", "2");
    change.erase("port");
    change.set("port", "3");
    change.rollback();
    EXPECT_EQ(config.raw("port"), "8080");
}

TEST(Transaction, NothingAfterItEnds) {
    Config config = sample();
    Transaction change(config);
    change.commit();
    EXPECT_THROW(change.set("a", "b"), std::logic_error);
    EXPECT_THROW(change.commit(), std::logic_error);
    EXPECT_THROW(change.rollback(), std::logic_error);
    EXPECT_FALSE(config.contains("a"));
}

TEST(Transaction, MovingHandsOverTheChanges) {
    EXPECT_FALSE(std::is_copy_constructible_v<Transaction>);
    EXPECT_TRUE(std::is_nothrow_move_constructible_v<Transaction>);
    Config config = sample();
    {
        Transaction first(config);
        first.set("port", "1");
        Transaction second = std::move(first);
        second.set("name", "x");
    }
    EXPECT_EQ(config.raw("port"), "8080");
    EXPECT_EQ(config.raw("name"), "billing");
}
