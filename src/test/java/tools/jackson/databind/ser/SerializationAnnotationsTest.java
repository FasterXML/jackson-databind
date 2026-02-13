package tools.jackson.databind.ser;

import java.io.*;
import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.core.*;
import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.StdSerializer;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This unit test suite tests use of some basic Jackson annotations for
 * bean serialization.
 */
public class SerializationAnnotationsTest
    extends DatabindTestUtil
{
    // Class for testing {@link JsonProperty} annotations with getters
    final static class SizeClassGetter
    {
        @JsonProperty public int size() { return 3; }
        @JsonProperty("length") public int foobar() { return -17; }
        // note: need not be public since there's annotation
        @JsonProperty protected int value() { return 0; }

        // dummy method; not a getter signature
        protected int getNotReally(int arg) { return 0; }
    }

    // And additional testing to cover [JACKSON-64]
    final static class SizeClassGetter2
    {
        // Should still be considered property "x"
        @JsonProperty protected int getX() { return 3; }
    }

    // and some support for testing [JACKSON-120]
    final static class SizeClassGetter3
    {
        // Should be considered property "y" even tho non-public
        @JsonSerialize protected int getY() { return 8; }
    }

    /**
     * Class for testing {@link ValueSerializer} annotation
     * for class itself.
     */
    @JsonSerialize(using=BogusSerializer.class)
    final static class ClassSerializer {
    }

    /**
     * Class for testing an active {@link JsonSerialize#using} annotation
     * for a method
     */
    final static class ClassMethodSerializer {
        private int _x;

        public ClassMethodSerializer(int x) { _x = x; }

        @JsonSerialize(using=StringSerializer.class)
        public int getX() { return _x; }
    }

    /**
     * Class for testing an inactive (one that will not have any effect)
     * {@link JsonSerialize} annotation for a method
     */
    final static class InactiveClassMethodSerializer {
        private int _x;

        public InactiveClassMethodSerializer(int x) { _x = x; }

        // Basically, has no effect, hence gets serialized as number
        @JsonSerialize(using=ValueSerializer.None.class)
        public int getX() { return _x; }
    }

    /**
     * Class for verifying that getter information is inherited
     * as expected via normal class inheritance
     */
    static class BaseBean {
        public int getX() { return 1; }
        @JsonProperty("y")
        private int getY() { return 2; }
    }

    static class SubClassBean extends BaseBean {
        public int getZ() { return 3; }
    }

    // Base class for testing {@link JsonProperty} annotations
    static class BasePojo
    {
        @JsonProperty public int width() { return 3; }
        @JsonProperty public int length() { return 7; }
    }

    /**
     * It should also be possible to specify annotations on interfaces,
     * to be implemented by classes. This should not only work when interface
     * is used (which may be the case for de-serialization) but also
     * when implementing class is used and overrides methods. In latter
     * case overriding methods should still "inherit" annotations -- this
     * is not something JVM runtime provides, but Jackson class
     * instrospector does.
     */
    interface PojoInterface
    {
        @JsonProperty int width();
        @JsonProperty int length();
    }

    /**
     * Sub-class for testing that inheritance is handled properly
     * wrt annotations.
     */
    static class PojoSubclass extends BasePojo
    {
        /**
         * Should still be recognized as a Getter here.
         */
        @Override
        public int width() { return 9; }
    }

    static class PojoImpl implements PojoInterface
    {
        // Both should be recognized as getters here

        @Override
        public int width() { return 1; }
        @Override
        public int length() { return 2; }

        public int getFoobar() { return 5; }
    }

    /*
    /**********************************************************
    /* Other helper classes
    /**********************************************************
     */

    public final static class BogusSerializer extends StdSerializer<Object>
    {
        public BogusSerializer() { super(Object.class); }
        @Override
        public void serialize(Object value, JsonGenerator g, SerializationContext provider)
        {
            g.writeBoolean(true);
        }
    }

    private final static class StringSerializer extends StdSerializer<Object>
    {
        public StringSerializer() { super(Object.class); }
        @Override
        public void serialize(Object value, JsonGenerator g, SerializationContext provider)
        {
            g.writeString("X"+value+"X");
        }

    }

    /*
    /**********************************************************
    /* Main tests
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testSimpleGetter() throws Exception
    {
        Map<String,Object> result = writeAndMap(MAPPER, new SizeClassGetter());
        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(3), result.get("size"));
        assertEquals(Integer.valueOf(-17), result.get("length"));
        assertEquals(Integer.valueOf(0), result.get("value"));
    }

    @Test
    public void testSimpleGetter2() throws Exception
    {
        Map<String,Object> result = writeAndMap(MAPPER, new SizeClassGetter2());
        assertEquals(1, result.size());
        assertEquals(Integer.valueOf(3), result.get("x"));
    }

    @Test
    public void testSimpleGetter3() throws Exception
    {
        Map<String,Object> result = writeAndMap(MAPPER, new SizeClassGetter3());
        assertEquals(1, result.size());
        assertEquals(Integer.valueOf(8), result.get("y"));
    }

    /**
     * Let's also verify that inherited super-class getters are used
     * as expected
     */
    @Test
    public void testGetterInheritance() throws Exception
    {
        Map<String,Object> result = writeAndMap(MAPPER, new SubClassBean());
        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(1), result.get("x"));
        assertEquals(Integer.valueOf(2), result.get("y"));
        assertEquals(Integer.valueOf(3), result.get("z"));
    }

    @Test
    public void testSimpleGetterInheritance() throws Exception
    {
        Map<String,Object> result = writeAndMap(MAPPER, new PojoSubclass());
        assertEquals(2, result.size());
        assertEquals(Integer.valueOf(7), result.get("length"));
        assertEquals(Integer.valueOf(9), result.get("width"));
    }

    @Test
    public void testSimpleGetterInterfaceImpl() throws Exception
    {
        Map<String,Object> result = writeAndMap(MAPPER, new PojoImpl());
        // should get 2 from interface, and one more from impl itself
        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(5), result.get("foobar"));
        assertEquals(Integer.valueOf(1), result.get("width"));
        assertEquals(Integer.valueOf(2), result.get("length"));
    }
    
    /**
     * Unit test to verify that {@link JsonSerialize#using} annotation works
     * when applied to a class
     */
    @Test
    public void testClassSerializer() throws Exception
    {
        StringWriter sw = new StringWriter();
        MAPPER.writeValue(sw, new ClassSerializer());
        assertEquals("true", sw.toString());
    }

    /**
     * Unit test to verify that {@code @JsonSerialize} annotation works
     * when applied to a Method
     */
    @Test
    public void testActiveMethodSerializer() throws Exception
    {
        StringWriter sw = new StringWriter();
        MAPPER.writeValue(sw, new ClassMethodSerializer(13));
        // Here we will get wrapped as an object, since we have
        // full object, just override a single property
        assertEquals("{\"x\":\"X13X\"}", sw.toString());
    }

    @Test
    public void testInactiveMethodSerializer() throws Exception
    {
        String json = MAPPER.writeValueAsString(new InactiveClassMethodSerializer(8));
        // Here we will get wrapped as an object, since we have
        // full object, just override a single property
        assertEquals("{\"x\":8}", json);
    }

}
