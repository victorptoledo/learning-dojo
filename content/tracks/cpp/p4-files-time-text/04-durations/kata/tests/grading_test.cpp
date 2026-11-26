#include <gtest/gtest.h>

#include <chrono>
#include <format>
#include <stdexcept>
#include <string>

#include "timefmt.hpp"

using namespace std::chrono_literals;
using dojo::Elapsed;
using dojo::ParseError;
using std::chrono::sys_days;

namespace {

ParseError duration_error(std::string_view text) {
    const auto parsed = dojo::parse_duration(text);
    EXPECT_FALSE(parsed.has_value()) << text << " should not parse";
    return parsed.has_value() ? ParseError::Empty : parsed.error();
}

ParseError timestamp_error(std::string_view text) {
    const auto parsed = dojo::parse_timestamp(text);
    EXPECT_FALSE(parsed.has_value()) << text << " should not parse";
    return parsed.has_value() ? ParseError::Empty : parsed.error();
}

}  // namespace

TEST(ParseDuration, ReadsUnitsInOrder) {
    EXPECT_EQ(dojo::parse_duration("45s"), 45s);
    EXPECT_EQ(dojo::parse_duration("1h30m"), 90min);
    EXPECT_EQ(dojo::parse_duration("90m"), 90min);
    EXPECT_EQ(dojo::parse_duration("2d4h5s"), 52h + 5s);
    EXPECT_EQ(dojo::parse_duration("0s"), 0s);
    EXPECT_EQ(dojo::parse_duration("007m"), 7min);
}

TEST(ParseDuration, RejectsMissingNumbersAndUnits) {
    EXPECT_EQ(duration_error(""), ParseError::Empty);
    EXPECT_EQ(duration_error("h"), ParseError::BadNumber);
    EXPECT_EQ(duration_error("1hm"), ParseError::BadNumber);
    EXPECT_EQ(duration_error("5"), ParseError::BadUnit);
    EXPECT_EQ(duration_error("1h30"), ParseError::BadUnit);
    EXPECT_EQ(duration_error("5x"), ParseError::BadUnit);
    EXPECT_EQ(duration_error("5H"), ParseError::BadUnit);
}

TEST(ParseDuration, RejectsSignsAndSpaces) {
    EXPECT_EQ(duration_error("-5s"), ParseError::BadNumber);
    EXPECT_EQ(duration_error("+5s"), ParseError::BadNumber);
    EXPECT_EQ(duration_error(" 5s"), ParseError::BadNumber);
    EXPECT_EQ(duration_error("1h 5s"), ParseError::BadNumber);
}

TEST(ParseDuration, UnitsMustBeInOrderAndOnce) {
    EXPECT_EQ(duration_error("30m1h"), ParseError::UnitOrder);
    EXPECT_EQ(duration_error("1h1h"), ParseError::UnitOrder);
    EXPECT_EQ(duration_error("1s1d"), ParseError::UnitOrder);
}

TEST(ParseDuration, ReportsOverflowInsteadOfWrapping) {
    EXPECT_EQ(duration_error("99999999999999999999s"), ParseError::OutOfRange);
    EXPECT_EQ(duration_error("200000000000000d"), ParseError::OutOfRange);  // * 86400 overflows
    EXPECT_EQ(duration_error("106751991167300d86399s"), ParseError::OutOfRange);  // the sum does
    EXPECT_EQ(dojo::parse_duration("9223372036854775807s"),
              std::chrono::seconds{9'223'372'036'854'775'807});
}

TEST(Elapsed, PrintsTheCompactForm) {
    EXPECT_EQ(std::format("{}", Elapsed{90min}), "1h30m");
    EXPECT_EQ(std::format("{}", Elapsed{52h + 5s}), "2d4h5s");
    EXPECT_EQ(std::format("{}", Elapsed{0s}), "0s");
    EXPECT_EQ(std::format("{}", Elapsed{-5min}), "-5m");
    EXPECT_EQ(std::format("{}", Elapsed{24h}), "1d");
    EXPECT_EQ(std::format("[{}|{}]", Elapsed{1s}, Elapsed{61s}), "[1s|1m1s]");
}

TEST(Elapsed, PrintsTheClockForm) {
    EXPECT_EQ(std::format("{:clock}", Elapsed{1h + 30min + 5s}), "01:30:05");
    EXPECT_EQ(std::format("{:clock}", Elapsed{26h}), "26:00:00");
    EXPECT_EQ(std::format("{:clock}", Elapsed{0s}), "00:00:00");
    EXPECT_EQ(std::format("{:clock}", Elapsed{-5s}), "-00:00:05");
    EXPECT_EQ(std::format("{:clock}", Elapsed{100h}), "100:00:00");
}

TEST(Elapsed, RoundTripsThroughParseDuration) {
    for (const std::chrono::seconds value : {1s, 59s, 60s, 3599s, 3600s, 86'399s, 86'400s,
                                             86'401s, 1'000'000s}) {
        EXPECT_EQ(dojo::parse_duration(std::format("{}", Elapsed{value})), value) << value;
    }
}

TEST(Elapsed, RejectsUnknownOptionsAtRunTime) {
    // A literal "{:clok}" would not compile; a run-time format string throws instead.
    const Elapsed one{1s};
    const std::string spec = "{:clok}";
    EXPECT_THROW((void)std::vformat(spec, std::make_format_args(one)), std::format_error);
    const std::string other = "{:x}";
    EXPECT_THROW((void)std::vformat(other, std::make_format_args(one)), std::format_error);
    EXPECT_EQ(std::vformat("{:clock}", std::make_format_args(one)), "00:00:01");
}

TEST(ParseTimestamp, ReadsUtcAndOffsets) {
    using namespace std::chrono;
    EXPECT_EQ(dojo::parse_timestamp("2026-03-29T01:30:00Z"), sys_days{2026y / March / 29} + 90min);
    EXPECT_EQ(dojo::parse_timestamp("2026-03-29T03:30:00+01:00"),
              sys_days{2026y / March / 29} + 150min);
    EXPECT_EQ(dojo::parse_timestamp("2026-01-01T00:00:00-05:30"),
              sys_days{2026y / January / 1} + 5h + 30min);
    EXPECT_EQ(dojo::parse_timestamp("2024-02-29T23:59:59Z"),
              sys_days{2024y / February / 29} + 86'399s);
}

TEST(ParseTimestamp, RejectsImpossibleDatesAndTimes) {
    EXPECT_EQ(timestamp_error("2026-02-29T00:00:00Z"), ParseError::BadDate);
    EXPECT_EQ(timestamp_error("2026-13-01T00:00:00Z"), ParseError::BadDate);
    EXPECT_EQ(timestamp_error("2026-04-31T00:00:00Z"), ParseError::BadDate);
    EXPECT_EQ(timestamp_error("2026-00-10T00:00:00Z"), ParseError::BadDate);
    EXPECT_EQ(timestamp_error("2026-01-01T24:00:00Z"), ParseError::BadTime);
    EXPECT_EQ(timestamp_error("2026-01-01T00:60:00Z"), ParseError::BadTime);
    EXPECT_EQ(timestamp_error("2026-01-01T00:00:60Z"), ParseError::BadTime);
    EXPECT_EQ(timestamp_error("2026-01-01T00:00:00+01:60"), ParseError::BadTime);
}

TEST(ParseTimestamp, RejectsTheWrongShape) {
    EXPECT_EQ(timestamp_error("2026-1-01T00:00:00Z"), ParseError::BadFormat);
    EXPECT_EQ(timestamp_error("2026-01-01 00:00:00Z"), ParseError::BadFormat);
    EXPECT_EQ(timestamp_error("2026-01-01T00:00:00"), ParseError::BadFormat);
    EXPECT_EQ(timestamp_error("2026-01-01T00:00:00z"), ParseError::BadFormat);
    EXPECT_EQ(timestamp_error("2026-01-01T0a:00:00Z"), ParseError::BadFormat);
    EXPECT_EQ(timestamp_error("2026-01-01T+1:00:00Z"), ParseError::BadFormat);
    EXPECT_EQ(timestamp_error("2026-01-01T00:00:00+0100"), ParseError::BadFormat);
    EXPECT_EQ(timestamp_error(""), ParseError::BadFormat);
}

TEST(LocalText, UsesTheTimeZoneDatabase) {
    using namespace std::chrono;
    EXPECT_EQ(dojo::local_text(sys_days{2026y / March / 29} + 1h, "Europe/Lisbon"),
              "2026-03-29 02:00 WEST")
        << "at 01:00 UTC Lisbon jumps from 01:00 WET to 02:00 WEST";
    EXPECT_EQ(dojo::local_text(sys_days{2026y / March / 29} + 59min, "Europe/Lisbon"),
              "2026-03-29 00:59 WET");
    EXPECT_EQ(dojo::local_text(sys_days{2026y / January / 15} + 12h, "America/New_York"),
              "2026-01-15 07:00 EST");
    EXPECT_EQ(dojo::local_text(sys_days{2026y / July / 1} + 3h + 30min, "Asia/Kolkata"),
              "2026-07-01 09:00 IST");
}

TEST(LocalText, UnknownZoneThrows) {
    using namespace std::chrono;
    EXPECT_THROW((void)dojo::local_text(sys_days{2026y / July / 1}, "Mars/Olympus"),
                 std::runtime_error);
}
