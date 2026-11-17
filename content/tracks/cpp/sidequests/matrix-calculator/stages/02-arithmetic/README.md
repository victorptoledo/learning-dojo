The arithmetic of matrices. Add these declarations to `matrix.hpp` and write them; none of
them changes its arguments, and an operation the shapes do not allow returns `std::nullopt`.

```cpp
// Members of Matrix:
Matrix transposed() const;    // cols x rows: (c, r) of the result is (r, c) of this matrix
Matrix& operator*=(double k); // every number times k; returns *this

// Free functions in namespace dojo:
std::optional<Matrix> add(const Matrix& a, const Matrix& b);       // same shape only
std::optional<Matrix> subtract(const Matrix& a, const Matrix& b);  // same shape only
std::optional<Matrix> multiply(const Matrix& a, const Matrix& b);  // a.cols() == b.rows()
Matrix operator*(const Matrix& m, double k);
Matrix operator*(double k, const Matrix& m);
Matrix operator-(const Matrix& m);
std::optional<double> trace(const Matrix& m);                       // square only
std::optional<Matrix> power(const Matrix& m, unsigned exponent);    // square only
```

- `multiply` is the matrix product: the result is `a.rows()` x `b.cols()`, and its `(r, c)` is
  the sum of `a(r, k) * b(k, c)` over every `k`. A 2 x 3 times a 3 x 2 is 2 x 2.
- The empty matrix counts as square (0 x 0): its trace is 0, any power of it is empty, and
  adding two empty matrices gives the empty matrix.
- `power(m, 0)` is the identity of m's size. `power(m, 40)` should not need 40 products:
  squaring (`m^8 = ((m^2)^2)^2`) needs about log2(40).

Write the scalar `*` on top of `*=` (copy, then scale the copy) so the logic lives in one
place. Taking an argument by `const Matrix&` and returning a new `Matrix` by value is cheap:
the result is moved or built in place, never copied.
