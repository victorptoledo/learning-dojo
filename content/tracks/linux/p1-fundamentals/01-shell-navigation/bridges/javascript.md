---
from: javascript
status: draft
concept: "Paths, the program search path and documentation"
from_way: "process.cwd(), the path module and __dirname in Node, and npx resolving binaries from node_modules/.bin."
native_way: "One tree from /, case-sensitive names, and commands resolved through PATH in order."
sensei_tip: "An import of './Config.js' that works on Windows or macOS (case-insensitive by default) fails on Linux when the file is config.js. Run CI on Linux to catch it."
---

| JavaScript / TypeScript | Linux |
| --- | --- |
| process.cwd() | pwd |
| path.join(__dirname, 'data') | a path relative to the script's directory |
| node_modules/.bin on the PATH of npm scripts | PATH, searched left to right |
| fs.readdirSync('.') | ls |
| MDN and the Node docs | man pages and --help |
