#include <gtest/gtest.h>

#include <filesystem>
#include <fstream>
#include <optional>
#include <string>
#include <string_view>
#include <vector>

#include "config.hpp"

using dojo::Config;
using dojo::ErrorKind;
using dojo::LoadError;
namespace fs = std::filesystem;

namespace {

// A folder of its own for each test, removed at the end.
class Folder {
public:
    Folder() : path_(fs::path(testing::TempDir()) / unique_name()) {
        fs::remove_all(path_);
        fs::create_directories(path_);
    }
    ~Folder() {
        std::error_code ignored;
        fs::remove_all(path_, ignored);
    }
    Folder(const Folder&) = delete;
    Folder& operator=(const Folder&) = delete;

    fs::path write(const std::string& name, std::string_view text) const {
        const fs::path file = path_ / name;
        fs::create_directories(file.parent_path());
        std::ofstream(file, std::ios::binary) << text;
        return file;
    }

    const fs::path& path() const { return path_; }

private:
    static std::string unique_name() {
        const auto* test = testing::UnitTest::GetInstance()->current_test_info();
        return std::string("dojo-ini-") + test->test_suite_name() + "-" + test->name();
    }

    fs::path path_;
};

std::optional<LoadError> load_error(const fs::path& file) {
    const auto result = Config::load(file);
    return result ? std::nullopt : std::optional<LoadError>(result.error());
}

}  // namespace

TEST(Stage3Files, LoadsAFile) {
    const Folder folder;
    const auto file = folder.write("app.ini", "[server]\nport = 8080\n");
    const auto config = Config::load(file);
    ASSERT_TRUE(config.has_value());
    EXPECT_EQ(config->get<int>("server", "port"), 8080);
    EXPECT_EQ(*config, Config::parse("[server]\nport = 8080\n").value_or(Config{}));
}

TEST(Stage3Files, AMissingFileCannotBeOpened) {
    const Folder folder;
    const fs::path missing = folder.path() / "missing.ini";
    EXPECT_EQ(load_error(missing),
              (LoadError{.file = missing, .line = 0, .kind = ErrorKind::CannotOpen}));
}

TEST(Stage3Files, ParseErrorsNameTheFileAndLine) {
    const Folder folder;
    const auto file = folder.write("bad.ini", "[a]\nx = 1\noops\n");
    EXPECT_EQ(load_error(file),
              (LoadError{.file = file, .line = 3, .kind = ErrorKind::MissingEquals}));
}

TEST(Stage3Files, IncludeReadsAnotherFileInPlace) {
    const Folder folder;
    folder.write("db.ini", "[db]\nhost = localhost\n");
    const auto main = folder.write("main.ini", "name = app\n@include db.ini\nport = 5432\n");
    const auto config = Config::load(main);
    ASSERT_TRUE(config.has_value());
    EXPECT_EQ(config->raw("", "name"), "app");
    EXPECT_EQ(config->raw("db", "host"), "localhost");
    EXPECT_EQ(config->raw("db", "port"), "5432") << "the section the included file opened goes on";
}

TEST(Stage3Files, IncludePathsAreRelativeToTheIncludingFile) {
    const Folder folder;
    folder.write("conf.d/extra/log.ini", "[log]\nlevel = debug\n");
    folder.write("conf.d/base.ini", "[base]\nx = 1\n@include extra/log.ini\n");
    const auto main = folder.write("main.ini", "  @include   conf.d/base.ini  \n");
    const auto config = Config::load(main);
    ASSERT_TRUE(config.has_value());
    EXPECT_EQ(config->raw("base", "x"), "1");
    EXPECT_EQ(config->raw("log", "level"), "debug");
}

TEST(Stage3Files, AMissingIncludeIsReportedWhereItIsIncluded) {
    const Folder folder;
    const auto main = folder.write("main.ini", "[a]\nx = 1\n@include nowhere.ini\n");
    EXPECT_EQ(load_error(main),
              (LoadError{.file = main, .line = 3, .kind = ErrorKind::CannotOpen}));
}

TEST(Stage3Files, ErrorsInsideAnIncludedFileNameThatFile) {
    const Folder folder;
    const auto inner = folder.write("inner.ini", "# fine\n[bad name]\n");
    const auto main = folder.write("main.ini", "@include inner.ini\n");
    EXPECT_EQ(load_error(main),
              (LoadError{.file = inner, .line = 2, .kind = ErrorKind::BadSection}));
}

TEST(Stage3Files, DuplicateKeysAcrossFilesAreDuplicates) {
    const Folder folder;
    const auto inner = folder.write("inner.ini", "[a]\nx = 2\n");
    const auto main = folder.write("main.ini", "[a]\nx = 1\n@include inner.ini\n");
    EXPECT_EQ(load_error(main),
              (LoadError{.file = inner, .line = 2, .kind = ErrorKind::DuplicateKey}));
}

TEST(Stage3Files, IncludeCyclesAreFound) {
    const Folder folder;
    const auto self = folder.write("self.ini", "x = 1\n@include self.ini\n");
    EXPECT_EQ(load_error(self),
              (LoadError{.file = self, .line = 2, .kind = ErrorKind::IncludeCycle}));

    folder.write("a.ini", "[a]\n@include b.ini\n");
    const auto b = folder.write("b.ini", "[b]\n\n@include sub/../a.ini\n");
    EXPECT_EQ(load_error(folder.path() / "a.ini"),
              (LoadError{.file = b, .line = 3, .kind = ErrorKind::IncludeCycle}));
}

TEST(Stage3Files, TheSameFileTwiceIsNotACycle) {
    const Folder folder;
    folder.write("common.ini", "shared = yes\n");
    folder.write("one.ini", "[one]\n@include common.ini\n");
    folder.write("two.ini", "[two]\n@include common.ini\n");
    const auto main = folder.write("main.ini", "@include one.ini\n@include two.ini\n");
    const auto config = Config::load(main);
    ASSERT_TRUE(config.has_value());
    EXPECT_EQ(config->raw("one", "shared"), "yes");
    EXPECT_EQ(config->raw("two", "shared"), "yes");
}
