package dojo;

public record Rectangle(double width, double height) implements Shape {
    public Rectangle {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("negative side: " + width + " x " + height);
        }
    }
}
