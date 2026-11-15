---
from: javascript
status: draft
concept: "Packages, visibility and tests"
from_way: "ES modules with import and export, and jest or vitest."
native_way: "mod and use, pub instead of export, and cargo test built in."
sensei_tip: "Everything in Rust is private unless marked pub, the opposite of a module where you forget to not export. Integration tests in tests/ see only your public API, like a consumer would."
---

| JavaScript / TypeScript | Rust |
| --- | --- |
| export function f | pub fn f |
| import { f } from './x' | use crate::x::f; |
| index.ts | lib.rs |
| describe/it | #[test] functions |
| expect(a).toBe(b) | assert_eq!(a, b) |
