---
from: go
status: draft
concept: "Closed sets of alternatives"
from_way: "Interfaces with a private marker method and type switches."
native_way: "Sealed interfaces and exhaustive switch patterns."
sensei_tip: "Go's trick of an unexported method to close an interface is what sealed does officially, and Java adds the exhaustiveness check that Go's type switch lacks."
---

| Go | Java 25 |
| --- | --- |
| interface with unexported isShape() | sealed interface Shape permits ... |
| switch v := s.(type) | switch (s) { case Circle c -> ... } |
| default: panic("unknown") | no default needed |
