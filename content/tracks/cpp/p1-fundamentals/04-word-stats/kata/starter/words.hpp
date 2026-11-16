#pragma once

#include <cstddef>
#include <map>
#include <string>
#include <string_view>
#include <vector>

namespace dojo {

struct WordCount {
    std::string word;
    int count = 0;

    bool operator==(const WordCount&) const = default;
};

// Every word of the text, in order and in lower case. A word is a run of ASCII letters or
// digits; an apostrophe between two such characters belongs to the word ("don't").
std::vector<std::string> words(std::string_view text);

// How many times each word appears.
std::map<std::string, int> count(const std::vector<std::string>& words);

// The n most frequent words, most frequent first; equal counts in alphabetical order.
std::vector<WordCount> top(std::string_view text, std::size_t n);

// Each different word once, in the order it first appears.
std::vector<std::string> first_seen(std::string_view text);

// Each word length mapped to the different words of that length, sorted.
std::map<std::size_t, std::vector<std::string>> by_length(std::string_view text);

}  // namespace dojo
