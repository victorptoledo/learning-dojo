Change a config, write it back as text and lay one config over another. Add to `config.hpp`:

```cpp
// Members of Config:
bool set(std::string section, std::string key, std::string value);
std::string to_string() const;

// Free function in namespace dojo:
Config merged(const Config& base, const Config& overlay);
```

- `set` replaces the value of an existing key (which keeps its place) or adds the key at the
  end of its section, adding the section at the end if it is new (`""` is the global
  section). It returns `false` and changes nothing when the result would not read back the
  same: a bad section name, a bad key, or a value with a line break or with spaces or tabs at
  either end.
- `to_string` writes the global keys first, then each section as `[name]` followed by its
  keys, one `key = value` per line, every line ending with `\n` and one empty line between
  blocks. Comments are not kept; empty sections are. An empty config is `""`:

```ini
a = 1

[server]
host = x
port = 80

[empty]
```

- For every config, `Config::parse(c.to_string())` gives a config equal to `c`.
- `merged(base, overlay)` is `base` with every key of `overlay` set on it, in `overlay`'s
  order: a key both have takes the overlay's value, new keys and new sections (even empty
  ones) are added at the end. Neither argument changes.

`set` takes its strings by value: a caller that passes a temporary moves it in, and `set`
moves it on into the config, so a long value is never copied twice.
