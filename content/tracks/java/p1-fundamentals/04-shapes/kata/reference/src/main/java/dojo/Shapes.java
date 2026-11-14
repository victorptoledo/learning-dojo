package dojo;

import java.util.List;

/** Operations over any shape. */
public final class Shapes {
    private Shapes() {}

    public static double area(Shape shape) {
        return switch (shape) {
            case Circle(double r) -> Math.PI * r * r;
            case Rectangle(double w, double h) -> w * h;
            case Triangle(double a, double b, double c) -> {
                double s = (a + b + c) / 2;
                yield Math.sqrt(s * (s - a) * (s - b) * (s - c));
            }
        };
    }

    public static double perimeter(Shape shape) {
        return switch (shape) {
            case Circle(double r) -> 2 * Math.PI * r;
            case Rectangle(double w, double h) -> 2 * (w + h);
            case Triangle(double a, double b, double c) -> a + b + c;
        };
    }

    public static String describe(Shape shape) {
        return switch (shape) {
            case Circle(double r) when r == 0 -> "point";
            case Circle c -> "circle";
            case Rectangle(double w, double h) when w == h -> "square";
            case Rectangle r -> "rectangle";
            case Triangle(double a, double b, double c) when a == b && b == c -> "equilateral triangle";
            case Triangle(double a, double b, double c) when a == b || b == c || a == c -> "isosceles triangle";
            case Triangle t -> "scalene triangle";
        };
    }

    public static double totalArea(List<Shape> shapes) {
        return shapes.stream().mapToDouble(Shapes::area).sum();
    }
}
