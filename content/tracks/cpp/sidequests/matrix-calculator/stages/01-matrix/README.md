A `Matrix` value whose shape can never be wrong. `matrix.hpp` already declares the interface;
add the data members and write the members in `matrix.cpp` (or inline in the header).

| Member | Does |
| --- | --- |
| `Matrix()` | The empty 0 x 0 matrix. |
| `Matrix(rows, cols, fill = 0.0)` | A `rows` x `cols` matrix full of `fill`. Zero rows **or** zero columns give the empty 0 x 0 matrix, so `Matrix(0, 5) == Matrix()`. |
| `Matrix::from_rows(rows)` | The matrix with these rows, row 0 first: `from_rows({{1, 2, 3}, {4, 5, 6}})` is 2 x 3. No rows give the empty matrix; `std::nullopt` when a row is empty or the rows have different lengths. |
| `Matrix::identity(n)` | n x n, ones on the diagonal and zeros elsewhere; `identity(0)` is empty. |
| `rows()`, `cols()`, `empty()` | The shape. |
| `at(row, col) const` | The number there (counting from 0), or `std::nullopt` outside the matrix. |
| `set(row, col, value)` | Changes that number and returns `true`; `false`, with nothing changed, outside the matrix. |
| `==` | Same shape and same numbers (`= default` does it once the members are right). |

The invariant lives in the members: a matrix always holds exactly `rows() * cols()` numbers.
One `std::vector<double>` stored row by row (the number at `(r, c)` is at `r * cols + c`) is
enough, and it makes copies, moves and destruction correct without writing any of them: the
rule of zero. A copy is a separate value, so changing it never changes the original.
