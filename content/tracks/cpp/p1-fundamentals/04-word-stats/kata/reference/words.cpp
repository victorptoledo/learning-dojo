#include "words.hpp"

#include <algorithm>
#include <cctype>
#include <set>
#include <unordered_set>

namespace dojo {

namespace {

bool is_word_char(char c) {
    return std::isalnum(static_cast<unsigned char>(c)) != 0;
}

char lower(char c) {
    return static_cast<char>(std::tolower(static_cast<unsigned char>(c)));
}

}  // namespace

std::vector<std::string> words(std::string_view text) {
    std::vector<std::string> result;
    std::string current;
    for (std::size_t i = 0; i < text.size(); ++i) {
        const char c = text[i];
        const bool inner_apostrophe =
            c == '\'' && !current.empty() && i + 1 < text.size() && is_word_char(text[i + 1]);
        if (is_word_char(c) || inner_apostrophe) {
            current += lower(c);
        } else if (!current.empty()) {
            result.push_back(std::move(current));
            current.clear();
        }
    }
    if (!current.empty()) {
        result.push_back(std::move(current));
    }
    return result;
}

std::map<std::string, int> count(const std::vector<std::string>& words) {
    std::map<std::string, int> counts;
    for (const auto& word : words) {
        ++counts[word];
    }
    return counts;
}

std::vector<WordCount> top(std::string_view text, std::size_t n) {
    std::vector<WordCount> ranking;
    for (const auto& [word, times] : count(words(text))) {
        ranking.push_back({word, times});
    }
    std::ranges::sort(ranking, [](const WordCount& a, const WordCount& b) {
        if (a.count != b.count) {
            return a.count > b.count;
        }
        return a.word < b.word;
    });
    ranking.resize(std::min(n, ranking.size()));
    return ranking;
}

std::vector<std::string> first_seen(std::string_view text) {
    std::vector<std::string> order;
    std::unordered_set<std::string> seen;
    for (auto& word : words(text)) {
        if (seen.insert(word).second) {
            order.push_back(std::move(word));
        }
    }
    return order;
}

std::map<std::size_t, std::vector<std::string>> by_length(std::string_view text) {
    std::map<std::size_t, std::set<std::string>> sets;
    for (auto& word : words(text)) {
        const auto length = word.size();
        sets[length].insert(std::move(word));
    }
    std::map<std::size_t, std::vector<std::string>> groups;
    for (const auto& [length, distinct] : sets) {
        groups[length] = {distinct.begin(), distinct.end()};
    }
    return groups;
}

}  // namespace dojo
