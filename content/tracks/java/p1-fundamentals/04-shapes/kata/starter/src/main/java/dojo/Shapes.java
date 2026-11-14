package dojo;

import java.util.List;

/** Operations over any shape. */
public final class Shapes {
    private Shapes() {}

    public static double area(Shape shape) {
        throw new UnsupportedOperationException("todo area of " + shape);
    }

    public static double perimeter(Shape shape) {
        throw new UnsupportedOperationException("todo perimeter of " + shape);
    }

    public static String describe(Shape shape) {
        throw new UnsupportedOperationException("todo describe " + shape);
    }

    public static double totalArea(List<Shape> shapes) {
        throw new UnsupportedOperationException("todo total area of " + shapes.size() + " shapes");
    }
}
