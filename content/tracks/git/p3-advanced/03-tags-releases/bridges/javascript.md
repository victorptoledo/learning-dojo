---
from: javascript
status: draft
concept: "Versions and release tags"
from_way: "npm version minor, which bumps package.json, commits and creates a tag in one go."
native_way: "That is exactly git tag under the hood: npm version creates the commit and the tag v1.5.0; you still push it (git push --follow-tags)."
sensei_tip: "npm version creates an annotated tag, so git push --follow-tags sends it with the commit. Set it once: git config --global push.followTags true."
---

| npm | Git |
| --- | --- |
| `npm version patch` | commit + `git tag -a v1.4.3` |
| `npm publish` | publish the code of that tag |
| `npm deprecate pkg@1.4.0` | leave the tag, release a fix |
| semantic-release | versions and notes from Conventional Commits |
