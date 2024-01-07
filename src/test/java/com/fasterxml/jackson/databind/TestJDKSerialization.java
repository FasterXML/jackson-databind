package com.fasterxml.jackson.databind;

import java.util.*;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.type.TypeFactory;

/**
 * Tests to verify that most core Jackson components can be serialized
 * using default JDK serialization: this feature is useful for some
 * platforms, such as Android, where memory management is handled
 * much more aggressively.
 */
public class TestJDKSerialization extends BaseMapTest
{
    @JsonPropertyOrder({ "x", "y" })
    static class MyPojo {
        public int x;
        protected int y;

        public MyPojo() { }
        public MyPojo(int x0, int y0) {
            x = x0;
            y = y0;
        }

        public int getY() { return y; }
        public void setY(int y) { this.y = y; }
    }

    // for [databind#899]
    @JsonPropertyOrder({ "abc", "stuff" })
    static class EnumPOJO {
        public ABC abc = ABC.B;

        public Map<String,ABC> stuff = new LinkedHashMap<String,ABC>();
    }

    static class AnyBean {
        HashMap<String,Object> _map;

        public AnyBean() {
            _map = new HashMap<String,Object>();
        }

        @JsonAnySetter
        AnyBean addEntry(String key, Object value) {
            _map.put(key, value);
            return this;
        }

        @JsonAnyGetter
        public Map<String,Object> properties() {
            return _map;
        }
    }


    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    @JsonSubTypes({@JsonSubTypes.Type(value = FooClassImpl.class)})
    public class FooClass { }
    class FooClassImpl extends FooClass { }

    @JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
    @JsonSubTypes({@JsonSubTypes.Type(value = FooDeductionImpl.class)})
    public class FooDeduction { }
    class FooDeductionImpl extends FooDeduction { }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
    @JsonSubTypes({@JsonSubTypes.Type(value = FooNoneImpl.class)})
    public class FooNone { }
    class FooNoneImpl extends FooNone { }

    @JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM)
    @JsonSubTypes({@JsonSubTypes.Type(value = FooCustomImpl.class)})
    public class FooCustom { }
    class FooCustomImpl extends FooCustom { }

    @JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS)
    @JsonSubTypes({@JsonSubTypes.Type(value = FooMinimalClassImpl.class)})
    public class FooMinimalClass { }
    class FooMinimalClassImpl extends FooMinimalClass { }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
    @JsonSubTypes({@JsonSubTypes.Type(value = FooNameImpl.class)})
    public class FooName { }
    class FooNameImpl extends FooName { }

    /*
    /**********************************************************
    /* Tests for individual objects
    /**********************************************************
     */

    /* 18-Oct-2013, tatu: Not sure why, but looks like sharing the default
     *   ObjectMapper here can lead to strange unit test suite failures, so
     *   let's create a private copy for this class only.
     */
    private final ObjectMapper MAPPER = newJsonMapper();

    public void testConfigs() throws Exception
    {
        byte[] base = jdkSerialize(MAPPER.getDeserializationConfig().getBaseSettings());
        assertNotNull(jdkDeserialize(base));

        // first things first: underlying BaseSettings

        DeserializationConfig origDC = MAPPER.getDeserializationConfig();
        SerializationConfig origSC = MAPPER.getSerializationConfig();
        byte[] dcBytes = jdkSerialize(origDC);
        byte[] scBytes = jdkSerialize(origSC);

        DeserializationConfig dc = jdkDeserialize(dcBytes);
        assertNotNull(dc);
        assertEquals(dc._deserFeatures, origDC._deserFeatures);
        SerializationConfig sc = jdkDeserialize(scBytes);
        assertNotNull(sc);
        assertEquals(sc._serFeatures, origSC._serFeatures);
    }

    // for [databind#899]
    public void testEnumHandlers() throws Exception
    {
        ObjectMapper mapper = newJsonMapper();
        // ensure we have serializers and/or deserializers, first
        String json = mapper.writerFor(EnumPOJO.class)
                .writeValueAsString(new EnumPOJO());
        EnumPOJO result = mapper.readerFor(EnumPOJO.class)
                .readValue(json);
        assertNotNull(result);

        // and then use JDK serialization to freeze/thaw objects
        byte[] bytes = jdkSerialize(mapper);
        ObjectMapper mapper2 = jdkDeserialize(bytes);
        assertNotNull(mapper2);

        bytes = jdkSerialize(mapper.readerFor(EnumPOJO.class));
        ObjectReader r = jdkDeserialize(bytes);
        assertNotNull(r);

        /* 14-Aug-2015, tatu: Looks like pre-loading JsonSerializer is problematic
         *    at this point; comment out for now. Try to fix later on.
         */
        bytes = jdkSerialize(mapper.writerFor(EnumPOJO.class));
        ObjectWriter w = jdkDeserialize(bytes);
        assertNotNull(w);

        // plus, ensure objects are usable:
        String json2 = w.writeValueAsString(new EnumPOJO());
        assertEquals(json, json2);
        EnumPOJO result2 = r.readValue(json2);
        assertNotNull(result2);
    }

    public void testObjectWriter() throws Exception
    {
        ObjectWriter origWriter = MAPPER.writer();
        final String EXP_JSON = "{\"x\":2,\"y\":3}";
        final MyPojo p = new MyPojo(2, 3);
        assertEquals(EXP_JSON, origWriter.writeValueAsString(p));
        String json = origWriter.writeValueAsString(new AnyBean()
                .addEntry("a", "b"));
        assertNotNull(json);
        byte[] bytes = jdkSerialize(origWriter);
        ObjectWriter writer2 = jdkDeserialize(bytes);
        assertEquals(EXP_JSON, writer2.writeValueAsString(p));
    }

    public void testObjectReader() throws Exception
    {
        ObjectReader origReader = MAPPER.readerFor(MyPojo.class);
        String JSON = "{\"x\":1,\"y\":2}";
        MyPojo p1 = origReader.readValue(JSON);
        assertEquals(2, p1.y);
        ObjectReader anyReader = MAPPER.readerFor(AnyBean.class);
        AnyBean any = anyReader.readValue(JSON);
        assertEquals(Integer.valueOf(2), any.properties().get("y"));

        byte[] readerBytes = jdkSerialize(origReader);
        ObjectReader reader2 = jdkDeserialize(readerBytes);
        MyPojo p2 = reader2.readValue(JSON);
        assertEquals(2, p2.y);

        ObjectReader anyReader2 = jdkDeserialize(jdkSerialize(anyReader));
        AnyBean any2 = anyReader2.readValue(JSON);
        assertEquals(Integer.valueOf(2), any2.properties().get("y"));
    }

    public void testObjectMapper() throws Exception
    {
        final String EXP_JSON = "{\"x\":2,\"y\":3}";
        final MyPojo p = new MyPojo(2, 3);
        assertEquals(EXP_JSON, MAPPER.writeValueAsString(p));
        assertNotNull(MAPPER.getFactory());
        assertNotNull(MAPPER.getFactory().getCodec());

        byte[] bytes = jdkSerialize(MAPPER);
        ObjectMapper mapper2 = jdkDeserialize(bytes);
        assertEquals(EXP_JSON, mapper2.writeValueAsString(p));
        MyPojo p2 = mapper2.readValue(EXP_JSON, MyPojo.class);
        assertEquals(p.x, p2.x);
        assertEquals(p.y, p2.y);

        // [databind#2038]: verify that codec is not lost
        assertNotNull(mapper2.getFactory());
        assertNotNull(mapper2.getFactory().getCodec());
    }

    public void testTypeFactory() throws Exception
    {
        TypeFactory orig = TypeFactory.defaultInstance();
        JavaType t = orig.constructType(JavaType.class);
        assertNotNull(t);

        byte[] bytes = jdkSerialize(orig);
        TypeFactory result = jdkDeserialize(bytes);
        assertNotNull(result);
        t = orig.constructType(JavaType.class);
        assertEquals(JavaType.class, t.getRawClass());
    }

    // [databind#4303]
    public void testObjectReaderSerializationWithPolymorphism()
        throws Exception
    {
        Class<?>[] classes = new Class<?>[] {
            FooClass.class,
            FooDeduction.class,
            FooNone.class,
            FooCustom.class,
            FooMinimalClass.class,
            FooName.class
        };

        for (Class<?> clazz : classes) {
            // Should be enough to ask for reader for polymorphic type
            // (no need to actually serialize/deserialize)
            ObjectReader reader = newJsonMapper()
                .readerFor(clazz);

            byte[] bytes = jdkSerialize(reader);
            ObjectReader result = jdkDeserialize(bytes);
            assertNotNull(result);
        }
    }
}
