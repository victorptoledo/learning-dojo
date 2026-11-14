package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ShapesTest {
    @Test
    void area_of_a_rectangle() {
        assertEquals(6.0, Shapes.area(new Rectangle(2, 3)), 1e-9);
    }
}
