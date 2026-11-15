---
from: javascript
status: draft
concept: "Files, names and links"
from_way: "fs.copyFile, fs.rename, fs.rm and fs.symlink in Node, usually through a helper library."
native_way: "cp, mv, rm and ln -s in the shell, and find for walking trees."
sensei_tip: "fs.rename is atomic only on the same filesystem, exactly like mv; across mounts it fails with EXDEV and libraries fall back to copy plus delete, which is not atomic."
---

| JavaScript / TypeScript | Linux |
| --- | --- |
| fs.cpSync(a, b, {recursive: true}) | cp -r a b |
| fs.renameSync(a, b) | mv a b |
| fs.rmSync(p, {recursive: true}) | rm -r p |
| fs.symlinkSync(target, link) | ln -s target link |
| fast-glob '**/*.log' | find . -name '*.log' |
