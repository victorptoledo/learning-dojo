---
from: python
status: draft
concept: "Files, names and links"
from_way: "shutil.copy, shutil.move, os.remove and pathlib, with os.link and os.symlink rarely used."
native_way: "cp, mv and rm in the shell, symbolic links everywhere for releases and configuration, and inodes behind every name."
sensei_tip: "os.replace(tmp, target) is the atomic rename that mv does on the same filesystem; write a temporary file next to the target and replace it, so readers never see half a file."
---

| Python | Linux |
| --- | --- |
| shutil.copytree(a, b) | cp -r a b |
| os.replace(tmp, path) | mv tmp path |
| os.remove(p) | rm p |
| os.symlink(target, link) | ln -s target link |
| Path('.').rglob('*.log') | find . -name '*.log' |
| os.stat(p).st_ino | ls -i, stat |
