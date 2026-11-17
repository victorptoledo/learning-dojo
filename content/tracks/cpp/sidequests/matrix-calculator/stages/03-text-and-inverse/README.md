Matrices as text, and the two classic questions about a square matrix. Add to `matrix.hpp`:

```cpp
std::optional<Matrix> parse(std::string_view text);
std::string to_string(const Matrix& m);
std::optional<double> determinant(const Matrix& m);  // square only; 1 for the empty matrix
std::optional<Matrix> inverse(const Matrix& m);      // square and not singular only
```

**Text.** A matrix is written `[1 2; 3 4]`: square brackets, rows separated by `;`, numbers
separated by spaces.

- `parse` allows any amount of whitespace (spaces, tabs, new lines) around the brackets, the
  `;` and the numbers. `[]` is the empty matrix. Numbers are what `std::from_chars` reads for
  a `double` (`-1.5`, `2e3`), and must be finite. Anything else is `std::nullopt`: missing
  brackets, an empty row (`[1 2;]`), rows of different lengths, text after `]`, a comma, a
  word, `inf` or `nan`.
- `to_string` writes `[1 2; 3 4]` with one space between numbers and `"; "` between rows, and
  `[]` for the empty matrix. Each number is written in its shortest exact form, which is what
  `std::format("{}", x)` gives (`2000`, `0.25`, `0.1`), so `parse(to_string(m)) == m` for every
  matrix.

**Determinant and inverse.** Use Gaussian elimination on a copy of the numbers:

1. For each column, pick the row (at or below the diagonal) with the largest absolute value
   in that column as the pivot, and swap it into place. Each swap flips the determinant's
   sign. A pivot whose absolute value is below `1e-12` means the matrix is singular.
2. Subtract multiples of the pivot row from the rows below it, so the column becomes zero
   under the diagonal. The determinant is the product of the pivots (with the sign).
3. For the inverse, do the same row operations on an identity matrix next to it, then clear
   the columns above each pivot and divide each row by its pivot: the identity has become the
   inverse (Gauss-Jordan).

`inverse` returns `std::nullopt` for a matrix that is not square or is singular (such as
`[1 2; 2 4]`, whose second row is twice the first). The tests compare results within `1e-9`.
