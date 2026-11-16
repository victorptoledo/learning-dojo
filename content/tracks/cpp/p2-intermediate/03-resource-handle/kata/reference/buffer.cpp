#include "buffer.hpp"

#include <algorithm>
#include <stdexcept>
#include <utility>

namespace dojo {

Buffer::Buffer(std::size_t size) : data_(size == 0 ? nullptr : new int[size]{}), size_(size) {}

Buffer::Buffer(const Buffer& other) : Buffer(other.size_) {
    std::copy_n(other.data_, other.size_, data_);
}

// Copy and swap: the copy is made first, so a failed allocation leaves *this untouched,
// and self-assignment works without a special case.
Buffer& Buffer::operator=(const Buffer& other) {
    Buffer copy(other);
    swap(copy);
    return *this;
}

Buffer::Buffer(Buffer&& other) noexcept
    : data_(std::exchange(other.data_, nullptr)), size_(std::exchange(other.size_, 0)) {}

Buffer& Buffer::operator=(Buffer&& other) noexcept {
    if (this != &other) {
        delete[] data_;
        data_ = std::exchange(other.data_, nullptr);
        size_ = std::exchange(other.size_, 0);
    }
    return *this;
}

Buffer::~Buffer() {
    delete[] data_;
}

int Buffer::at(std::size_t index) const {
    if (index >= size_) {
        throw std::out_of_range("Buffer::at: index out of range");
    }
    return data_[index];
}

void Buffer::swap(Buffer& other) noexcept {
    std::swap(data_, other.data_);
    std::swap(size_, other.size_);
}

IntList::~IntList() {
    // Take each node out of the chain before it dies, so no destructor recurses.
    while (head_) {
        head_ = std::move(head_->next);
    }
}

void IntList::push_front(int value) {
    auto node = std::make_unique<Node>();
    node->value = value;
    node->next = std::move(head_);
    head_ = std::move(node);
    ++size_;
}

int IntList::pop_front() {
    if (!head_) {
        throw std::out_of_range("IntList::pop_front: empty list");
    }
    const int value = head_->value;
    head_ = std::move(head_->next);
    --size_;
    return value;
}

void IntList::reverse() {
    std::unique_ptr<Node> reversed;
    while (head_) {
        auto rest = std::move(head_->next);
        head_->next = std::move(reversed);
        reversed = std::move(head_);
        head_ = std::move(rest);
    }
    head_ = std::move(reversed);
}

int IntList::front() const {
    if (!head_) {
        throw std::out_of_range("IntList::front: empty list");
    }
    return head_->value;
}

}  // namespace dojo
