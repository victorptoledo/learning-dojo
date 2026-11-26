#pragma once

// The C interface of the metrics library, for Python (ctypes, cffi), Go (cgo), Rust and C.
// It is plain C: a C project would call this file metrics.h. No C++ exception ever crosses it:
// every function returns a status code instead.

#include <stddef.h>  // not <cstddef>: this header is also C

#ifdef __cplusplus
extern "C" {
#endif

// An opaque handle: C code only ever holds a pointer to it.
typedef struct dojo_registry dojo_registry;  // typedef, not using: C has no using

enum {
    DOJO_OK = 0,
    DOJO_UNKNOWN_METRIC = 1,    // no histogram with that name
    DOJO_INVALID_ARGUMENT = 2,  // a null pointer, a bad name or bounds, NaN, a name defined twice
    DOJO_OUT_OF_MEMORY = 3,     // an allocation failed
};

// A new, empty registry, or NULL when memory runs out. Free it with dojo_registry_free.
dojo_registry* dojo_registry_new(void);

// Frees the registry. NULL is fine.
void dojo_registry_free(dojo_registry* registry);

// Defines a histogram with `count` bounds.
int dojo_define(dojo_registry* registry, const char* name, const double* bounds, size_t count);

// Records a value. Never allocates.
int dojo_record(dojo_registry* registry, const char* name, double value);

// Writes the registry's text (see Registry::render) like snprintf: at most capacity - 1
// characters and a terminating NUL (nothing when capacity is 0), and returns the length of the
// whole text, so a caller can retry with a buffer of the returned size + 1. A NULL registry or
// a failure writes "" and returns 0.
size_t dojo_render(const dojo_registry* registry, char* buffer, size_t capacity);

#ifdef __cplusplus
}
#endif
