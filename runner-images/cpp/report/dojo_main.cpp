// The main() of every C++ kata test binary in the dojo-cpp image.
//
// With DOJO_RESULTS set, it replaces GoogleTest's printer with a listener that appends one
// record per event to that file, written straight away so nothing is lost when a test crashes:
//
//   S<TAB>name            a test started
//   P<TAB>name            it passed (or was skipped)
//   F<TAB>name<TAB>text   it failed; text has \n and \\ escaped
//   T                     the running test passed DOJO_TEST_TIMEOUT seconds (default 10)
//
// dojo-report reads the file, reruns the binary after a crash and prints the `cargo test`
// format. Without DOJO_RESULTS the binary behaves like a normal GoogleTest binary.
#include <gtest/gtest.h>

#include <fcntl.h>
#include <unistd.h>

#include <atomic>
#include <chrono>
#include <cstdlib>
#include <string>
#include <string_view>
#include <thread>

namespace {

int results_fd = -1;
std::atomic<long long> deadline_ms{0};

long long now_ms() {
    using namespace std::chrono;
    return duration_cast<milliseconds>(steady_clock::now().time_since_epoch()).count();
}

void write_all(std::string_view text) {
    while (!text.empty()) {
        const auto written = ::write(results_fd, text.data(), text.size());
        if (written <= 0) {
            return;
        }
        text.remove_prefix(static_cast<std::size_t>(written));
    }
}

std::string escape(std::string_view text) {
    std::string out;
    for (const char c : text) {
        if (c == '\\') {
            out += "\\\\";
        } else if (c == '\n') {
            out += "\\n";
        } else if (c == '\t' || c == '\r') {
            out += ' ';
        } else {
            out += c;
        }
    }
    return out;
}

// GoogleTest reports an escaped exception as `C++ exception with description "..." thrown in
// the test body.`; keep only the description, so a "todo: ..." placeholder reads as one.
std::string clean(std::string_view summary) {
    constexpr std::string_view prefix = "C++ exception with description \"";
    if (summary.starts_with(prefix)) {
        auto rest = summary.substr(prefix.size());
        const auto end = rest.rfind("\" thrown in");
        if (end != std::string_view::npos) {
            const auto description = rest.substr(0, end);
            if (description.starts_with("todo")) {
                return std::string(description);
            }
            return "uncaught exception: " + std::string(description);
        }
    }
    // EXPECT_THROW that met the placeholder: `... Actual: it throws std::logic_error with
    // description "todo: ...".`
    constexpr std::string_view todo = "with description \"todo";
    if (const auto at = summary.find(todo); at != std::string_view::npos) {
        auto description = summary.substr(at + todo.size() - 4);
        return std::string(description.substr(0, description.find('"')));
    }
    while (summary.ends_with('\n')) {
        summary.remove_suffix(1);
    }
    return std::string(summary);
}

class RecordListener : public testing::EmptyTestEventListener {
public:
    void OnTestStart(const testing::TestInfo& info) override {
        failures_.clear();
        write_all("S\t" + name(info) + "\n");
        deadline_ms.store(now_ms() + timeout_ms(), std::memory_order_release);
    }

    void OnTestPartResult(const testing::TestPartResult& result) override {
        if (!result.failed()) {
            return;
        }
        if (!failures_.empty()) {
            failures_ += '\n';
        }
        failures_ += clean(result.summary());
    }

    void OnTestEnd(const testing::TestInfo& info) override {
        deadline_ms.store(0, std::memory_order_release);
        if (info.result()->Failed()) {
            write_all("F\t" + name(info) + "\t" + escape(failures_) + "\n");
        } else {
            write_all("P\t" + name(info) + "\n");
        }
    }

private:
    static std::string name(const testing::TestInfo& info) {
        return std::string(info.test_suite_name()) + "." + info.name();
    }

    static long long timeout_ms() {
        const char* value = std::getenv("DOJO_TEST_TIMEOUT");
        const long long seconds = value != nullptr ? std::atoll(value) : 0;
        return (seconds > 0 ? seconds : 10) * 1000;
    }

    std::string failures_;
};

// Ends the process when one test runs too long. It does not read the test's name (the main
// thread may be writing it), only the deadline: dojo-report knows which test was running.
[[noreturn]] void watchdog() {
    for (;;) {
        std::this_thread::sleep_for(std::chrono::milliseconds(100));
        const long long deadline = deadline_ms.load(std::memory_order_acquire);
        if (deadline != 0 && now_ms() > deadline) {
            write_all("T\n");
            ::_exit(124);
        }
    }
}

}  // namespace

int main(int argc, char** argv) {
    testing::InitGoogleTest(&argc, argv);
    if (const char* path = std::getenv("DOJO_RESULTS"); path != nullptr) {
        results_fd = ::open(path, O_WRONLY | O_CREAT | O_APPEND | O_CLOEXEC, 0644);
        if (results_fd < 0) {
            return 3;
        }
        auto& listeners = testing::UnitTest::GetInstance()->listeners();
        delete listeners.Release(listeners.default_result_printer());
        listeners.Append(new RecordListener);
        std::thread(watchdog).detach();
    }
    return RUN_ALL_TESTS();
}
