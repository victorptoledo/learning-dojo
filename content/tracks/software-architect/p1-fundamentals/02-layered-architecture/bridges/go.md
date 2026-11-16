---
from: go
status: draft
concept: "Layered architecture"
from_way: "`handlers`, `services`, `repositories` and `models` packages, with a giant `models` package shared by all of them."
native_way: "Packages per feature or domain concept (`orders`, `payments`) that hold their handlers, logic and storage, unexported by default, with `cmd/` wiring them."
sensei_tip: "The Go community's advice to name packages for what they provide, not what they contain, is package by feature: `orders` tells you something, `services` does not."
---

| Go | Layering idea |
| --- | --- |
| `net/http` handlers | presentation layer |
| plain functions and structs with methods | application and domain |
| `database/sql`, sqlc | persistence layer |
| `handlers`, `services`, `models` packages | package by layer |
| `orders`, `payments` packages | package by feature |
| a handler that only calls the store | sinkhole |
