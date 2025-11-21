package tools.jackson.databind.deser.jdk;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

/**
 * This unit test verifies that the "Native" java type mapper can properly deals with "forward reference resolution".
 */
public class ObjectArrayDeserializerTest extends DatabindTestUtil {

    public static final class Draw {
        private Shape[] ashapes;
        private Point[] points;

        public Shape[] getAShapes() {
            return ashapes;
        }

        public void setAShapes(Shape[] shapes) {
            this.ashapes = shapes;
        }

        public Point[] getPoints() {
            return points;
        }

        public void setPoints(Point[] points) {
            this.points = points;
        }
    }

    public static final class Shape {
        @JsonIdentityReference(alwaysAsId = true)
        private Point[] points;

        public Point[] getPoints() {
            return points;
        }

        public void setPoints(Point[] points) {
            this.points = points;
        }
    }

    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    public static final record Point(int id, int x, int y) {
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testForwardReferenceResolution() throws Exception {
        Draw draw = new Draw();
        Point point_0_0 = new Point(1, 0, 0);
        Point point_0_2 = new Point(2, 0, 2);
        Point point_2_2 = new Point(3, 2, 2);
        Point point_2_0 = new Point(4, 2, 0);
        Point point_1_3 = new Point(5, 1, 3);
        Shape square = new Shape();
        square.setPoints(new Point[] { point_0_0, point_0_2, point_2_2, point_2_0 });
        Shape triangle = new Shape();
        triangle.setPoints(new Point[] { point_0_2, point_1_3, point_2_2 });
        draw.setAShapes(new Shape[] { square, triangle });
        draw.setPoints(new Point[] { point_0_0, point_0_2, point_2_2, point_2_0, point_1_3 });
        final String JSON = MAPPER.writeValueAsString(draw);
        draw = MAPPER.readValue(JSON, Draw.class);
        assertNotNull(draw);
        assertEquals(5, draw.points.length);
        assertEquals(2, draw.ashapes.length);
        assertEquals(4, draw.ashapes[0].points.length);
        assertEquals(3, draw.ashapes[1].points.length);
        assertSame(draw.points[0], draw.ashapes[0].points[0]);
        assertSame(draw.points[1], draw.ashapes[0].points[1]);
        assertSame(draw.points[2], draw.ashapes[0].points[2]);
        assertSame(draw.points[3], draw.ashapes[0].points[3]);
        assertSame(draw.points[1], draw.ashapes[1].points[0]);
        assertSame(draw.points[4], draw.ashapes[1].points[1]);
        assertSame(draw.points[2], draw.ashapes[1].points[2]);
    }

}
