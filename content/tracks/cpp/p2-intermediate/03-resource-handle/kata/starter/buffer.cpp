#include "buffer.hpp"

#include <stdexcept>

namespace dojo {

Buffer::Buffer(std::size_t size) {
    (void)size;
    throw std::logic_error("todo: write Buffer(size)");
}

Buffer::Buffer(const Buffer& other) {
    (void)other;
    throw std::logic_error("todo: write the copy constructor");
}

Buffer& Buffer::operator=(const Buffer& other) {
    (void)other;
    throw std::logic_error("todo: write the copy assignment");
}

// A move must not throw: write it before you run the tests that move buffers.
Buffer::Buffer(Buffer&& other) noexcept {
    (void)other;
}

Buffer& Buffer::operator=(Buffer&& other) noexcept {
    (void)other;
    return *this;
}

Buffer::~Buffer() {
    // todo: free the memory (a destructor must not throw, so this one is empty for now)
}

int Buffer::at(std::size_t index) const {
    (void)index;
    throw std::logic_error("todo: write Buffer::at");
}

void Buffer::swap(Buffer& other) noexcept {
    (void)other;
}

IntList::~IntList() {
    // todo: free the nodes in a loop
}

void IntList::push_front(int value) {
    (void)value;
    throw std::logic_error("todo: write IntList::push_front");
}

int IntList::pop_front() {
    throw std::logic_error("todo: write IntList::pop_front");
}

void IntList::reverse() {
    throw std::logic_error("todo: write IntList::reverse");
}

int IntList::front() const {
    throw std::logic_error("todo: write IntList::front");
}

}  // namespace dojo
