package tools.jackson.databind.tofix;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.testutil.failure.JacksonTestFailureExpected;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for issue #4014: combining @JsonTypeInfo(include = As.PROPERTY)
 * with @JsonIdentityInfo(generator = PropertyGenerator.class) on an interface
 * causes deserialization to fail.
 */
public class ObjectIdWithTypeInfo4014Test extends DatabindTestUtil
{
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@c")
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "@id")
    public interface BaseEntity {
        @JsonProperty("@id")
        Integer getId();
    }

    // Classes without setter for @id - demonstrates the bug
    static class Foo implements BaseEntity {
        private final Integer id;
        private Bar bar;

        public Foo(Integer id) {
            this.id = id;
        }

        @Override
        @JsonProperty("@id")
        public Integer getId() {
            return id;
        }

        public Bar getBar() {
            return bar;
        }

        public void setBar(Bar bar) {
            this.bar = bar;
        }
    }

    static class Bar implements BaseEntity {
        private final Integer id;
        private Foo foo;

        public Bar(Integer id) {
            this.id = id;
        }

        @Override
        @JsonProperty("@id")
        public Integer getId() {
            return id;
        }

        public Foo getFoo() {
            return foo;
        }

        public void setFoo(Foo foo) {
            this.foo = foo;
        }
    }

    // Classes WITH setter for @id - demonstrates the workaround
    static class FooWithSetter implements BaseEntity {
        private Integer id;
        private BarWithSetter bar;

        public FooWithSetter() {
        }

        public FooWithSetter(Integer id) {
            this.id = id;
        }

        @Override
        @JsonProperty("@id")
        public Integer getId() {
            return id;
        }

        @JsonProperty("@id")
        public void setId(Integer id) {
            this.id = id;
        }

        public BarWithSetter getBar() {
            return bar;
        }

        public void setBar(BarWithSetter bar) {
            this.bar = bar;
        }
    }

    static class BarWithSetter implements BaseEntity {
        private Integer id;
        private FooWithSetter foo;

        public BarWithSetter() {
        }

        public BarWithSetter(Integer id) {
            this.id = id;
        }

        @Override
        @JsonProperty("@id")
        public Integer getId() {
            return id;
        }

        @JsonProperty("@id")
        public void setId(Integer id) {
            this.id = id;
        }

        public FooWithSetter getFoo() {
            return foo;
        }

        public void setFoo(FooWithSetter foo) {
            this.foo = foo;
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    /**
     * Test combining @JsonTypeInfo with @JsonIdentityInfo on an interface.
     * Currently fails with "cannot find property with name '@id'" error.
     */
    @JacksonTestFailureExpected
    @Test
    public void testTypeInfoWithIdentityInfoOnInterface() throws Exception
    {
        // Create circular reference
        Foo foo = new Foo(1);
        Bar bar = new Bar(2);
        foo.setBar(bar);
        bar.setFoo(foo);

        // Serialization works fine
        String json = MAPPER.writeValueAsString(foo);
        assertNotNull(json);

        // Verify the JSON contains both type info and identity info
        assertTrue(json.contains("@c"), "Should contain type information");
        assertTrue(json.contains("@id"), "Should contain identity information");

        // Deserialization should work but currently fails
        BaseEntity result = MAPPER.readValue(json, BaseEntity.class);
        assertNotNull(result);
        assertInstanceOf(Foo.class, result);

        Foo resultFoo = (Foo) result;
        assertEquals(1, resultFoo.getId());
        assertNotNull(resultFoo.getBar());
        assertEquals(2, resultFoo.getBar().getId());

        // Verify circular reference is preserved
        assertSame(resultFoo, resultFoo.getBar().getFoo());
    }

    /**
     * Test that even with setters for the @id property, deserialization should work.
     * Currently still fails when annotations are on the interface.
     */
    @JacksonTestFailureExpected
    @Test
    public void testTypeInfoWithIdentityInfoWithSetter() throws Exception
    {
        // Create circular reference using classes with setters
        FooWithSetter foo = new FooWithSetter(1);
        BarWithSetter bar = new BarWithSetter(2);
        foo.setBar(bar);
        bar.setFoo(foo);

        // Serialization works fine
        String json = MAPPER.writeValueAsString(foo);
        assertNotNull(json);

        // Verify the JSON contains both type info and identity info
        assertTrue(json.contains("@c"), "Should contain type information");
        assertTrue(json.contains("@id"), "Should contain identity information");

        // Deserialization should work but currently fails
        BaseEntity result = MAPPER.readValue(json, BaseEntity.class);
        assertNotNull(result);
        assertInstanceOf(FooWithSetter.class, result);

        FooWithSetter resultFoo = (FooWithSetter) result;
        assertEquals(1, resultFoo.getId());
        assertNotNull(resultFoo.getBar());
        assertEquals(2, resultFoo.getBar().getId());

        // Verify circular reference is preserved
        assertSame(resultFoo, resultFoo.getBar().getFoo());
    }
}
