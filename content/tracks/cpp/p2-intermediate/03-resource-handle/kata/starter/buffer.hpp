#pragma once

#include <cstddef>
#include <memory>

namespace dojo {

// A fixed-size array of ints on the heap that owns its memory by hand: new[] in the
// constructor, delete[] in the destructor, and the rule of five for copies and moves.
// (In real code std::vector<int> does all of this. Here you write it once to see how.)
class Buffer {
public:
    Buffer() = default;                          // empty: size 0, no memory
    explicit Buffer(std::size_t size);           // size ints, all 0
    Buffer(const Buffer& other);                 // a deep copy
    Buffer& operator=(const Buffer& other);      // a deep copy; safe for self-assignment
    Buffer(Buffer&& other) noexcept;             // steals the memory; other becomes empty
    Buffer& operator=(Buffer&& other) noexcept;  // frees its own memory, then steals
    ~Buffer();

    std::size_t size() const { return size_; }
    bool empty() const { return size_ == 0; }
    int* data() { return data_; }
    const int* data() const { return data_; }

    int& operator[](std::size_t index) { return data_[index]; }
    const int& operator[](std::size_t index) const { return data_[index]; }

    // The value at index, or std::out_of_range when index >= size().
    int at(std::size_t index) const;

    // Exchanges the contents of two buffers without allocating.
    void swap(Buffer& other) noexcept;

private:
    int* data_ = nullptr;
    std::size_t size_ = 0;
};

// A singly linked list of ints where each node owns the next one with std::unique_ptr.
// No new or delete in this class: the smart pointers free the nodes.
class IntList {
public:
    IntList() = default;
    IntList(const IntList&) = delete;
    IntList& operator=(const IntList&) = delete;
    IntList(IntList&&) noexcept = default;
    IntList& operator=(IntList&&) noexcept = default;
    // Frees the nodes one by one in a loop. The default destructor would free them
    // recursively, one stack frame per node, and overflow the stack on a long list.
    ~IntList();

    void push_front(int value);
    // Removes the first value and returns it; std::out_of_range when the list is empty.
    int pop_front();
    // Reverses the list in place by relinking the nodes (no new nodes).
    void reverse();

    std::size_t size() const { return size_; }
    bool empty() const { return head_ == nullptr; }
    // The value of the first node; std::out_of_range when the list is empty.
    int front() const;

private:
    struct Node {
        int value = 0;
        std::unique_ptr<Node> next;
    };
    std::unique_ptr<Node> head_;
    std::size_t size_ = 0;
};

}  // namespace dojo
