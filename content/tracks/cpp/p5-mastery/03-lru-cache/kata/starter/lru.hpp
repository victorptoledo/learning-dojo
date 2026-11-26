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
        throw std::logic_error("todo: write the LruCache constructor");
    }

    // The value of key, which becomes the most recently used entry, or nullptr when the key
    // is not cached. Counts a hit or a miss. The pointer is valid until the entry leaves.
    V* get(const K& key) {
        (void)key;
        throw std::logic_error("todo: write LruCache::get");
    }

    // Looks at a value without making it recent and without counting a hit or a miss.
    const V* peek(const K& key) const {
        (void)key;
        throw std::logic_error("todo: write LruCache::peek");
    }

    // Inserts or replaces the value of key; it becomes the most recently used entry. When a
    // new key does not fit, the least recently used entry is evicted first (and on_evict is
    // called with it). Replacing a value evicts nothing.
    void put(K key, V value) {
        (void)key;
        (void)value;
        throw std::logic_error("todo: write LruCache::put");
    }

    // Removes key (without calling on_evict). False when it was not cached.
    bool erase(const K& key) {
        (void)key;
        throw std::logic_error("todo: write LruCache::erase");
    }

    // The cached keys, most recently used first.
    std::vector<K> keys() const { throw std::logic_error("todo: write LruCache::keys"); }

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
