Every server that creates links needs codes no other server will hand out, without asking a
central database for each one. The trick is a ticket server with **id blocks**: each
shortener reserves a range of ids in one round trip and spends it locally.

```java
public Shortener(LinkStore store, IdAllocator ids, Clock clock, ShortenerConfig config)
public String shorten(String url)                  // a new generated code
public String shorten(String url, String alias)    // the alias is the code
public Optional<String> resolve(String code)       // the long URL, if the code exists
```

**Generated codes**

- The shortener calls `ids.reserve(config.blockSize())` when it needs an id and has none
  left, never before (not in the constructor) and never more than needed: 250 links with
  blocks of 100 cost 3 reservations. The ids of a block are used in order.
- The code of id `n` is `n` in base 62 with the digits
  `0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ` (so `a` is 10 and `Z` is
  61), left-padded with `0` to `config.codeLength()` characters. A number that needs more
  characters just gets a longer code: id 3844 with a length of 2 is `100`.
- The link is saved with `store.putIfAbsent(code, url)`. When it returns `false` (somebody
  took that code as an alias), the id is skipped and the next one is tried.
- Shortening the same URL twice gives two codes. Codes are case-sensitive.

**Validation**

- A URL is valid when `java.net.URI` parses it, its scheme is `http` or `https` (any case),
  it has a host, and it has at most 2048 characters. Anything else, `null` included, throws
  `IllegalArgumentException` and costs no id.
- An alias has 3 to 32 characters among ASCII letters, digits, `-` and `_`; otherwise
  `IllegalArgumentException`. An alias that is already a code throws `IllegalStateException`
  and the existing link is untouched.

`resolve` of a code nobody created is `Optional.empty()`.
