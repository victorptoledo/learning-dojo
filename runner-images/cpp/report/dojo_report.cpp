// dojo-report runs a kata test binary built with dojo_main.cpp, adds the results check.sh
// wrote for the compiler warnings, clang-format and clang-tidy, and prints everything in the
// `cargo test` format the dojo reads.
//
// Usage: dojo-report BINARY EXTRA_DIR (run from the kata folder)
//
// A crash (a sanitizer report, a failed libstdc++ assertion, std::terminate) or a timeout
// stops the binary, so the tests after it would never run. When that happens the running
// test gets the reason as its failure, and the binary runs again with a --gtest_filter that
// skips every test that already has a result, until each one has one.
// Exits 0 when everything passed, 1 when something failed, 2 when no test ran.
#include <sys/wait.h>
#include <unistd.h>

#include <algorithm>
#include <chrono>
#include <csignal>
#include <cstring>
#include <cstdlib>
#include <filesystem>
#include <fstream>
#include <iostream>
#include <sstream>
#include <string>
#include <string_view>
#include <thread>
#include <vector>

namespace fs = std::filesystem;

namespace {

constexpr int kMaxRuns = 40;
constexpr int kMaxMessageLines = 8;
constexpr int kMaxLayoutRetries = 25;
constexpr auto kRunLimit = std::chrono::seconds(90);
const std::string kResults = "/target/results.txt";
const std::string kLog = "/target/run.log";
constexpr std::string_view kKataPrefix = "/target/kata/";

struct Result {
    std::string name;
    bool passed = false;
    std::string message;
};

std::string read_file(const std::string& path) {
    std::ifstream in(path, std::ios::binary);
    std::ostringstream text;
    text << in.rdbuf();
    return text.str();
}

std::vector<std::string> split_lines(const std::string& text) {
    std::vector<std::string> lines;
    std::istringstream in(text);
    for (std::string line; std::getline(in, line);) {
        if (!line.empty() && line.back() == '\r') {
            line.pop_back();
        }
        lines.push_back(line);
    }
    return lines;
}

std::string strip_paths(std::string text) {
    for (auto at = text.find(kKataPrefix); at != std::string::npos; at = text.find(kKataPrefix)) {
        text.erase(at, kKataPrefix.size());
    }
    return text;
}

std::string unescape(std::string_view text) {
    std::string out;
    for (std::size_t i = 0; i < text.size(); ++i) {
        if (text[i] == '\\' && i + 1 < text.size()) {
            ++i;
            out += text[i] == 'n' ? '\n' : text[i];
        } else {
            out += text[i];
        }
    }
    return out;
}

std::string trim(std::string_view text) {
    const auto first = text.find_first_not_of(" \t");
    if (first == std::string_view::npos) {
        return {};
    }
    const auto last = text.find_last_not_of(" \t");
    return std::string(text.substr(first, last - first + 1));
}

// The first stack frame in the learner's or the tests' code: "    #1 0x... in fn /target/kata/x.cpp:12"
// (AddressSanitizer) or "    #0 fn /target/kata/x.cpp:13 (binary+0x...)" (ThreadSanitizer).
std::string kata_frame(const std::vector<std::string>& lines, std::size_t from) {
    for (std::size_t i = from; i < lines.size(); ++i) {
        const auto frame = trim(lines[i]);
        const auto at = frame.find(kKataPrefix);
        if (!frame.starts_with("#") || at == std::string::npos) {
            continue;
        }
        auto location = frame.substr(at + kKataPrefix.size());
        location = location.substr(0, location.find(' '));
        // Keep file:line, drop the column.
        if (const auto first = location.find(':'); first != std::string::npos) {
            location = location.substr(0, location.find(':', first + 1));
        }
        auto head = frame.substr(0, at);
        head = head.substr(head.find(' ') + 1);  // drop "#N"
        if (head.starts_with("0x")) {
            const auto in = head.find(" in ");
            head = in == std::string::npos ? "" : head.substr(in + 4);
        }
        const auto function = trim(head);
        return location + (function.empty() ? "" : " in " + function);
    }
    return {};
}

// Why the binary stopped in the middle of a test, from what it printed.
std::string crash_reason(const std::string& log, int status) {
    const auto lines = split_lines(log);
    for (std::size_t i = 0; i < lines.size(); ++i) {
        const auto& line = lines[i];
        if (const auto at = line.find("ERROR: AddressSanitizer: "); at != std::string::npos) {
            auto kind = line.substr(at + 25);
            kind = kind.substr(0, kind.find(" on "));
            const auto frame = kata_frame(lines, i + 1);
            return "AddressSanitizer: " + kind + (frame.empty() ? "" : " at " + frame);
        }
        if (line.find("WARNING: ThreadSanitizer: ") != std::string::npos) {
            auto kind = line.substr(line.find("ThreadSanitizer: ") + 17);
            kind = kind.substr(0, kind.find(" (pid"));
            const auto frame = kata_frame(lines, i + 1);
            return "ThreadSanitizer: " + kind + (frame.empty() ? "" : " at " + frame) +
                   " (two threads touch the same memory without synchronisation)";
        }
        if (const auto at = line.find(": runtime error: "); at != std::string::npos) {
            const auto where = strip_paths(line.substr(0, at));
            return "undefined behaviour: " + line.substr(at + 17) + " (" + where + ")";
        }
        if (const auto at = line.find("Assertion '"); at != std::string::npos) {
            auto condition = line.substr(at + 11);
            condition = condition.substr(0, condition.find('\''));
            return "a libstdc++ check failed: " + condition +
                   " (an index out of range, or front()/back() on an empty container?)";
        }
        if (line.starts_with("terminate called")) {
            for (std::size_t j = i + 1; j < lines.size() && j < i + 3; ++j) {
                const auto what = lines[j].find("what():");
                if (what != std::string::npos) {
                    const auto text = trim(lines[j].substr(what + 7));
                    if (text.starts_with("todo")) {
                        return text;
                    }
                    return "std::terminate: an exception escaped (" + text +
                           "); was it thrown in a thread or a noexcept function?";
                }
            }
            return "std::terminate was called (an exception escaped, or a joinable std::thread "
                   "was destroyed)";
        }
    }
    if (WIFSIGNALED(status)) {
        return "the test crashed with signal " + std::to_string(WTERMSIG(status)) + " (" +
               strsignal(WTERMSIG(status)) + ")";
    }
    return "the test binary stopped with exit status " + std::to_string(WEXITSTATUS(status));
}

std::string leak_reason(const std::string& log) {
    const auto lines = split_lines(log);
    for (std::size_t i = 0; i < lines.size(); ++i) {
        if (lines[i].find("SUMMARY: AddressSanitizer: ") != std::string::npos &&
            lines[i].find("leaked") != std::string::npos) {
            auto summary = lines[i].substr(lines[i].find("SUMMARY: AddressSanitizer: ") + 27);
            // The allocation site is in the first "Direct leak" block.
            for (std::size_t j = 0; j < lines.size(); ++j) {
                if (lines[j].find("leak of") != std::string::npos) {
                    const auto frame = kata_frame(lines, j + 1);
                    if (!frame.empty()) {
                        summary += " First one allocated at " + frame + ".";
                    }
                    break;
                }
            }
            return "memory leak: " + summary + " Every new needs an owner that frees it.";
        }
    }
    return {};
}

struct Run {
    int status = 0;
    bool killed = false;
};

Run run_binary(const std::string& binary, const std::vector<Result>& done) {
    std::ofstream(kResults, std::ios::trunc).close();
    std::string filter = "--gtest_filter=*";
    if (!done.empty()) {
        filter += "-";
        for (std::size_t i = 0; i < done.size(); ++i) {
            filter += (i == 0 ? "" : ":") + done[i].name;
        }
    }
    const pid_t child = fork();
    if (child == 0) {
        std::FILE* log = std::fopen(kLog.c_str(), "w");
        if (log != nullptr) {
            ::dup2(fileno(log), STDOUT_FILENO);
            ::dup2(fileno(log), STDERR_FILENO);
        }
        ::setenv("DOJO_RESULTS", kResults.c_str(), 1);
        std::vector<char*> args{const_cast<char*>(binary.c_str()), filter.data(), nullptr};
        ::execv(binary.c_str(), args.data());
        ::_exit(127);
    }
    Run run;
    const auto deadline = std::chrono::steady_clock::now() + kRunLimit;
    for (;;) {
        const pid_t finished = ::waitpid(child, &run.status, WNOHANG);
        if (finished == child) {
            return run;
        }
        if (std::chrono::steady_clock::now() > deadline) {
            ::kill(child, SIGKILL);
            ::waitpid(child, &run.status, 0);
            run.killed = true;
            return run;
        }
        std::this_thread::sleep_for(std::chrono::milliseconds(20));
    }
}

bool contains(const std::vector<Result>& results, const std::string& name) {
    return std::ranges::any_of(results, [&](const Result& result) { return result.name == name; });
}

std::vector<Result> run_tests(const std::string& binary, std::string& first_log) {
    std::vector<Result> results;
    const bool leak_check = std::getenv("DOJO_LEAK_CHECK") != nullptr;
    int layout_retries = 0;
    for (int attempt = 0; attempt < kMaxRuns; ++attempt) {
        const Run run = run_binary(binary, results);
        const std::string log = read_file(kLog);
        if (attempt == 0) {
            first_log = log;
        }
        std::string running;
        bool timed_out = false;
        bool started_any = false;
        for (const auto& line : split_lines(read_file(kResults))) {
            if (line.starts_with("S\t")) {
                running = line.substr(2);
                started_any = true;
            } else if (line.starts_with("P\t")) {
                results.push_back({line.substr(2), true, {}});
                running.clear();
            } else if (line.starts_with("F\t")) {
                const auto rest = line.substr(2);
                const auto tab = rest.find('\t');
                results.push_back({rest.substr(0, tab), false,
                                   tab == std::string::npos ? "" : unescape(rest.substr(tab + 1))});
                running.clear();
            } else if (line == "T") {
                timed_out = true;
            }
        }
        if (!running.empty() && !contains(results, running)) {
            std::string reason;
            if (timed_out || run.killed) {
                reason = "timed out: look for an infinite loop, a deadlock or a thread never joined";
            } else {
                reason = crash_reason(log, run.status);
            }
            results.push_back({running, false, reason});
            continue;
        }
        if (!started_any) {
            // ThreadSanitizer needs a memory layout that address randomisation sometimes does
            // not give, and the sandbox may not turn randomisation off: just start again.
            if (log.find("incompatible memory layout") != std::string::npos &&
                layout_retries < kMaxLayoutRetries) {
                ++layout_retries;
                --attempt;
                continue;
            }
            break;
        }
        if (leak_check) {
            const auto leak = leak_reason(log);
            results.push_back({"leak_sanitizer", leak.empty(), leak});
        }
        break;
    }
    return results;
}

// check.sh writes one file per extra result: its name, "ok" or "FAILED", then the message.
std::vector<Result> extra_results(const fs::path& dir) {
    std::vector<fs::path> files;
    if (fs::is_directory(dir)) {
        for (const auto& entry : fs::directory_iterator(dir)) {
            files.push_back(entry.path());
        }
    }
    std::ranges::sort(files);
    std::vector<Result> results;
    for (const auto& file : files) {
        const auto lines = split_lines(read_file(file.string()));
        if (lines.size() < 2) {
            continue;
        }
        Result result{lines[0], lines[1] == "ok", {}};
        for (std::size_t i = 2; i < lines.size(); ++i) {
            result.message += (i == 2 ? "" : "\n") + lines[i];
        }
        results.push_back(result);
    }
    return results;
}

// The dojo shows the lines up to the first blank one, so blank lines are dropped.
std::string message_block(const std::string& message) {
    std::string out;
    int count = 0;
    for (const auto& line : split_lines(strip_paths(message))) {
        if (trim(line).empty()) {
            continue;
        }
        if (++count > kMaxMessageLines) {
            out += "...\n";
            break;
        }
        out += line + "\n";
    }
    return out.empty() ? "failed\n" : out;
}

int print_cargo(const std::vector<Result>& results) {
    std::cout << "\nrunning " << results.size() << " tests\n";
    int failed = 0;
    for (const auto& result : results) {
        std::cout << "test " << result.name << " ... " << (result.passed ? "ok" : "FAILED") << "\n";
        failed += result.passed ? 0 : 1;
    }
    if (failed > 0) {
        std::cout << "\nfailures:\n\n";
        for (const auto& result : results) {
            if (!result.passed) {
                std::cout << "---- " << result.name << " stdout ----\n"
                          << "thread '" << result.name << "' panicked at " << result.name << ":\n"
                          << message_block(result.message) << "\n";
            }
        }
        std::cout << "failures:\n\n";
    }
    const auto passed = results.size() - static_cast<std::size_t>(failed);
    std::cout << "test result: " << (failed > 0 ? "FAILED" : "ok") << ". " << passed
              << " passed; " << failed << " failed\n";
    return failed > 0 ? 1 : 0;
}

}  // namespace

int main(int argc, char** argv) {
    if (argc != 3) {
        std::cerr << "usage: dojo-report BINARY EXTRA_DIR\n";
        return 2;
    }
    std::string first_log;
    auto results = run_tests(argv[1], first_log);
    if (results.empty()) {
        std::cout << "error: no tests ran\n";
        const auto lines = split_lines(strip_paths(first_log));
        for (std::size_t i = 0; i < lines.size() && i < 40; ++i) {
            std::cout << lines[i] << "\n";
        }
        return 2;
    }
    std::ranges::move(extra_results(argv[2]), std::back_inserter(results));
    return print_cargo(results);
}
