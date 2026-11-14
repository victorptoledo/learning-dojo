package dojo;

public record Circle(double radius) implements Shape {
    public Circle {
        throw new UnsupportedOperationException("todo validate the radius " + radius);
    }
}
