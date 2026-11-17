#include "config.hpp"

#include <algorithm>
#include <cctype>
#include <fstream>
#include <ranges>
#include <sstream>

namespace dojo {

namespace {

bool is_blank(char c) {
    return c == ' ' || c == '\t';
}

std::string_view trim(std::string_view text) {
    while (!text.empty() && is_blank(text.front())) {
        text.remove_prefix(1);
    }
    while (!text.empty() && (is_blank(text.back()) || text.back() == '\r')) {
        text.remove_suffix(1);
    }
    return text;
}

bool is_name(std::string_view name) {
    return !name.empty() && std::ranges::all_of(name, [](char c) {
        return std::isalnum(static_cast<unsigned char>(c)) != 0 || c == '_' || c == '-' || c == '.';
    });
}

// The lines of a text, without their line breaks.
std::vector<std::string_view> lines_of(std::string_view text) {
    std::vector<std::string_view> lines;
    for (const auto line : std::views::split(text, '\n')) {
        lines.emplace_back(line.begin(), line.end());
    }
    if (!lines.empty() && lines.back().empty()) {
        lines.pop_back();  // the text ended with a line break
    }
    return lines;
}

std::optional<std::string> read_file(const std::filesystem::path& file) {
    std::ifstream in(file, std::ios::binary);
    if (!in) {
        return std::nullopt;
    }
    std::ostringstream text;
    text << in.rdbuf();
    return std::move(text).str();
}

}  // namespace

namespace detail {

std::expected<bool, GetError> to_bool(std::string_view text) {
    std::string lower(text);
    std::ranges::transform(lower, lower.begin(),
                           [](unsigned char c) { return static_cast<char>(std::tolower(c)); });
    if (lower == "true" || lower == "yes" || lower == "on" || lower == "1") {
        return true;
    }
    if (lower == "false" || lower == "no" || lower == "off" || lower == "0") {
        return false;
    }
    return std::unexpected(GetError::BadValue);
}

}  // namespace detail

// Reads lines one at a time into a Config, remembering the open section.
class Parser {
public:
    explicit Parser(Config& config) : config_(config) {}

    std::optional<ErrorKind> line(std::string_view text) {
        text = trim(text);
        if (text.empty() || text.front() == '#' || text.front() == ';') {
            return std::nullopt;
        }
        if (text.front() == '[') {
            if (text.back() != ']') {
                return ErrorKind::BadSection;
            }
            const auto name = trim(text.substr(1, text.size() - 2));
            if (!is_name(name)) {
                return ErrorKind::BadSection;
            }
            section_ = &config_.open(name) - config_.sections_.data();
            return std::nullopt;
        }
        const std::size_t equals = text.find('=');
        if (equals == std::string_view::npos) {
            return ErrorKind::MissingEquals;
        }
        const auto key = trim(text.substr(0, equals));
        if (!is_name(key)) {
            return ErrorKind::BadKey;
        }
        auto& entries = config_.sections_[static_cast<std::size_t>(section_)].entries;
        if (std::ranges::contains(entries, key, &Config::Entry::first)) {
            return ErrorKind::DuplicateKey;
        }
        entries.emplace_back(std::string(key), std::string(trim(text.substr(equals + 1))));
        return std::nullopt;
    }

    // Reads `file` and the files it includes; `reading` holds the files being read.
    std::optional<LoadError> read(const std::filesystem::path& file,
                                  std::vector<std::filesystem::path>& reading) {
        const auto text = read_file(file);
        if (!text) {
            return LoadError{.file = file, .line = 0, .kind = ErrorKind::CannotOpen};
        }
        reading.push_back(std::filesystem::weakly_canonical(file));
        const auto lines = lines_of(*text);
        for (std::size_t i = 0; i < lines.size(); ++i) {
            const auto error = [&](ErrorKind kind) {
                return LoadError{.file = file, .line = i + 1, .kind = kind};
            };
            const auto trimmed = trim(lines[i]);
            constexpr std::string_view kInclude = "@include";
            if (trimmed.starts_with(kInclude)) {
                const auto target = trim(trimmed.substr(kInclude.size()));
                const auto path = file.parent_path() / std::filesystem::path(target);
                if (std::ranges::contains(reading, std::filesystem::weakly_canonical(path))) {
                    return error(ErrorKind::IncludeCycle);
                }
                if (!std::filesystem::is_regular_file(path)) {
                    return error(ErrorKind::CannotOpen);
                }
                if (auto failed = read(path, reading)) {
                    return failed;
                }
                continue;
            }
            if (const auto kind = line(lines[i])) {
                return error(*kind);
            }
        }
        reading.pop_back();
        return std::nullopt;
    }

private:
    Config& config_;
    std::ptrdiff_t section_ = 0;  // an index: opening a section may move the others
};

std::expected<Config, ParseError> Config::parse(std::string_view text) {
    Config config;
    Parser parser(config);
    const auto lines = lines_of(text);
    for (std::size_t i = 0; i < lines.size(); ++i) {
        if (const auto kind = parser.line(lines[i])) {
            return std::unexpected(ParseError{.line = i + 1, .kind = *kind});
        }
    }
    return config;
}

std::expected<Config, LoadError> Config::load(const std::filesystem::path& file) {
    Config config;
    Parser parser(config);
    std::vector<std::filesystem::path> reading;
    if (auto error = parser.read(file, reading)) {
        return std::unexpected(std::move(*error));
    }
    return config;
}

const Config::Section* Config::find(std::string_view name) const {
    const auto found = std::ranges::find(sections_, name, &Section::name);
    return found == sections_.end() ? nullptr : &*found;
}

Config::Section& Config::open(std::string_view name) {
    const auto found = std::ranges::find(sections_, name, &Section::name);
    if (found != sections_.end()) {
        return *found;
    }
    return sections_.emplace_back(Section{.name = std::string(name), .entries = {}});
}

std::optional<std::string> Config::raw(std::string_view section, std::string_view key) const {
    const Section* const found = find(section);
    if (found == nullptr) {
        return std::nullopt;
    }
    const auto entry = std::ranges::find(found->entries, key, &Entry::first);
    if (entry == found->entries.end()) {
        return std::nullopt;
    }
    return entry->second;
}

std::vector<std::string> Config::sections() const {
    std::vector<std::string> names;
    for (const auto& section : sections_) {
        if (!section.name.empty() || !section.entries.empty()) {
            names.push_back(section.name);
        }
    }
    return names;
}

std::vector<std::string> Config::keys(std::string_view section) const {
    std::vector<std::string> names;
    if (const Section* const found = find(section)) {
        for (const auto& entry : found->entries) {
            names.push_back(entry.first);
        }
    }
    return names;
}

}  // namespace dojo
