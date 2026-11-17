Evaluate a tree. Add to `expr.hpp`:

```cpp
// More kinds at the end of ErrorKind:
UnknownVariable,
UnknownFunction,
WrongArity,      // a function called with the wrong number of arguments
DivisionByZero,  // x / 0
NotFinite,       // a result that is infinite or not a number

using Function = std::function<double(std::span<const double>)>;

struct Environment {
    std::map<std::string, double, std::less<>> variables;
    std::map<std::string, Function, std::less<>> functions;  // these hide the builtins
};

std::expected<double, Error> evaluate(const Expr& expr, const Environment& env = {});
```

- Builtins: `sqrt(x)`, `abs(x)`, `pow(x, y)`, and `min` and `max` of one or more arguments.
  A function of the environment takes any number of arguments, and hides a builtin of the same
  name.
- `^` is `std::pow`. `std::less<>` lets `find` take a `std::string_view` without building a
  string.

Errors and where they are:

| Kind | Position |
| --- | --- |
| `UnknownVariable` | the start of the name |
| `UnknownFunction`, `WrongArity` | the start of the function's name |
| `DivisionByZero` | the `/` (whenever the right side is 0, even `0 / 0`) |
| `NotFinite` | the node whose result is infinite or NaN: the operator (`10 ^ 400` → the `^`) or the function's name (`sqrt(-1)`) |

The first error wins, in this order: a call checks its function's name, then its number of
arguments, then evaluates the arguments from left to right; a binary operation evaluates its
left side, then its right side, then itself. A parse error stays what it was.

One visitor with an `operator()` per kind of node, and `std::visit` to pick it, is the whole
evaluator; `std::expected::transform` and `and_then` chain the steps that may fail.
