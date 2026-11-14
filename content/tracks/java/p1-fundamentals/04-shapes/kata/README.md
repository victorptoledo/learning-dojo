# Shapes

`Shape` is a sealed interface with three records: `Circle`, `Rectangle` and `Triangle`.
Make the records validate themselves, then implement the methods of `Shapes`.

## 1. Valid by construction (compact constructors)

Each record's compact constructor throws `IllegalArgumentException` for an impossible shape:

| Record | Invalid when |
| --- | --- |
| `Circle(double radius)` | `radius < 0` (a radius of 0 is a point, and it is allowed) |
| `Rectangle(double width, double height)` | a side is `< 0` |
| `Triangle(double a, double b, double c)` | a side is `<= 0`, or one side is `>=` the sum of the other two |

## 2. `Shapes`

- `area(shape)`: `π·r²`, `w·h`, and for a triangle Heron's formula:
  `s = (a + b + c) / 2`, `area = √(s·(s−a)·(s−b)·(s−c))`.
- `perimeter(shape)`: `2·π·r`, `2·(w + h)`, `a + b + c`.
- `describe(shape)`:

  | Shape | Description |
  | --- | --- |
  | circle with radius 0 | `"point"` |
  | any other circle | `"circle"` |
  | rectangle with equal sides | `"square"` |
  | other rectangle | `"rectangle"` |
  | triangle with three equal sides | `"equilateral triangle"` |
  | triangle with exactly two equal sides | `"isosceles triangle"` |
  | other triangle | `"scalene triangle"` |

- `totalArea(shapes)`: the sum of the areas of a list (0 for an empty list).

Use `switch` expressions with record patterns and `when` guards, and **no `default`**: the
compiler should be the one telling you every shape is handled.

> Tip: `Math.PI`, `Math.sqrt`, and `case Rectangle(double w, double h) when w == h -> ...`.
