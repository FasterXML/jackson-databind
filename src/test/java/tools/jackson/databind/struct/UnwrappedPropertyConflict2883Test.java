package tools.jackson.databind.struct;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.exc.InvalidDefinitionException;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for [databind#2883]: detection of property name collisions
 * between unwrapped properties and regular bean properties.
 */
public class UnwrappedPropertyConflict2883Test extends DatabindTestUtil
{
    static class InnerB {
        public int ba = 3;
    }

    static class InnerD {
        public int da = 4;
    }

    static class OuterConflict {
        public InnerB b = new InnerB();
        @JsonUnwrapped
        public InnerC c = new InnerC();
    }

    static class InnerC {
        public InnerD b = new InnerD();
    }

    static class OuterNoConflict {
        public InnerB b = new InnerB();
        @JsonUnwrapped(prefix = "c_")
        public InnerC c = new InnerC();
    }

    static class OuterNoConflict2 {
        public String name = "test";
        @JsonUnwrapped
        public Location location = new Location(1, 2);
    }

    static class Location {
        public int x;
        public int y;

        public Location() { }
        public Location(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    // Two unwrapped types that both produce a property named "id"
    static class TwoUnwrappedConflict {
        @JsonUnwrapped
        public HasId1 first = new HasId1();
        @JsonUnwrapped
        public HasId2 second = new HasId2();
    }

    static class HasId1 {
        public int id = 1;
    }

    static class HasId2 {
        public int id = 2;
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testUnwrappedConflictDetected() throws Exception {
        InvalidDefinitionException ex = assertThrows(
                InvalidDefinitionException.class,
                () -> MAPPER.writeValueAsString(new OuterConflict()));
        verifyException(ex, "Conflict between unwrapped property");
        verifyException(ex, "'b'");
    }

    @Test
    public void testUnwrappedNoConflictWithPrefix() throws Exception {
        String json = MAPPER.writeValueAsString(new OuterNoConflict());
        assertNotNull(json);
    }

    @Test
    public void testTwoUnwrappedConflictDetected() throws Exception {
        InvalidDefinitionException ex = assertThrows(
                InvalidDefinitionException.class,
                () -> MAPPER.writeValueAsString(new TwoUnwrappedConflict()));
        verifyException(ex, "Conflict between unwrapped property");
        verifyException(ex, "'id'");
    }

    @Test
    public void testUnwrappedNoConflictDifferentNames() throws Exception {
        String json = MAPPER.writeValueAsString(new OuterNoConflict2());
        assertTrue(json.contains("\"name\":\"test\""));
        assertTrue(json.contains("\"x\":1"));
        assertTrue(json.contains("\"y\":2"));
    }
}
