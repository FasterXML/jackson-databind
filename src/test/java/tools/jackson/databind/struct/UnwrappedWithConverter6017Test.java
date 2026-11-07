package tools.jackson.databind.struct;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

import tools.jackson.databind.DatabindException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.util.StdConverter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// [databind#6017]: `@JsonUnwrapped` should be compatible with `@JsonSerialize(converter = )`
public class UnwrappedWithConverter6017Test extends DatabindTestUtil
{
    static class Point {
        public int x, y;
        public Point(int x, int y) { this.x = x; this.y = y; }
    }

    static class PointConverter extends StdConverter<String, Point> {
        @Override
        public Point convert(String value) {
            String[] parts = value.split(",");
            return new Point(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        }
    }

    static class Outer {
        public String name = "test";

        @JsonUnwrapped
        @JsonSerialize(converter = PointConverter.class)
        public String coords = "2,3";
    }

    static class OuterPrefixed {
        public String name = "test";

        @JsonUnwrapped(prefix = "p_")
        @JsonSerialize(converter = PointConverter.class)
        public String coords = "2,3";
    }

    // [databind#6017]: converter-produced unwrapped property ("x") collides with
    // a regular outer property; conflict detection (#2883) should see through the
    // converting serializer and report it.
    static class OuterConflict {
        public int x = 1;

        @JsonUnwrapped
        @JsonSerialize(converter = PointConverter.class)
        public String coords = "2,3";
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testUnwrappedWithConverter() throws Exception {
        assertEquals("""
                {"x":2,"y":3,"name":"test"}""",
                MAPPER.writeValueAsString(new Outer()));
    }

    @Test
    public void testUnwrappedPrefixedWithConverter() throws Exception {
        assertEquals("""
                {"p_x":2,"p_y":3,"name":"test"}""",
                MAPPER.writeValueAsString(new OuterPrefixed()));
    }

    @Test
    public void testUnwrappedConverterPropertyConflictDetected() {
        DatabindException e = assertThrows(DatabindException.class,
                () -> MAPPER.writeValueAsString(new OuterConflict()));
        assertTrue(e.getMessage().contains("unwrapped property 'x'"),
                "Unexpected message: " + e.getMessage());
    }
}
