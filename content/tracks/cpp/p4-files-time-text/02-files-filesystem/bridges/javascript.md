---
from: javascript
status: draft
concept: "Files and the file system"
from_way: "Node's fs and fs/promises, path.join, readdir, and writeFile to a temporary name plus rename for atomic writes."
native_way: "Synchronous std::filesystem calls and file streams, std::filesystem::path with /, and fs::rename for the same atomic pattern."
sensei_tip: "The temp-then-rename trick you may know from write-file-atomic is exactly the same in C++, with the same same-directory rule."
---

| JavaScript | C++ |
| --- | --- |
| `path.join(dir, name)` | `std::filesystem::path(dir) / name` |
| `fs.readFileSync(p, 'utf8')` | `std::string{std::istreambuf_iterator<char>(in), {}}` |
| `fs.readdirSync(dir)` | `std::filesystem::directory_iterator(dir)` |
| `fs.renameSync(tmp, p)` | `std::filesystem::rename(tmp, p)` |
| `err.code === 'ENOENT'` | `ec == std::errc::no_such_file_or_directory` |
