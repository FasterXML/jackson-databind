package tools.jackson.databind.struct;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.PropertyNamingStrategies;
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

    // [databind#2883]: rename via @JsonProperty on inner field should be
    // respected when detecting conflicts
    static class OuterRenameConflict {
        public int id = 0;
        @JsonUnwrapped
        public RenamedToId inner = new RenamedToId();
    }

    static class RenamedToId {
        @JsonProperty("id")
        public int internalKey = 7;
    }

    // Inner field name collides with outer only AFTER snake-case naming strategy
    // ("fooBar" -> "foo_bar") is applied by the mapper.
    static class OuterNamingStrategyConflict {
        public int fooBar = 1;
        @JsonUnwrapped
        public HasFooBar inner = new HasFooBar();
    }

    static class HasFooBar {
        public int fooBar = 2;
    }

    // Self-referential @JsonUnwrapped: not a sensible model (structurally
    // infinite) but conflict-detection must not infinite-recurse or stack
    // overflow while building the serializer for such a type.
    static class SelfUnwrapped {
        public int id = 1;
        @JsonUnwrapped
        public SelfUnwrapped self;
    }

    // Nested unwrapping: Level1 has @JsonUnwrapped Level2 which itself has
    // @JsonUnwrapped Level3. Documents current single-level check scope.
    static class Level1 {
        public String a = "a";
        @JsonUnwrapped
        public Level2 l2 = new Level2();
    }

    static class Level2 {
        public String b = "b";
        @JsonUnwrapped
        public Level3 l3 = new Level3();
    }

    static class Level3 {
        public String c = "c";
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testUnwrappedConflictDetected() throws Exception {
        InvalidDefinitionException ex = assertThrows(
                InvalidDefinitionException.class,
                () -> MAPPER.writeValueAsString(new OuterConflict()));
        verifyException(ex, "Conflict between unwrapped property");
        verifyException(ex, "'b'");
        // Error should also identify the offending unwrapped type
        verifyException(ex, "InnerC");
    }

    @Test
    public void testUnwrappedNoConflictWithPrefix() throws Exception {
        String json = MAPPER.writeValueAsString(new OuterNoConflict());
        // Outer's own `b` stays as "b"; unwrapped InnerC's `b` becomes "c_b"
        assertEquals("{\"b\":{\"ba\":3},\"c_b\":{\"da\":4}}", json);
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

    // Conflict arises via `@JsonProperty` rename on the inner type; check must
    // use post-rename name (the one actually emitted), not the Java field name.
    @Test
    public void testConflictViaJsonPropertyRename() throws Exception {
        InvalidDefinitionException ex = assertThrows(
                InvalidDefinitionException.class,
                () -> MAPPER.writeValueAsString(new OuterRenameConflict()));
        verifyException(ex, "Conflict between unwrapped property");
        verifyException(ex, "'id'");
    }

    // Conflict arises via PropertyNamingStrategy (both outer.fooBar and
    // unwrapped inner.fooBar become "foo_bar"); check must use post-transform name.
    @Test
    public void testConflictViaNamingStrategy() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder()
                .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .build();
        InvalidDefinitionException ex = assertThrows(
                InvalidDefinitionException.class,
                () -> mapper.writeValueAsString(new OuterNamingStrategyConflict()));
        verifyException(ex, "Conflict between unwrapped property");
        verifyException(ex, "'foo_bar'");
    }

    // Nested unwrapping without conflicts flattens all levels; documents that
    // the check traverses through each unwrapped serializer's final property names.
    @Test
    public void testNestedUnwrappedNoConflict() throws Exception {
        String json = MAPPER.writeValueAsString(new Level1());
        assertEquals("{\"a\":\"a\",\"b\":\"b\",\"c\":\"c\"}", json);
    }

    // Conflict-detection must tolerate self-referential @JsonUnwrapped without
    // infinite recursion. Serialization of such a value is nonsensical (would
    // infinite-loop), but building/resolving the serializer must not hang.
    // We give self=null here so we never actually try to serialize the cycle.
    @Test
    public void testSelfReferentialUnwrapDoesNotHang() throws Exception {
        String json = MAPPER.writeValueAsString(new SelfUnwrapped());
        assertTrue(json.contains("\"id\":1"));
    }

    // Inner field that would otherwise clash with outer is ignored via @JsonIgnore,
    // so it never appears in the inner serializer's effective properties and the
    // check must accept the configuration.
    static class OuterConflictResolvedByIgnore {
        public int id = 0;
        @JsonUnwrapped
        public IgnoredIdHolder inner = new IgnoredIdHolder();
    }

    static class IgnoredIdHolder {
        @JsonIgnore
        public int id = 99;
        public int other = 7;
    }

    @Test
    public void testNoConflictWhenUnwrappedPropertyIsIgnored() throws Exception {
        String json = MAPPER.writeValueAsString(new OuterConflictResolvedByIgnore());
        assertEquals("{\"id\":0,\"other\":7}", json);
    }

    // Regular "l3" at outer coexists with inner Level2 that has @JsonUnwrapped
    // Level3 (field named `l3`). The inner Level2's own `l3` name is not
    // emitted (it's unwrapped away), so this is NOT a real conflict.
    // Regression test: the check must not treat inner UnwrappingBeanPropertyWriter
    // field names as emitted names.
    static class OuterWithPhantomCollision {
        public String l3 = "outer";
        @JsonUnwrapped
        public Level2 inner = new Level2();
    }

    @Test
    public void testNoFalseConflictWithNestedUnwrappedFieldName() throws Exception {
        String json = MAPPER.writeValueAsString(new OuterWithPhantomCollision());
        assertTrue(json.contains("\"l3\":\"outer\""), json);
        assertTrue(json.contains("\"b\":\"b\""), json);
        assertTrue(json.contains("\"c\":\"c\""), json);
    }
}
