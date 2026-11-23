A column with a few distinct values (a city, a status, a flag) is a poor fit for an ordered index:
`city = 3` matches a tenth of the table, and reading a tenth of the table is slow. Column stores
answer it with **bitmaps**: for each distinct value, one bit per row, set when the row has that
value. `city = 3 AND status = 1` is then an AND of two bitmaps, one `long` word (64 rows) at a
time, and its count is the sum of `Long.bitCount` over the words: no row is read at all.

Add to `Table`:

| Method | Does |
| --- | --- |
| `void createBitmapIndex(String column)` | A bitmap per distinct value of the column, built from the rows already there and kept up to date on insert and delete. An unknown column, or one that already has a bitmap index, is an `IllegalArgumentException`. A column may have both kinds of index. |
| `long count(List<Predicate> where)` | How many rows `query(where)` would return. |

**No reads when every predicate is indexed.** When every predicate's column has an index (bitmap
or ordered), `query` and `count` read **no row**: they combine the index results as bitmaps (an
`In` or a `Range` on a bitmap column is the OR of the bitmaps of its values; an ordered index
gives its rows, which you set in a bitmap) and AND them, together with a bitmap of the live rows.
Otherwise they read only the rows that all the indexed predicates allow.

**Write the bitmap yourself**, on a `long[]`: row `i` is bit `i & 63` of word `i >>> 6`, setting
it is `words[i >>> 6] |= 1L << i` (Java shifts a `long` by `i mod 64`), and the rows of a bitmap,
in order, come from `Long.numberOfTrailingZeros(word)` and `word &= word - 1`, which clears the
lowest set bit. `java.util.BitSet` does the same, and the tests refuse it here.

The tests count 2,000 times on 500,000 rows, with three bitmap predicates at a time.
