package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class ShapesGradingTest {
    @Test
    void area_of_each_shape() {
        assertEquals(Math.PI * 4, Shapes.area(new Circle(2)), 1e-9);
        assertEquals(12.0, Shapes.area(new Rectangle(3, 4)), 1e-9);
        assertEquals(6.0, Shapes.area(new Triangle(3, 4, 5)), 1e-9);
    }

    @Test
    void perimeter_of_each_shape() {
        assertEquals(4 * Math.PI, Shapes.perimeter(new Circle(2)), 1e-9);
        assertEquals(14.0, Shapes.perimeter(new Rectangle(3, 4)), 1e-9);
        assertEquals(12.0, Shapes.perimeter(new Triangle(3, 4, 5)), 1e-9);
    }

    @Test
    void describes_circles_and_points() {
        assertEquals("point", Shapes.describe(new Circle(0)));
        assertEquals("circle", Shapes.describe(new Circle(1.5)));
    }

    @Test
    void describes_squares_and_rectangles() {
        assertEquals("square", Shapes.describe(new Rectangle(2, 2)));
        assertEquals("rectangle", Shapes.describe(new Rectangle(2, 5)));
    }

    @Test
    void describes_the_three_kinds_of_triangle() {
        assertEquals("equilateral triangle", Shapes.describe(new Triangle(2, 2, 2)));
        assertEquals("isosceles triangle", Shapes.describe(new Triangle(2, 2, 3)));
        assertEquals("isosceles triangle", Shapes.describe(new Triangle(3, 2, 2)));
        assertEquals("scalene triangle", Shapes.describe(new Triangle(3, 4, 5)));
    }

    @Test
    void total_area_adds_every_shape_and_is_zero_for_none() {
        List<Shape> shapes = List.of(new Rectangle(1, 2), new Triangle(3, 4, 5), new Circle(0));
        assertEquals(8.0, Shapes.totalArea(shapes), 1e-9);
        assertEquals(0.0, Shapes.totalArea(List.of()), 1e-9);
    }

    @Test
    void rejects_a_negative_radius() {
        assertThrows(IllegalArgumentException.class, () -> new Circle(-1));
    }

    @Test
    void rejects_a_negative_side() {
        assertThrows(IllegalArgumentException.class, () -> new Rectangle(-1, 2));
        assertThrows(IllegalArgumentException.class, () -> new Rectangle(2, -0.5));
    }

    @Test
    void rejects_impossible_triangles() {
        assertThrows(IllegalArgumentException.class, () -> new Triangle(1, 2, 3));
        assertThrows(IllegalArgumentException.class, () -> new Triangle(0, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new Triangle(10, 1, 1));
    }

    @Test
    void records_compare_by_value() {
        assertEquals(new Rectangle(2, 3), new Rectangle(2, 3));
        assertEquals("Circle[radius=1.0]", new Circle(1).toString());
    }
}
