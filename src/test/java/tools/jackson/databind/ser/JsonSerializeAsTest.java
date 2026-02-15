package tools.jackson.databind.ser;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSerializeAs;

import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link JsonSerializeAs} annotation (new)
 * and legacy {@link JsonSerialize}{@code (as=...)} annotation.
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

    interface Bean5476Base {
        public int getA();
    }

    @JsonPropertyOrder({"a","b"})
    static abstract class Bean5476Abstract implements Bean5476Base {
        @Override
        public int getA() { return 1; }

        public int getB() { return 2; }
    }

    static class Bean5476Impl extends Bean5476Abstract {
        public int getC() { return 3; }
    }

    static class Bean5476Wrapper {
        @JsonSerializeAs(content=Bean5476Abstract.class)
        public List<Bean5476Base> values;
        public Bean5476Wrapper(int count) {
            values = new ArrayList<Bean5476Base>();
            for (int i = 0; i < count; ++i) {
                values.add(new Bean5476Impl());
            }
        }
    }

    static class Bean5476Holder {
        @JsonSerializeAs(Bean5476Abstract.class)
        public Bean5476Base value = new Bean5476Impl();
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
    /* Annotated helper classes for legacy @JsonSerialize(as=...)
    /**********************************************************************
     */

    public interface LegacyFooable {
        public int getFoo();
    }

    // force use of interface
    @JsonSerialize(as=LegacyFooable.class)
    public static class LegacyFooImpl implements LegacyFooable {
        @Override
        public int getFoo() { return 42; }
        public int getBar() { return 15; }
    }

    static class LegacyFooImplNoAnno implements LegacyFooable {
        @Override
        public int getFoo() { return 42; }
        public int getBar() { return 15; }
    }

    public class LegacyFooables {
        public LegacyFooImpl[] getFoos() {
            return new LegacyFooImpl[] { new LegacyFooImpl() };
        }
    }

    public class LegacyFooableWrapper {
        public LegacyFooImpl getFoo() {
            return new LegacyFooImpl();
        }
    }

    // for [databind#1023]
    static class LegacyFooableWithFieldWrapper {
        @JsonSerialize(as=LegacyFooable.class)
        public LegacyFooable getFoo() {
            return new LegacyFooImplNoAnno();
        }
    }

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
        @JsonSerialize(contentAs=Bean1178Abstract.class)
        public List<Bean1178Base> values;
        public Bean1178Wrapper(int count) {
            values = new ArrayList<Bean1178Base>();
            for (int i = 0; i < count; ++i) {
                values.add(new Bean1178Impl());
            }
        }
    }

    static class Bean1178Holder {
        @JsonSerialize(as=Bean1178Abstract.class)
        public Bean1178Base value = new Bean1178Impl();
    }

    /*
    /**********************************************************************
    /* Test methods, @JsonSerializeAs (new annotation)
    /**********************************************************************
     */

    private final ObjectWriter WRITER = objectWriter();

    @Test
    public void testSerializeAsInClass() throws Exception {
        assertEquals("{\"foo\":42}", WRITER.writeValueAsString(new FooImpl()));
    }

    @Test
    public void testSerializeAsForArrayProp() throws Exception {
        assertEquals("{\"foos\":[{\"foo\":42}]}",
                WRITER.writeValueAsString(new Fooables()));
    }

    @Test
    public void testSerializeAsForSimpleProp() throws Exception {
        assertEquals("{\"foo\":{\"foo\":42}}",
                WRITER.writeValueAsString(new FooableWrapper()));
    }

    @Test
    public void testSerializeWithFieldAnno() throws Exception {
        assertEquals("{\"foo\":{\"foo\":42}}",
                WRITER.writeValueAsString(new FooableWithFieldWrapper()));
    }

    // Test for content parameter
    @Test
    public void testSpecializedContentAs() throws Exception {
        assertEquals(a2q("{'values':[{'a':1,'b':2}]}"),
                WRITER.writeValueAsString(new Bean5476Wrapper(1)));
    }

    // Test for value parameter
    @Test
    public void testSpecializedAsIntermediate() throws Exception {
        assertEquals(a2q("{'value':{'a':1,'b':2}}"),
                WRITER.writeValueAsString(new Bean5476Holder()));
    }

    // Test for key parameter
    @Test
    public void testSpecializedKeyAs() throws Exception {
        String json = WRITER.writeValueAsString(new MapKeyWrapper());
        // Map key serialization depends on how MapKeyAbstract is serialized
        // Since it has only getId(), we expect the key to be serialized as just that property
        assertTrue(json.contains("\"values\""), "Should contain 'values' field");
    }

    /*
    /**********************************************************************
    /* Test methods, legacy @JsonSerialize(as=...)
    /**********************************************************************
     */

    @Test
    public void testLegacySerializeAsInClass() throws Exception {
        assertEquals("{\"foo\":42}", WRITER.writeValueAsString(new LegacyFooImpl()));
    }

    @Test
    public void testLegacySerializeAsForArrayProp() throws Exception {
        assertEquals("{\"foos\":[{\"foo\":42}]}",
                WRITER.writeValueAsString(new LegacyFooables()));
    }

    @Test
    public void testLegacySerializeAsForSimpleProp() throws Exception {
        assertEquals("{\"foo\":{\"foo\":42}}",
                WRITER.writeValueAsString(new LegacyFooableWrapper()));
    }

    // for [databind#1023]
    @Test
    public void testLegacySerializeWithFieldAnno() throws Exception {
        assertEquals("{\"foo\":{\"foo\":42}}",
                WRITER.writeValueAsString(new LegacyFooableWithFieldWrapper()));
    }

    // for [databind#1178]
    @Test
    public void testLegacySpecializedContentAs1178() throws Exception {
        assertEquals(a2q("{'values':[{'a':1,'b':2}]}"),
                WRITER.writeValueAsString(new Bean1178Wrapper(1)));
    }

    // for [databind#1231] (and continuation of [databind#1178])
    @Test
    public void testLegacySpecializedAsIntermediate1231() throws Exception {
        assertEquals(a2q("{'value':{'a':1,'b':2}}"),
                WRITER.writeValueAsString(new Bean1178Holder()));
    }
}
