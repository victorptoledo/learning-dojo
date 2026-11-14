package dojo;

public record Rectangle(double width, double height) implements Shape {
    public Rectangle {
        throw new UnsupportedOperationException("todo validate " + width + " x " + height);
    }
}
