---
from: java
status: draft
concept: "Content-addressed storage"
from_way: "Old centralised systems that stored incremental deltas."
native_way: "Git is a content-addressed key-value store (`.git/objects`). Branches are just lightweight pointers to a commit hash."
sensei_tip: "Creating a branch is nearly free: Git only writes a hash into a small file."
---
