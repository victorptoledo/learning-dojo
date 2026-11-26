---
from: python
status: draft
concept: "C extensions"
from_way: "`ctypes`, `cffi` or C extension modules; memory bugs crash the interpreter."
native_way: "`unsafe` marks the few lines where memory rules are your job; everything else stays checked."
sensei_tip: "With PyO3, Rust is a common way to write Python extensions: the `unsafe` FFI lives inside the library, and your code stays safe."
---

| Python | Rust |
| --- | --- |
| `ctypes.CDLL(...).strlen` | `unsafe extern "C" { fn strlen(...); }` |
| `ctypes.c_char_p` | `*const c_char`, `CStr`, `CString` |
| segfault in a C extension | undefined behaviour, caught early by Miri |
| PyO3 extension | `#[pyfunction]` over safe Rust |
