---
from: javascript
status: draft
concept: "Content-addressed, immutable objects"
from_way: "Mutating objects in place, or immutable state in Redux where every change creates a new object."
native_way: "Git works like immutable state: a commit is never edited, a new commit is created and a name (the branch) is moved to it."
sensei_tip: "The lock file's integrity field (sha512-...) is the same idea: the hash of the content identifies and verifies it."
---

| JavaScript | Git |
| --- | --- |
| `Object.freeze(commit)` | every object is immutable |
| `const next = { ...state, change }` | a new commit with the old one as parent |
| `let main = next` | moving the branch ref |
| `"integrity": "sha512-..."` in package-lock.json | the object hash |
