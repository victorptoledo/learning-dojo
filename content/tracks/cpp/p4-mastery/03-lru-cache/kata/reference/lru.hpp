#pragma once

#include <cstddef>
#include <functional>
#include <list>
#include <stdexcept>
#include <unordered_map>
#include <utility>
#include <vector>

namespace dojo {

// A cache of at most `capacity` entries that evicts the least recently used one.
// get, put and erase must take O(1) time on average: the tests run hundreds of thousands
// of operations on a large cache. The usual layout is a std::list of entries, most recent
// first, plus a std::unordered_map from each key to its position in the list.
template <typename K, typename V, typename Hash = std::hash<K>>
class LruCache {
public:
    // Called with the key and value of each entry the cache evicts to make room.
    using OnEvict = std::function<void(const K&, const V&)>;

    // std::invalid_argument when capacity is 0.
    explicit LruCache(std::size_t capacity, OnEvict on_evict = {})
        : capacity_(capacity), on_evict_(std::move(on_evict)) {
        if (capacity_ == 0) {
            throw std::invalid_argument("LruCache: capacity must be at least 1");
        }
        index_.reserve(capacity_);
    }

    // The value of key, which becomes the most recently used entry, or nullptr when the key
    // is not cached. Counts a hit or a miss. The pointer is valid until the entry leaves.
    V* get(const K& key) {
        const auto found = index_.find(key);
        if (found == index_.end()) {
            ++misses_;
            return nullptr;
        }
        ++hits_;
        // splice relinks the node: no copy, and iterators to it stay valid.
        entries_.splice(entries_.begin(), entries_, found->second);
        return &found->second->second;
    }

    // Looks at a value without making it recent and without counting a hit or a miss.
    const V* peek(const K& key) const {
        const auto found = index_.find(key);
        return found == index_.end() ? nullptr : &found->second->second;
    }

    // Inserts or replaces the value of key; it becomes the most recently used entry. When a
    // new key does not fit, the least recently used entry is evicted first (and on_evict is
    // called with it). Replacing a value evicts nothing.
    void put(K key, V value) {
        if (const auto found = index_.find(key); found != index_.end()) {
            found->second->second = std::move(value);
            entries_.splice(entries_.begin(), entries_, found->second);
            return;
        }
        if (index_.size() == capacity_) {
            auto& [old_key, old_value] = entries_.back();
            if (on_evict_) {
                on_evict_(old_key, old_value);
            }
            index_.erase(old_key);
            entries_.pop_back();
        }
        entries_.emplace_front(key, std::move(value));
        index_.emplace(std::move(key), entries_.begin());
    }

    // Removes key (without calling on_evict). False when it was not cached.
    bool erase(const K& key) {
        const auto found = index_.find(key);
        if (found == index_.end()) {
            return false;
        }
        entries_.erase(found->second);
        index_.erase(found);
        return true;
    }

    // The cached keys, most recently used first.
    std::vector<K> keys() const {
        std::vector<K> result;
        result.reserve(entries_.size());
        for (const auto& [key, value] : entries_) {
            result.push_back(key);
        }
        return result;
    }

    std::size_t size() const { return index_.size(); }
    std::size_t capacity() const { return capacity_; }
    std::size_t hits() const { return hits_; }
    std::size_t misses() const { return misses_; }

private:
    using Entries = std::list<std::pair<K, V>>;

    std::size_t capacity_;
    OnEvict on_evict_;
    Entries entries_;  // most recently used first
    std::unordered_map<K, typename Entries::iterator, Hash> index_;
    std::size_t hits_ = 0;
    std::size_t misses_ = 0;
};

}  // namespace dojo
