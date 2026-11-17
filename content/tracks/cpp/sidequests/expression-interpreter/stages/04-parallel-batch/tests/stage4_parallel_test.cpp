#include <gtest/gtest.h>

#include <chrono>
#include <expected>
#include <format>
#include <mutex>
#include <set>
#include <span>
#include <string>
#include <thread>
#include <vector>

#include "expr.hpp"

using dojo::Environment;
using dojo::Error;
using dojo::ErrorKind;

namespace {

std::vector<std::string> formulas(int count) {
    std::vector<std::string> texts;
    for (int i = 0; i < count; ++i) {
        texts.push_back(std::format("{} * x + {} ^ 2 - max({}, y)", i, i % 7, i % 5));
    }
    return texts;
}

Environment sample() {
    Environment env;
    env.variables = {{"x", 2}, {"y", 3}};
    return env;
}

}  // namespace

TEST(Stage4Parallel, ResultsAreInTheOrderOfTheFormulas) {
    const std::vector<std::string> texts{"1 + 1", "x * 10", "2 ^ 10", "y - x"};
    const auto results = dojo::evaluate_all(texts, sample(), 2);
    ASSERT_EQ(results.size(), 4U);
    EXPECT_EQ(results[0], 2.0);
    EXPECT_EQ(results[1], 20.0);
    EXPECT_EQ(results[2], 1024.0);
    EXPECT_EQ(results[3], 1.0);
}

TEST(Stage4Parallel, ErrorsStayInTheirPlace) {
    const std::vector<std::string> texts{"1 +", "x", "1 / 0", "nope", "y"};
    const auto results = dojo::evaluate_all(texts, sample(), 3);
    ASSERT_EQ(results.size(), 5U);
    EXPECT_EQ(results[0], std::unexpected(Error{.position = 3, .kind = ErrorKind::UnexpectedEnd}));
    EXPECT_EQ(results[1], 2.0);
    EXPECT_EQ(results[2], std::unexpected(Error{.position = 2, .kind = ErrorKind::DivisionByZero}));
    EXPECT_EQ(results[3],
              std::unexpected(Error{.position = 0, .kind = ErrorKind::UnknownVariable}));
    EXPECT_EQ(results[4], 3.0);
}

TEST(Stage4Parallel, AnyNumberOfThreadsGivesTheSameResults) {
    const auto texts = formulas(400);
    const Environment env = sample();
    std::vector<std::expected<double, Error>> expected;
    for (const auto& text : texts) {
        expected.push_back(dojo::parse(text).and_then(
            [&env](const dojo::Expr& e) { return dojo::evaluate(e, env); }));
    }
    for (unsigned threads : {0U, 1U, 2U, 3U, 8U, 1000U}) {
        EXPECT_EQ(dojo::evaluate_all(texts, env, threads), expected) << threads << " threads";
    }
}

TEST(Stage4Parallel, NoFormulasNoResults) {
    EXPECT_TRUE(dojo::evaluate_all({}, sample(), 4).empty());
}

TEST(Stage4Parallel, FormulasRunOnSeveralThreadsAtOnce) {
    std::mutex mutex;
    std::set<std::thread::id> seen;
    Environment env = sample();
    env.functions["work"] = [&](std::span<const double> values) {
        {
            const std::lock_guard lock(mutex);
            seen.insert(std::this_thread::get_id());
        }
        std::this_thread::sleep_for(std::chrono::milliseconds(5));
        return values.empty() ? 0.0 : values[0];
    };
    std::vector<std::string> texts;
    for (int i = 0; i < 24; ++i) {
        texts.push_back(std::format("work({})", i));
    }
    const auto results = dojo::evaluate_all(texts, env, 4);
    ASSERT_EQ(results.size(), 24U);
    EXPECT_EQ(results[23], 23.0);
    const std::lock_guard lock(mutex);
    EXPECT_GE(seen.size(), 2U) << "evaluate_all(..., 4) used only one thread";
    EXPECT_LE(seen.size(), 4U) << "evaluate_all(..., 4) used more than 4 threads";
}
