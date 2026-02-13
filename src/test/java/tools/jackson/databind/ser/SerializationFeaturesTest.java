package tools.jackson.databind.ser;

import java.io.*;
import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.exc.InvalidDefinitionException;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for checking handling of some of {@link MapperFeature}s
 * and {@link SerializationFeature}s for serialization.
 */
public class SerializationFeaturesTest
    extends DatabindTestUtil
{
    static class CloseableBean implements AutoCloseable
    {
        public int a = 3;

        protected boolean wasClosed = false;

        @Override
        public void close() throws IOException {
            wasClosed = true;
        }
    }

    private static class StringListBean {
        @SuppressWarnings("unused")
        public Collection<String> values;

        public StringListBean(Collection<String> v) { values = v; }
    }

    static class Empty { }

    @JsonSerialize
    static class EmptyWithAnno { }

    @JsonSerialize(using=NonZeroSerializer.class)
    static class NonZero {
        public int nr;

        public NonZero(int i) { nr = i; }
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    static class NonZeroWrapper {
        public NonZero value;

        public NonZeroWrapper(int i) {
            value = new NonZero(i);
        }
    }

    static class NonZeroSerializer extends ValueSerializer<NonZero>
    {
        @Override
        public void serialize(NonZero value, JsonGenerator g, SerializationContext ctxt)
        {
            g.writeNumber(value.nr);
        }

        @Override
        public boolean isEmpty(SerializationContext ctxt, NonZero value) {
            if (value == null) return true;
            return (value.nr == 0);
        }
    }

    // For [JACKSON-666] ("SerializationFeature of the Beast!")
    @JsonPropertyOrder(alphabetic=true)
    static class GettersWithoutSetters
    {
        public int d = 0;

        @JsonCreator
        public GettersWithoutSetters(@JsonProperty("a") int a) { }

        // included, since there is a constructor property
        public int getA() { return 3; }

        // not included, as there's nothing matching
        public int getB() { return 4; }

        // include as there is setter
        public int getC() { return 5; }
        public void setC(int v) { }

        // and included, as there is a field
        public int getD() { return 6; }
    }

    // [JACKSON-806]: override 'need-setter' with explicit annotation
    static class GettersWithoutSetters2
    {
        @JsonProperty
        public int getA() { return 123; }
    }

    // for [databind#736]
    public static class Data736 {
        private int readonly;
        private int readwrite;

        public Data736() {
            readonly = 1;
            readwrite = 2;
        }

        public int getReadwrite() {
            return readwrite;
        }
        public void setReadwrite(int readwrite) {
            this.readwrite = readwrite;
        }
        public int getReadonly() {
            return readonly;
        }
    }

    /*
    /**********************************************************************
    /* Test methods, SerializationFeature.CLOSE_CLOSEABLE
    /**********************************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    @SuppressWarnings("resource")
    @Test
    public void testCloseCloseable() throws Exception
    {
        // default should be disabled:
        CloseableBean bean = new CloseableBean();
        MAPPER.writeValueAsString(bean);
        assertFalse(bean.wasClosed);

        // via writer as well
        bean = new CloseableBean();
        MAPPER.writer()
            .writeValueAsString(bean);
        assertFalse(bean.wasClosed);
        
        // but can enable it:
        ObjectMapper mapper2 = jsonMapperBuilder()
                .enable(SerializationFeature.CLOSE_CLOSEABLE)
                .build();
        bean = new CloseableBean();
        mapper2.writeValueAsString(bean);
        assertTrue(bean.wasClosed);

        // and same via writer
        bean = new CloseableBean();
        mapper2.writer()
            .writeValueAsString(bean);
        assertTrue(bean.wasClosed);

        // also: let's ensure that ObjectWriter won't interfere with it
        bean = new CloseableBean();
        MAPPER.writerFor(CloseableBean.class)
            .with(SerializationFeature.CLOSE_CLOSEABLE)
            .writeValueAsString(bean);
        assertTrue(bean.wasClosed);
    }

    /*
    /**********************************************************************
    /* SerializationFeature.WRITE_CHAR_ARRAYS_AS_JSON_ARRAYS
    /**********************************************************************
     */
    
    @Test
    public void testCharArrays() throws Exception
    {
        char[] chars = new char[] { 'a','b','c' };
        ObjectMapper m = new ObjectMapper();
        // default: serialize as Strings
        assertEquals(q("abc"), m.writeValueAsString(chars));

        // new feature: serialize as JSON array:
        assertEquals("[\"a\",\"b\",\"c\"]",
                m.writer()
                .with(SerializationFeature.WRITE_CHAR_ARRAYS_AS_JSON_ARRAYS)
                .writeValueAsString(chars));
    }

    /*
    /**********************************************************************
    /* Test methods, SerializationFeature.FLUSH_AFTER_WRITE_VALUE
    /**********************************************************************
     */

    @Test
    public void testFlushingAutomatic() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        assertTrue(mapper.serializationConfig().isEnabled(SerializationFeature.FLUSH_AFTER_WRITE_VALUE));
        // default is to flush after writeValue()
        StringWriter sw = new StringWriter();
        mapper.writeValue(sw, Integer.valueOf(13));
        assertEquals("13", sw.toString());

        // ditto with ObjectWriter
        sw = new StringWriter();
        ObjectWriter ow = mapper.writer();
        ow.writeValue(sw, Integer.valueOf(99));
        assertEquals("99", sw.toString());
    }

    @Test
    public void testFlushingNotAutomatic() throws Exception
    {
        // but should not occur if configured otherwise
        ObjectMapper mapper = jsonMapperBuilder()
                .configure(SerializationFeature.FLUSH_AFTER_WRITE_VALUE, false)
                .build();
        StringWriter sw = new StringWriter();
        JsonGenerator g = mapper.createGenerator(sw);

        mapper.writeValue(g, Integer.valueOf(13));
        // no flushing now:
        assertEquals("", sw.toString());
        // except when actually flushing
        g.flush();
        assertEquals("13", sw.toString());
        g.close();
        // Also, same should happen with ObjectWriter
        sw = new StringWriter();
        g = mapper.createGenerator(sw);
        ObjectWriter ow = mapper.writer();
        ow.writeValue(g, Integer.valueOf(99));
        assertEquals("", sw.toString());
        // except when actually flushing
        g.flush();
        assertEquals("99", sw.toString());
        g.close();
    }

    /*
    /**********************************************************************
    /* Test methods, SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED
    /**********************************************************************
     */
    
    @Test
    public void testSingleElementCollections() throws Exception
    {
        final ObjectWriter writer = objectWriter()
                .with(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED);

        // Lists:
        ArrayList<String> strs = new ArrayList<String>();
        strs.add("xyz");
        assertEquals(q("xyz"), writer.writeValueAsString(strs));
        ArrayList<Integer> ints = new ArrayList<Integer>();
        ints.add(13);
        assertEquals("13", writer.writeValueAsString(ints));

        // other Collections, like Sets:
        HashSet<Long> longs = new HashSet<Long>();
        longs.add(42L);
        assertEquals("42", writer.writeValueAsString(longs));
        // [databind#180]
        final String EXP_STRINGS = "{\"values\":\"foo\"}";
        assertEquals(EXP_STRINGS, writer.writeValueAsString(new StringListBean(Collections.singletonList("foo"))));

        final Set<String> SET = new HashSet<String>();
        SET.add("foo");
        assertEquals(EXP_STRINGS, writer.writeValueAsString(new StringListBean(SET)));

        // arrays:
        assertEquals("true", writer.writeValueAsString(new boolean[] { true }));
        assertEquals("[true,false]", writer.writeValueAsString(new boolean[] { true, false }));
        assertEquals("true", writer.writeValueAsString(new Boolean[] { Boolean.TRUE }));

        assertEquals("3", writer.writeValueAsString(new short[] { 3 }));
        assertEquals("[3,2]", writer.writeValueAsString(new short[] { 3, 2 }));

        assertEquals("3", writer.writeValueAsString(new int[] { 3 }));
        assertEquals("[3,2]", writer.writeValueAsString(new int[] { 3, 2 }));

        assertEquals("1", writer.writeValueAsString(new long[] { 1L }));
        assertEquals("[-1,4]", writer.writeValueAsString(new long[] { -1L, 4L }));

        assertEquals("0.5", writer.writeValueAsString(new double[] { 0.5 }));
        assertEquals("[0.5,2.5]", writer.writeValueAsString(new double[] { 0.5, 2.5 }));

        assertEquals("0.5", writer.writeValueAsString(new float[] { 0.5f }));
        assertEquals("[0.5,2.5]", writer.writeValueAsString(new float[] { 0.5f, 2.5f }));

        assertEquals(q("foo"), writer.writeValueAsString(new String[] { "foo" }));
    }

    /*
    /**********************************************************************
    /* Test methods, SerializationFeature.FAIL_ON_EMPTY_BEANS
    /**********************************************************************
     */
    
    @Test
    public void testEmptyWithAnnotations() throws Exception
    {
        // First: without annotations, should complain
        try {
            MAPPER.writer()
                .with(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                    .writeValueAsString(new Empty());
            fail("Should fail");
        } catch (InvalidDefinitionException e) {
            verifyException(e, "No serializer found for class");
        }

        // But not if there is a recognized annotation
        assertEquals("{}", MAPPER.writeValueAsString(new EmptyWithAnno()));

        // Including class annotation through mix-ins
        ObjectMapper m2 = jsonMapperBuilder()
                .addMixIn(Empty.class, EmptyWithAnno.class)
                .build();
        assertEquals("{}", m2.writeValueAsString(new Empty()));
    }

    /**
     * Alternative it is possible to use a feature to allow
     * serializing empty classes, too
     */
    @Test
    public void testEmptyWithFeature() throws Exception
    {
        // should be disabled by default as of 3.x
        assertFalse(MAPPER.isEnabled(SerializationFeature.FAIL_ON_EMPTY_BEANS));
        assertEquals("{}",
                MAPPER.writer()
                    .writeValueAsString(new Empty()));
    }

    @Test
    public void testCustomNoEmpty() throws Exception
    {
        // first non-empty:
        assertEquals("{\"value\":123}", MAPPER.writeValueAsString(new NonZeroWrapper(123)));
        // then empty:
        assertEquals("{}", MAPPER.writeValueAsString(new NonZeroWrapper(0)));
    }

    /*
    /**********************************************************************
    /* Test methods, SerializationFeature.REQUIRE_SETTERS_FOR_GETTERS
    /**********************************************************************
     */

    @Test
    public void testGettersWithoutSetters() throws Exception
    {
        assertFalse(MAPPER.isEnabled(MapperFeature.REQUIRE_SETTERS_FOR_GETTERS));

        // by default, all 4 found:
        GettersWithoutSetters bean = new GettersWithoutSetters(123);
        assertEquals("{\"a\":3,\"b\":4,\"c\":5,\"d\":6}", MAPPER.writeValueAsString(bean));

        // but 3 if we require mutator:
        ObjectMapper m = jsonMapperBuilder()
                .enable(MapperFeature.REQUIRE_SETTERS_FOR_GETTERS)
                .build();
        assertEquals("{\"a\":3,\"c\":5,\"d\":6}", m.writeValueAsString(bean));
    }

    @Test
    public void testGettersWithoutSettersOverride() throws Exception
    {
        ObjectMapper m = jsonMapperBuilder()
                .enable(MapperFeature.REQUIRE_SETTERS_FOR_GETTERS)
                .build();
        assertEquals("{\"a\":123}", m.writeValueAsString(new GettersWithoutSetters2()));
    }

    // for [databind#736]
    @Test
    public void testNeedForSetters() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .changeDefaultVisibility(vc -> vc
                        .withVisibility(PropertyAccessor.ALL, Visibility.NONE)
                        .withVisibility(PropertyAccessor.FIELD, Visibility.NONE)
                        .withVisibility(PropertyAccessor.GETTER, Visibility.PUBLIC_ONLY)
                        .withVisibility(PropertyAccessor.SETTER, Visibility.PUBLIC_ONLY))
                .enable(MapperFeature.REQUIRE_SETTERS_FOR_GETTERS)
                .build();
        Data736 dataB = new Data736();

        String json = mapper.writeValueAsString(dataB);
        assertEquals(a2q("{'readwrite':2}"), json);
    }
}
