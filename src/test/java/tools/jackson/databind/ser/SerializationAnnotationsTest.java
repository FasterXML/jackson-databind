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
 * bean serialization, as well as serialization ordering constraints.
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

    interface PojoInterface
    {
        @JsonProperty int width();
        @JsonProperty int length();
    }

    static class PojoSubclass extends BasePojo
    {
        @Override
        public int width() { return 9; }
    }

    static class PojoImpl implements PojoInterface
    {
        @Override
        public int width() { return 1; }
        @Override
        public int length() { return 2; }

        public int getFoobar() { return 5; }
    }

    /*
    /**********************************************************
    /* Other helper classes (annotations test)
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

    // From SerializationOrderTest

    static class BeanWithCreator
    {
        public int a;
        public int b;
        public int c;

        @JsonCreator
        public BeanWithCreator(@JsonProperty("c") int c, @JsonProperty("a") int a) {
            this.a = a;
            this.c = c;
        }
    }

    @JsonPropertyOrder({"c", "a", "b"})
    static class BeanWithOrder
    {
        public int d, b, a, c;

        public BeanWithOrder(int a, int b, int c, int d) {
            this.a = a;
            this.b = b;
            this.c = c;
            this.d = d;
        }
    }

    @JsonPropertyOrder(value={"d"}, alphabetic=true)
    static class SubBeanWithOrder extends BeanWithOrder
    {
        public SubBeanWithOrder(int a, int b, int c, int d) {
            super(a, b, c, d);
        }
    }

    @JsonPropertyOrder({"b", "a",
        // note: including non-existant properties is fine (has no effect, but not an error)
        "foobar",
        "c"
    })
    static class OrderMixIn { }

    @JsonPropertyOrder(value={"a","b","x","z"})
    static class BeanFor268 {
        @JsonProperty("a") public String xA = "a";
        @JsonProperty("z") public String aZ = "z";
    	   @JsonProperty("b") public String xB() { return "b"; }
    	   @JsonProperty("x") public String aX() { return "x"; }
    }

    static class BeanFor459 {
        public int d = 4;
        public int c = 3;
        public int b = 2;
        public int a = 1;
    }

    // For [databind#311]
    @JsonPropertyOrder(alphabetic = true)
    static class BeanForGH311 {
        private final int a;
        private final int b;

        @JsonCreator
        public BeanForGH311(@JsonProperty("b") int b, @JsonProperty("a") int a) {
            this.a = a;
            this.b = b;
        }

        public int getA() { return a; }
        public int getB() { return b; }
    }

    // We'll expect ordering of "FUBAR"
    @JsonPropertyOrder({ "f"  })
    static class OrderingByIndexBean {
        public int r;
        public int a;

        @JsonProperty(index = 1)
        public int b;

        @JsonProperty(index = 0)
        public int u;

        public int f;
    }

    // For [databind#2879]
    @JsonPropertyOrder({ "a", "c" })
    static class BeanFor2879 {
        public int c;
        public int b;
        public int a;

        @JsonCreator
        public BeanFor2879(@JsonProperty("a") int a,
                @JsonProperty("b") int b,
                @JsonProperty("c") int c) {
            this.a = a;
            this.b = b;
            this.c = c;
        }
    }

    // For [databind#2879]
    static class BeanForStrictOrdering {
        private final int a;
        private int b;
        private final int c;

        @JsonCreator
        public BeanForStrictOrdering(@JsonProperty("c") int c, @JsonProperty("a") int a) {
            this.a = a;
            this.c = c;
        }

        public int getA() { return a; }
        public int getB() { return b; }
        public int getC() { return c; }
    }

    /*
    /**********************************************************
    /* Main tests
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    private final ObjectMapper ALPHA_MAPPER = jsonMapperBuilder()
            .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .build();

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

    @Test
    public void testClassSerializer() throws Exception
    {
        StringWriter sw = new StringWriter();
        MAPPER.writeValue(sw, new ClassSerializer());
        assertEquals("true", sw.toString());
    }

    @Test
    public void testActiveMethodSerializer() throws Exception
    {
        StringWriter sw = new StringWriter();
        MAPPER.writeValue(sw, new ClassMethodSerializer(13));
        assertEquals("{\"x\":\"X13X\"}", sw.toString());
    }

    @Test
    public void testInactiveMethodSerializer() throws Exception
    {
        String json = MAPPER.writeValueAsString(new InactiveClassMethodSerializer(8));
        assertEquals("{\"x\":8}", json);
    }

    // From SerializationOrderTest

    @Test
    public void testImplicitOrderByCreator() throws Exception {
        assertEquals("{\"c\":1,\"a\":2,\"b\":0}",
                MAPPER.writeValueAsString(new BeanWithCreator(1, 2)));
    }

    @Test
    public void testExplicitOrder() throws Exception {
        assertEquals("{\"c\":3,\"a\":1,\"b\":2,\"d\":4}",
                MAPPER.writeValueAsString(new BeanWithOrder(1, 2, 3, 4)));
    }

    @Test
    public void testAlphabeticOrder() throws Exception {
        assertEquals("{\"d\":4,\"a\":1,\"b\":2,\"c\":3}",
                MAPPER.writeValueAsString(new SubBeanWithOrder(1, 2, 3, 4)));
    }

    @Test
    public void testOrderWithMixins() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .addMixIn(BeanWithOrder.class, OrderMixIn.class)
                .build();
        assertEquals("{\"b\":2,\"a\":1,\"c\":3,\"d\":4}",
                mapper.writeValueAsString(new BeanWithOrder(1, 2, 3, 4)));
    }

    @Test
    public void testOrderWrt268() throws Exception
    {
        assertEquals("{\"a\":\"a\",\"b\":\"b\",\"x\":\"x\",\"z\":\"z\"}",
                MAPPER.writeValueAsString(new BeanFor268()));
    }

    @Test
    public void testOrderWithFeature() throws Exception
    {
        assertEquals("{\"a\":1,\"b\":2,\"c\":3,\"d\":4}",
                ALPHA_MAPPER.writeValueAsString(new BeanFor459()));
    }

    // [databind#2879]: verify that Creator properties never override explicit order
    @Test
    public void testCreatorVsExplicitOrdering() throws Exception
    {
        assertEquals(a2q("{'a':1,'c':3,'b':2}"),
                MAPPER.writeValueAsString(new BeanFor2879(1, 2, 3)));
        assertEquals(a2q("{'a':1,'c':3,'b':2}"),
                ALPHA_MAPPER.writeValueAsString(new BeanFor2879(1, 2, 3)));
    }

    // [databind#311]
    @Test
    public void testAlphaAndCreatorOrdering() throws Exception
    {
        assertEquals(a2q("{'b':2,'a':1}"),
                ALPHA_MAPPER.writeValueAsString(new BeanForGH311(2, 1)));
        final ObjectMapper mapper = jsonMapperBuilder()
                .disable(MapperFeature.SORT_CREATOR_PROPERTIES_FIRST)
                .build();
        assertEquals(a2q("{'a':1,'b':2}"),
                mapper.writeValueAsString(new BeanForGH311(2, 1)));
    }

    // [databind#2555]
    @Test
    public void testOrderByIndexEtc() throws Exception
    {
        // since "default" order can actually vary with later JDKs, only verify
        // case of alphabetic-as-default
        assertEquals(a2q("{'f':0,'u':0,'b':0,'a':0,'r':0}"),
                ALPHA_MAPPER.writeValueAsString(new OrderingByIndexBean()));
    }

    // [databind#2879]: allow preventing Creator properties from overriding alphabetic ordering
    @Test
    public void testStrictAlphaAndCreatorOrdering() throws Exception
    {
        // without changing defaults, creators are sorted before other properties
        assertTrue(ALPHA_MAPPER.isEnabled(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY));
        assertTrue(ALPHA_MAPPER.isEnabled(MapperFeature.SORT_CREATOR_PROPERTIES_FIRST));
        assertEquals(a2q("{'c':2,'a':3,'b':0}"),
                ALPHA_MAPPER.writeValueAsString(new BeanForStrictOrdering(2, 3)));

        // but can change that
        final ObjectMapper STRICT_ALPHA_MAPPER = jsonMapperBuilder()
                .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                .disable(MapperFeature.SORT_CREATOR_PROPERTIES_FIRST)
                .build();

        assertEquals(a2q("{'a':2,'b':0,'c':1}"),
                STRICT_ALPHA_MAPPER.writeValueAsString(new BeanForStrictOrdering(1, 2)));
    }
}
