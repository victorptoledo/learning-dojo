#include <gtest/gtest.h>

#include <string>
#include <vector>

#include "config.hpp"
#include "config_testing.hpp"

using dojo::Config;
using dojo_testing::parsed;
using Names = std::vector<std::string>;

TEST(Stage4Write, SetAddsAndReplacesKeys) {
    Config config = parsed("[a]\nx = 1\n");
    EXPECT_TRUE(config.set("a", "x", "10"));
    EXPECT_TRUE(config.set("a", "y", "2"));
    EXPECT_TRUE(config.set("", "top", "yes"));
    EXPECT_TRUE(config.set("new", "z", ""));
    EXPECT_EQ(config.raw("a", "x"), "10");
    EXPECT_EQ(config.keys("a"), (Names{"x", "y"})) << "a replaced key keeps its place";
    EXPECT_EQ(config.sections(), (Names{"", "a", "new"}));
    EXPECT_EQ(config.raw("new", "z"), "");
}

TEST(Stage4Write, SetRefusesWhatWouldNotReadBack) {
    Config config = parsed("[a]\nx = 1\n");
    const Config before = config;
    EXPECT_FALSE(config.set("a b", "x", "1"));
    EXPECT_FALSE(config.set("a", "", "1"));
    EXPECT_FALSE(config.set("a", "x=y", "1"));
    EXPECT_FALSE(config.set("a", "x", "two\nlines"));
    EXPECT_FALSE(config.set("a", "x", " padded "));
    EXPECT_EQ(config, before) << "a refused set must change nothing";
}

TEST(Stage4Write, ToStringWritesACanonicalDocument) {
    const Config config = parsed(
        "[server]\n  host=x\n# gone\nport = 80\n[empty]\n[server]\nz = 1\n"
        "top = no\n");
    EXPECT_EQ(config.to_string(), "[server]\nhost = x\nport = 80\nz = 1\ntop = no\n\n[empty]\n");
    const Config global = parsed("a = 1\n[s]\nb = 2\n");
    EXPECT_EQ(global.to_string(), "a = 1\n\n[s]\nb = 2\n");
    EXPECT_EQ(Config{}.to_string(), "");
}

TEST(Stage4Write, ToStringReadsBackEqual) {
    Config config = parsed("name = demo\n[server]\nurl = http://x/?a=b\n[empty]\n");
    config.set("server", "note", "# not a comment");
    config.set("other", "list", "a, b ; c");
    const auto back = Config::parse(config.to_string());
    ASSERT_TRUE(back.has_value()) << config.to_string();
    EXPECT_EQ(*back, config) << config.to_string();
}

TEST(Stage4Write, MergedOverlaysKeysInOrder) {
    const Config base = parsed("[server]\nhost = a\nport = 80\n[log]\nlevel = info\n");
    const Config overlay = parsed("debug = on\n[server]\nport = 8080\ntls = yes\n[cache]\n");
    const Config result = dojo::merged(base, overlay);
    EXPECT_EQ(result.sections(), (Names{"", "server", "log", "cache"}));
    EXPECT_EQ(result.keys("server"), (Names{"host", "port", "tls"}));
    EXPECT_EQ(result.raw("server", "port"), "8080");
    EXPECT_EQ(result.raw("server", "host"), "a");
    EXPECT_EQ(result.raw("log", "level"), "info");
    EXPECT_EQ(result.raw("", "debug"), "on");
    EXPECT_EQ(base.raw("server", "port"), "80") << "merged must not change its arguments";
}

TEST(Stage4Write, MergingWithAnEmptyConfigChangesNothing) {
    const Config base = parsed("[a]\nx = 1\n");
    EXPECT_EQ(dojo::merged(base, Config{}), base);
    EXPECT_EQ(dojo::merged(Config{}, base), base);
}
