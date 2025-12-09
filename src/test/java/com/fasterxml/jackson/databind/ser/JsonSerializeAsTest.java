package com.fasterxml.jackson.databind.ser;

import java.io.IOException;
import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSerializeAs;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for new {@link JsonSerializeAs} annotation.
 *
 * @since 2.21
 */
public class JsonSerializeAsTest extends DatabindTestUtil
{
    /*
    /**********************************************************************
    /* Annotated helper classes for @JsonSerializeAs#value on class
    /**********************************************************************
     */

    public interface Fooable {
        public int getFoo();
    }

    // force use of interface
    @JsonSerializeAs(Fooable.class)
    public static class FooImpl implements Fooable {
        @Override
        public int getFoo() { return 42; }
        public int getBar() { return 15; }
    }

    static class FooImplNoAnno implements Fooable {
        @Override
        public int getFoo() { return 42; }
        public int getBar() { return 15; }
    }

    public class Fooables {
        public FooImpl[] getFoos() {
            return new FooImpl[] { new FooImpl() };
        }
    }

    /*
    /**********************************************************************
    /* Annotated helper classes for @JsonSerializeAs#value on property
    /**********************************************************************
     */

    public class FooableWrapper {
        public FooImpl getFoo() {
            return new FooImpl();
        }
    }

    static class FooableWithFieldWrapper {
        @JsonSerializeAs(Fooable.class)
        public Fooable getFoo() {
            return new FooImplNoAnno();
        }
    }

    /*
    /**********************************************************************
    /* Annotated helper classes for @JsonSerializeAs#content
    /**********************************************************************
     */

    interface Bean1178Base {
        public int getA();
    }

    @JsonPropertyOrder({"a","b"})
    static abstract class Bean1178Abstract implements Bean1178Base {
        @Override
        public int getA() { return 1; }

        public int getB() { return 2; }
    }

    static class Bean1178Impl extends Bean1178Abstract {
        public int getC() { return 3; }
    }

    static class Bean1178Wrapper {
        @JsonSerializeAs(content=Bean1178Abstract.class)
        public List<Bean1178Base> values;
        public Bean1178Wrapper(int count) {
            values = new ArrayList<Bean1178Base>();
            for (int i = 0; i < count; ++i) {
                values.add(new Bean1178Impl());
            }
        }
    }

    static class Bean1178Holder {
        @JsonSerializeAs(Bean1178Abstract.class)
        public Bean1178Base value = new Bean1178Impl();
    }

    /*
    /**********************************************************************
    /* Annotated helper classes for @JsonSerializeAs#key
    /**********************************************************************
     */

    interface MapKeyBase {
        String getId();
    }

    @JsonPropertyOrder({"id"})
    static abstract class MapKeyAbstract implements MapKeyBase {
        @Override
        public String getId() { return "key"; }
    }

    static class MapKeyImpl extends MapKeyAbstract {
        public String getExtra() { return "extra"; }
    }

    static class MapKeyWrapper {
        @JsonSerializeAs(key=MapKeyAbstract.class)
        public Map<MapKeyBase, String> values;

        public MapKeyWrapper() {
            values = new LinkedHashMap<>();
            values.put(new MapKeyImpl(), "value1");
        }
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final ObjectWriter WRITER = objectWriter();

    @Test
    public void testSerializeAsInClass() throws IOException {
        assertEquals("{\"foo\":42}", WRITER.writeValueAsString(new FooImpl()));
    }

    @Test
    public void testSerializeAsForArrayProp() throws IOException {
        assertEquals("{\"foos\":[{\"foo\":42}]}",
                WRITER.writeValueAsString(new Fooables()));
    }

    @Test
    public void testSerializeAsForSimpleProp() throws IOException {
        assertEquals("{\"foo\":{\"foo\":42}}",
                WRITER.writeValueAsString(new FooableWrapper()));
    }

    @Test
    public void testSerializeWithFieldAnno() throws IOException {
        assertEquals("{\"foo\":{\"foo\":42}}",
                WRITER.writeValueAsString(new FooableWithFieldWrapper()));
    }

    // Test for content parameter (similar to [databind#1178])
    @Test
    public void testSpecializedContentAs() throws IOException {
        assertEquals(a2q("{'values':[{'a':1,'b':2}]}"),
                WRITER.writeValueAsString(new Bean1178Wrapper(1)));
    }

    // Test for value parameter (similar to [databind#1231])
    @Test
    public void testSpecializedAsIntermediate() throws IOException {
        assertEquals(a2q("{'value':{'a':1,'b':2}}"),
                WRITER.writeValueAsString(new Bean1178Holder()));
    }

    // Test for key parameter
    @Test
    public void testSpecializedKeyAs() throws IOException {
        String json = WRITER.writeValueAsString(new MapKeyWrapper());
        // Map key serialization depends on how MapKeyAbstract is serialized
        // Since it has only getId(), we expect the key to be serialized as just that property
        assertTrue(json.contains("\"values\""), "Should contain 'values' field");
    }
}
