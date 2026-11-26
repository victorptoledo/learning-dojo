#include <gtest/gtest.h>

#include <algorithm>
#include <atomic>
#include <chrono>
#include <filesystem>
#include <format>
#include <fstream>
#include <iterator>
#include <random>
#include <string>
#include <vector>

#include "retention.hpp"

namespace fs = std::filesystem;
using namespace std::chrono;
using dojo::Backup;
using dojo::Plan;
using dojo::Policy;

namespace {

// A fresh folder under the system's temporary folder, removed with everything in it.
class TempDir {
public:
    TempDir() {
        static std::atomic<int> counter{0};
        path_ = fs::temp_directory_path() /
                std::format("dojo-retention-{}-{}", std::random_device{}(), ++counter);
        fs::create_directories(path_);
    }
    TempDir(const TempDir&) = delete;
    TempDir& operator=(const TempDir&) = delete;
    ~TempDir() {
        std::error_code ec;
        fs::permissions(path_, fs::perms::owner_all, ec);
        fs::remove_all(path_, ec);
    }
    const fs::path& path() const { return path_; }
    fs::path touch(const std::string& name) const {
        std::ofstream(path_ / name) << name;
        return path_ / name;
    }

private:
    fs::path path_;
};

std::string read_file(const fs::path& path) {
    std::ifstream in(path, std::ios::binary);
    return {std::istreambuf_iterator<char>(in), std::istreambuf_iterator<char>()};
}

std::vector<std::string> names(const std::vector<fs::path>& paths) {
    std::vector<std::string> out;
    for (const fs::path& p : paths) {
        out.push_back(p.filename().string());
    }
    return out;
}

std::vector<std::string> kept(const Plan& plan) {
    std::vector<std::string> out;
    for (const dojo::Decision& d : plan.keep) {
        out.push_back(std::format("{} {}", d.path.filename().string(), d.reason));
    }
    return out;
}

// A backup taken at a local wall time, as if the zone were UTC (plan only needs the order).
Backup at(year_month_day date, hours h) {
    const local_days day{date};
    const auto ymd = date;
    return Backup{.path = fs::path("/backups") /
                          std::format("db-{:%Y%m%d}-{:%H}00.bak", sys_days{ymd}, h),
                  .taken = sys_days{date} + h,
                  .day = day};
}

const time_zone* lisbon() { return locate_zone("Europe/Lisbon"); }

}  // namespace

TEST(Scan, ReadsBackupsNewestFirstAndSkipsOtherFiles) {
    const TempDir dir;
    dir.touch("db-20261125-0300.bak");
    dir.touch("db-20261126-0300.bak");
    dir.touch("notes.txt");
    dir.touch("db-2026112-0300.bak");
    fs::create_directory(dir.path() / "old");
    std::ofstream(dir.path() / "old" / "db-20260101-0300.bak") << "x";
    fs::create_directory(dir.path() / "db-20261127-0300.bak");  // a folder, not a file
    const auto result = dojo::scan(dir.path(), lisbon());
    ASSERT_TRUE(result.has_value());
    std::vector<fs::path> found;
    for (const Backup& b : result->backups) {
        found.push_back(b.path);
    }
    EXPECT_EQ(names(found), (std::vector<std::string>{"db-20261126-0300.bak", "db-20261125-0300.bak"}));
    EXPECT_EQ(names(result->skipped), (std::vector<std::string>{"db-2026112-0300.bak", "notes.txt"}));
    EXPECT_EQ(result->backups[0].path, dir.path() / "db-20261126-0300.bak");
}

TEST(Scan, NamesAreLocalTimeInTheZone) {
    const TempDir dir;
    dir.touch("db-20260701-0300.bak");  // summer: WEST, UTC+1
    dir.touch("db-20260115-0300.bak");  // winter: WET, UTC+0
    const auto result = dojo::scan(dir.path(), lisbon());
    ASSERT_TRUE(result.has_value());
    ASSERT_EQ(result->backups.size(), 2U);
    EXPECT_EQ(result->backups[0].taken, sys_days{2026y / July / 1} + 2h);
    EXPECT_EQ(result->backups[0].day, local_days{2026y / July / 1});
    EXPECT_EQ(result->backups[1].taken, sys_days{2026y / January / 15} + 3h);
    const auto tokyo = dojo::scan(dir.path(), locate_zone("Asia/Tokyo"));
    ASSERT_TRUE(tokyo.has_value());
    EXPECT_EQ(tokyo->backups[0].taken, sys_days{2026y / June / 30} + 18h);
    EXPECT_EQ(tokyo->backups[0].day, local_days{2026y / July / 1}) << "the day is the local one";
}

TEST(Scan, ATimeThatHappensTwiceIsTheEarlier) {
    const TempDir dir;
    dir.touch("db-20261025-0130.bak");  // 01:30 WEST, then 01:30 WET an hour later
    const auto result = dojo::scan(dir.path(), lisbon());
    ASSERT_TRUE(result.has_value());
    ASSERT_EQ(result->backups.size(), 1U);
    EXPECT_EQ(result->backups[0].taken, sys_days{2026y / October / 25} + 30min);
}

TEST(Scan, ATimeThatNeverHappensIsSkipped) {
    const TempDir dir;
    dir.touch("db-20260329-0130.bak");  // 01:00 jumps to 02:00 that night
    dir.touch("db-20260329-0230.bak");
    const auto result = dojo::scan(dir.path(), lisbon());
    ASSERT_TRUE(result.has_value());
    EXPECT_EQ(names(result->skipped), (std::vector<std::string>{"db-20260329-0130.bak"}));
    ASSERT_EQ(result->backups.size(), 1U);
    EXPECT_EQ(result->backups[0].taken, sys_days{2026y / March / 29} + 1h + 30min);
}

TEST(Scan, RejectsNamesThatAreAlmostBackups) {
    const TempDir dir;
    for (const char* name : {"db-20260230-0300.bak", "db-20260101-2400.bak", "db-20260101-0360.bak",
                             "-20260101-0300.bak", "db-20260101-0300.BAK", "db_20260101_0300.bak",
                             "db-2026010a-0300.bak", "db-20260101-0300.bak.gz", "db-202601010300.bak",
                             "db-20260101-+300.bak"}) {
        dir.touch(name);
    }
    dir.touch("x-20260101-0300.bak");
    const auto result = dojo::scan(dir.path(), lisbon());
    ASSERT_TRUE(result.has_value());
    ASSERT_EQ(result->backups.size(), 1U);
    EXPECT_EQ(result->backups[0].path.filename(), "x-20260101-0300.bak");
    EXPECT_EQ(result->skipped.size(), 10U);
}

TEST(Scan, ReportsFolderErrorsWithoutThrowing) {
    const TempDir dir;
    const fs::path file = dir.touch("plain.txt");
    EXPECT_EQ(dojo::scan(dir.path() / "missing", lisbon()).error(), dojo::ScanError::NotADirectory);
    EXPECT_EQ(dojo::scan(file, lisbon()).error(), dojo::ScanError::NotADirectory);
    const fs::path locked = dir.path() / "locked";
    fs::create_directory(locked);
    fs::permissions(locked, fs::perms::none);
    const auto result = dojo::scan(locked, lisbon());
    fs::permissions(locked, fs::perms::owner_all);
    ASSERT_FALSE(result.has_value());
    EXPECT_EQ(result.error(), dojo::ScanError::Unreadable);
}

TEST(Plan, KeepsDailyWeeklyAndMonthly) {
    std::vector<Backup> backups;
    for (sys_days d = sys_days{2026y / August / 1}; d <= sys_days{2026y / November / 26}; d += days{1}) {
        backups.push_back(at(year_month_day{d}, 3h));
    }
    backups.push_back(at(2026y / November / 26, 15h));  // a second backup that day
    const Plan result = dojo::plan(backups, Policy{});
    EXPECT_EQ(kept(result), (std::vector<std::string>{
                                "db-20261126-1500.bak daily+weekly+monthly",
                                "db-20261125-0300.bak daily",
                                "db-20261124-0300.bak daily",
                                "db-20261123-0300.bak daily",
                                "db-20261122-0300.bak daily+weekly",
                                "db-20261121-0300.bak daily",
                                "db-20261120-0300.bak daily",
                                "db-20261115-0300.bak weekly",
                                "db-20261108-0300.bak weekly",
                                "db-20261031-0300.bak monthly",
                                "db-20260930-0300.bak monthly",
                                "db-20260831-0300.bak monthly",
                            }));
    ASSERT_EQ(result.remove.size(), 107U);
    EXPECT_EQ(result.remove.front().filename(), "db-20261126-0300.bak");
    EXPECT_EQ(result.remove.back().filename(), "db-20260801-0300.bak");
}

TEST(Plan, IsoWeeksBelongToTheirIsoYear) {
    // 2025-12-28 is a Sunday in week 52 of 2025; 2025-12-29 (Monday) and 2026-01-02 are both
    // in week 1 of 2026.
    const std::vector<Backup> backups{at(2025y / December / 28, 3h), at(2025y / December / 29, 3h),
                                      at(2026y / January / 2, 3h)};
    const Plan result = dojo::plan(backups, Policy{.daily = 0, .weekly = 2, .monthly = 0});
    EXPECT_EQ(kept(result), (std::vector<std::string>{"db-20260102-0300.bak weekly",
                                                      "db-20251228-0300.bak weekly"}));
    EXPECT_EQ(names(result.remove), (std::vector<std::string>{"db-20251229-0300.bak"}));
}

TEST(Plan, CountsDaysThatHaveBackupsNotCalendarDays) {
    const std::vector<Backup> backups{at(2026y / November / 1, 3h), at(2026y / November / 10, 3h),
                                      at(2026y / November / 20, 3h)};
    const Plan result = dojo::plan(backups, Policy{.daily = 2, .weekly = 0, .monthly = 0});
    EXPECT_EQ(kept(result), (std::vector<std::string>{"db-20261120-0300.bak daily",
                                                      "db-20261110-0300.bak daily"}));
}

TEST(Plan, AcceptsAnyOrderAndRulesCanBeOff) {
    std::vector<Backup> backups;
    for (int i = 1; i <= 20; ++i) {
        backups.push_back(at(2026y / October / day{static_cast<unsigned>(i)}, 3h));
    }
    const Plan sorted = dojo::plan(backups, Policy{});
    std::ranges::shuffle(backups, std::mt19937{42});
    const Plan shuffled = dojo::plan(backups, Policy{});
    EXPECT_EQ(kept(shuffled), kept(sorted));
    EXPECT_EQ(shuffled.remove, sorted.remove);
    const Plan none = dojo::plan(backups, Policy{.daily = 0, .weekly = 0, .monthly = 0});
    EXPECT_TRUE(none.keep.empty());
    ASSERT_EQ(none.remove.size(), 20U);
    EXPECT_EQ(none.remove.front().filename(), "db-20261020-0300.bak");
}

TEST(Report, LinesArePaddedAndSummarised) {
    const Plan sample{
        .keep = {{"/b/db-20261126-0300.bak", "daily+weekly"}, {"/b/a-20261120-0300.bak", "daily"}},
        .remove = {"/b/db-20261101-0300.bak"},
    };
    EXPECT_EQ(dojo::report(sample),
              "keep   db-20261126-0300.bak  daily+weekly\n"
              "keep   a-20261120-0300.bak   daily\n"
              "remove db-20261101-0300.bak\n"
              "2 kept, 1 removed\n");
    EXPECT_EQ(dojo::report(Plan{}), "0 kept, 0 removed\n");
}

TEST(Apply, RemovesFilesAndWritesTheLog) {
    const TempDir dir;
    const fs::path keep = dir.touch("db-20261126-0300.bak");
    const fs::path old1 = dir.touch("db-20261101-0300.bak");
    const fs::path old2 = dir.touch("db-20261001-0300.bak");
    const Plan sample{.keep = {{keep, "daily"}}, .remove = {old1, old2}};
    const auto removed = dojo::apply(dir.path(), sample);
    ASSERT_TRUE(removed.has_value()) << removed.error().message();
    EXPECT_EQ(*removed, 2U);
    EXPECT_TRUE(fs::exists(keep));
    EXPECT_FALSE(fs::exists(old1));
    EXPECT_FALSE(fs::exists(old2));
    EXPECT_EQ(read_file(dir.path() / "retention.log"), dojo::report(sample));
    std::vector<std::string> left;
    for (const auto& entry : fs::directory_iterator(dir.path())) {
        left.push_back(entry.path().filename().string());
    }
    std::ranges::sort(left);
    EXPECT_EQ(left, (std::vector<std::string>{"db-20261126-0300.bak", "retention.log"}))
        << "the temporary file must be gone";
}

TEST(Apply, AFileAlreadyGoneIsFineAndTheOldLogIsReplaced) {
    const TempDir dir;
    std::ofstream(dir.path() / "retention.log") << "an old, much longer report that must not survive\n";
    const fs::path old = dir.touch("db-20261101-0300.bak");
    const Plan sample{.keep = {}, .remove = {old, dir.path() / "db-20261002-0300.bak"}};
    const auto removed = dojo::apply(dir.path(), sample);
    ASSERT_TRUE(removed.has_value());
    EXPECT_EQ(*removed, 1U);
    EXPECT_EQ(read_file(dir.path() / "retention.log"), dojo::report(sample));
}

TEST(Apply, ReportsErrorsWithoutThrowing) {
    const TempDir dir;
    const fs::path locked = dir.path() / "locked";
    fs::create_directory(locked);
    std::ofstream(locked / "db-20261101-0300.bak") << "x";
    fs::permissions(locked, fs::perms::owner_read | fs::perms::owner_exec);  // no deleting inside
    std::expected<std::size_t, std::error_code> result;
    EXPECT_NO_THROW(result = dojo::apply(locked, Plan{.keep = {}, .remove = {locked / "db-20261101-0300.bak"}}));
    fs::permissions(locked, fs::perms::owner_all);
    ASSERT_FALSE(result.has_value());
    EXPECT_EQ(result.error(), std::errc::permission_denied);
    EXPECT_TRUE(fs::exists(locked / "db-20261101-0300.bak"));
    EXPECT_FALSE(fs::exists(locked / "retention.log")) << "no log after a failed removal";
}

TEST(EndToEnd, ScanPlanApplyAndScanAgain) {
    const TempDir dir;
    for (sys_days d = sys_days{2026y / September / 1}; d <= sys_days{2026y / November / 26}; d += days{1}) {
        dir.touch(std::format("db-{:%Y%m%d}-0300.bak", d));
    }
    dir.touch("README.txt");
    const auto first = dojo::scan(dir.path(), lisbon());
    ASSERT_TRUE(first.has_value());
    const Plan decided = dojo::plan(first->backups, Policy{.daily = 3, .weekly = 2, .monthly = 3});
    const auto removed = dojo::apply(dir.path(), decided);
    ASSERT_TRUE(removed.has_value());
    EXPECT_EQ(*removed, first->backups.size() - decided.keep.size());
    const auto second = dojo::scan(dir.path(), lisbon());
    ASSERT_TRUE(second.has_value());
    EXPECT_EQ(second->backups.size(), decided.keep.size());
    EXPECT_EQ(names(second->skipped), (std::vector<std::string>{"README.txt", "retention.log"}));
    EXPECT_EQ(kept(dojo::plan(second->backups, Policy{.daily = 3, .weekly = 2, .monthly = 3})),
              kept(decided))
        << "applying the same policy again keeps the same files";
}
