package dojo;

public record Triangle(double a, double b, double c) implements Shape {
    public Triangle {
        if (a <= 0 || b <= 0 || c <= 0 || a >= b + c || b >= a + c || c >= a + b) {
            throw new IllegalArgumentException("not a triangle: " + a + ", " + b + ", " + c);
        }
    }
}
