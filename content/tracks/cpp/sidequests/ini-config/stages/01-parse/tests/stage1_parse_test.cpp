#include <gtest/gtest.h>

#include <optional>
#include <string>
#include <vector>

#include "config.hpp"
#include "config_testing.hpp"

using dojo::Config;
using dojo::ErrorKind;
using dojo::ParseError;
using dojo_testing::parsed;
using Names = std::vector<std::string>;

namespace {

const char* const kServer = R"(# the web server
name = demo

[server]
host = 127.0.0.1
port = 8080

; storage
[storage]
path = /var/lib/demo
)";

}  // namespace

TEST(Stage1Parse, ReadsSectionsAndKeys) {
    const Config config = parsed(kServer);
    EXPECT_EQ(config.raw("", "name"), "demo");
    EXPECT_EQ(config.raw("server", "host"), "127.0.0.1");
    EXPECT_EQ(config.raw("server", "port"), "8080");
    EXPECT_EQ(config.raw("storage", "path"), "/var/lib/demo");
}

TEST(Stage1Parse, MissingSectionsAndKeysAreEmpty) {
    const Config config = parsed(kServer);
    EXPECT_EQ(config.raw("server", "path"), std::nullopt);
    EXPECT_EQ(config.raw("client", "host"), std::nullopt);
    EXPECT_EQ(config.raw("", "host"), std::nullopt);
    EXPECT_EQ(config.raw("Server", "host"), std::nullopt) << "names are case-sensitive";
}

TEST(Stage1Parse, ListsSectionsAndKeysInOrder) {
    const Config config = parsed(kServer);
    EXPECT_EQ(config.sections(), (Names{"", "server", "storage"}));
    EXPECT_EQ(config.keys("server"), (Names{"host", "port"}));
    EXPECT_EQ(config.keys(""), (Names{"name"}));
    EXPECT_TRUE(config.keys("nowhere").empty());
}

TEST(Stage1Parse, GlobalSectionIsListedOnlyWithKeys) {
    const Config config = parsed("[a]\nx = 1\n[b]\n");
    EXPECT_EQ(config.sections(), (Names{"a", "b"}));
    EXPECT_TRUE(parsed("").sections().empty());
    EXPECT_TRUE(parsed("# only a comment\n\n").sections().empty());
}

TEST(Stage1Parse, TrimsAndKeepsTheRestOfTheValue) {
    const Config config =
        parsed("[ paths ]\n  home\t=  /home/me  \nurl = http://x/?a=b\nempty =\n");
    EXPECT_EQ(config.raw("paths", "home"), "/home/me");
    EXPECT_EQ(config.raw("paths", "url"), "http://x/?a=b") << "only the first = splits";
    EXPECT_EQ(config.raw("paths", "empty"), "");
}

TEST(Stage1Parse, AcceptsWindowsLineEndingsAndNoFinalNewline) {
    const Config config = parsed("[a]\r\nx = 1\r\ny = 2");
    EXPECT_EQ(config.raw("a", "x"), "1");
    EXPECT_EQ(config.raw("a", "y"), "2");
}

TEST(Stage1Parse, ReopeningASectionAddsToIt) {
    const Config config = parsed("[a]\nx = 1\n[b]\ny = 2\n[a]\nz = 3\n");
    EXPECT_EQ(config.sections(), (Names{"a", "b"}));
    EXPECT_EQ(config.keys("a"), (Names{"x", "z"}));
}

TEST(Stage1Parse, NamesUseLettersDigitsAndDotsDashesUnderscores) {
    const Config config = parsed("[log.file-1]\nmax_size.MB = 10\n");
    EXPECT_EQ(config.raw("log.file-1", "max_size.MB"), "10");
}

TEST(Stage1Parse, ReportsTheLineAndKindOfAnError) {
    const auto error = [](const char* text) {
        const auto result = Config::parse(text);
        return result ? std::nullopt : std::optional<ParseError>(result.error());
    };
    EXPECT_EQ(error("a = 1\n[b\n"), (ParseError{.line = 2, .kind = ErrorKind::BadSection}));
    EXPECT_EQ(error("[]\n"), (ParseError{.line = 1, .kind = ErrorKind::BadSection}));
    EXPECT_EQ(error("[a b]\n"), (ParseError{.line = 1, .kind = ErrorKind::BadSection}));
    EXPECT_EQ(error("\n\n = 1\n"), (ParseError{.line = 3, .kind = ErrorKind::BadKey}));
    EXPECT_EQ(error("my key = 1\n"), (ParseError{.line = 1, .kind = ErrorKind::BadKey}));
    EXPECT_EQ(error("[a]\njust words\n"),
              (ParseError{.line = 2, .kind = ErrorKind::MissingEquals}));
    EXPECT_EQ(error("[a]\nx = 1\n# c\nx = 2\n"),
              (ParseError{.line = 4, .kind = ErrorKind::DuplicateKey}));
}

TEST(Stage1Parse, SameKeyInTwoSectionsIsFine) {
    const Config config = parsed("x = 0\n[a]\nx = 1\n[b]\nx = 2\n");
    EXPECT_EQ(config.raw("", "x"), "0");
    EXPECT_EQ(config.raw("b", "x"), "2");
}

TEST(Stage1Parse, ConfigsAreComparableValues) {
    EXPECT_EQ(parsed("[a]\nx = 1\n"), parsed("# same\n[a]\n  x=1\n"));
    EXPECT_NE(parsed("[a]\nx = 1\n"), parsed("[a]\nx = 2\n"));
    const Config original = parsed(kServer);
    const Config copy = original;
    EXPECT_EQ(copy, original);
}
