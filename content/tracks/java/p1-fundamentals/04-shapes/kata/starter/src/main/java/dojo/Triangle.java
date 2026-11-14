package dojo;

public record Triangle(double a, double b, double c) implements Shape {
    public Triangle {
        throw new UnsupportedOperationException("todo validate the sides " + a + ", " + b + ", " + c);
    }
}
