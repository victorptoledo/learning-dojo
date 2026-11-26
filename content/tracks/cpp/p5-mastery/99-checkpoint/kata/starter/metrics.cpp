#include "metrics.hpp"

#include "metrics_c.hpp"

namespace dojo {

Histogram::Histogram(std::span<const double> bounds) {
    (void)bounds;
    throw std::logic_error("todo: write the Histogram constructor");
}

void Histogram::record(double value) noexcept {
    (void)value;
    // todo: count the value; this must not allocate or throw
}

double Histogram::quantile(double q) const {
    (void)q;
    throw std::logic_error("todo: write Histogram::quantile");
}

Histogram& Registry::define(std::string_view name, std::span<const double> bounds) {
    (void)name;
    (void)bounds;
    throw std::logic_error("todo: write Registry::define");
}

Histogram* Registry::find(std::string_view name) noexcept {
    (void)name;
    return nullptr;  // todo: write Registry::find
}

const Histogram* Registry::find(std::string_view name) const noexcept {
    (void)name;
    return nullptr;  // todo: write Registry::find
}

std::string Registry::render() const {
    throw std::logic_error("todo: write Registry::render");
}

}  // namespace dojo

// The handle C code holds. Only this file knows what is inside.
struct dojo_registry {
    dojo::Registry registry;
};

extern "C" {

dojo_registry* dojo_registry_new(void) {
    return nullptr;  // todo: write dojo_registry_new
}

void dojo_registry_free(dojo_registry* registry) {
    (void)registry;  // todo: write dojo_registry_free
}

int dojo_define(dojo_registry* registry, const char* name, const double* bounds, size_t count) {
    (void)registry;
    (void)name;
    (void)bounds;
    (void)count;
    return DOJO_INVALID_ARGUMENT;  // todo: write dojo_define
}

int dojo_record(dojo_registry* registry, const char* name, double value) {
    (void)registry;
    (void)name;
    (void)value;
    return DOJO_INVALID_ARGUMENT;  // todo: write dojo_record
}

size_t dojo_render(const dojo_registry* registry, char* buffer, size_t capacity) {
    (void)registry;
    (void)buffer;
    (void)capacity;
    return 0;  // todo: write dojo_render
}

}  // extern "C"
