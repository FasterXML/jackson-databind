package com.fasterxml.jackson.databind;

import java.io.*;
import java.util.*;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.SimpleLookupCache;

/**
 * Tests to verify that most core Jackson components can be serialized
 * using default JDK serialization: this feature is useful for some
 * platforms, such as Android, where memory management is handled
 * much more aggressively.
 */
public class MapperJDKSerializationTest extends BaseMapTest
{
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

    /*
    /**********************************************************
    /* Tests for individual objects
    /**********************************************************
     */

    /* 18-Oct-2013, tatu: Not sure why, but looks like sharing the default
     *   ObjectMapper here can lead to strange unit test suite failures, so
     *   let's create a private copy for this class only.
     */
    private final ObjectMapper MAPPER = new ObjectMapper();
    
    public void testConfigs() throws IOException
    {
        byte[] base = jdkSerialize(MAPPER.deserializationConfig().getBaseSettings());
        assertNotNull(jdkDeserialize(base));

        // first things first: underlying BaseSettings

        DeserializationConfig origDC = MAPPER.deserializationConfig();
        SerializationConfig origSC = MAPPER.serializationConfig();
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
    public void testEnumHandlers() throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();
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

    public void testObjectWriter() throws IOException
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
    
    public void testObjectReader() throws IOException
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

    public void testObjectMapper() throws IOException
    {
        final String EXP_JSON = "{\"x\":2,\"y\":3}";
        final MyPojo p = new MyPojo(2, 3);
        assertEquals(EXP_JSON, MAPPER.writeValueAsString(p));

        byte[] bytes = jdkSerialize(MAPPER);
        ObjectMapper mapper2 = jdkDeserialize(bytes);
        assertEquals(EXP_JSON, mapper2.writeValueAsString(p));
        MyPojo p2 = mapper2.readValue(EXP_JSON, MyPojo.class);
        assertEquals(p.x, p2.x);
        assertEquals(p.y, p2.y);
    }

    public void testMapperWithModule() throws IOException
    {
        SimpleModule module = new SimpleModule("JDKSerTestModule", Version.unknownVersion());
        {
            byte[] b = jdkSerialize(module);
            assertNotNull(b);
        }
        
        ObjectMapper mapper = ObjectMapper.builder()
                .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
                .addModule(module)
                .build();

        // just force serialization first
        final String EXP_JSON = "{\"x\":2,\"y\":3}";
        final MyPojo p = new MyPojo(2, 3);
        assertEquals(EXP_JSON, mapper.writeValueAsString(p));

        byte[] bytes = jdkSerialize(mapper);
        ObjectMapper mapper2 = jdkDeserialize(bytes);

        // verify settings
        assertTrue(mapper.isEnabled(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS));
        assertTrue(mapper.isEnabled(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY));
        
        assertEquals(EXP_JSON, mapper2.writeValueAsString(p));
        MyPojo p2 = mapper2.readValue(EXP_JSON, MyPojo.class);
        assertEquals(p.x, p2.x);
        assertEquals(p.y, p2.y);

        // and then reconfigure a bit
        ObjectMapper mapper3 = mapper2.rebuild()
                .disable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                .disable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
                .build();
        assertFalse(mapper3.isEnabled(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS));
        assertFalse(mapper3.isEnabled(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY));
        bytes = jdkSerialize(mapper3);
        ObjectMapper mapper4 = jdkDeserialize(bytes);

        assertFalse(mapper4.isEnabled(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS));
        assertFalse(mapper4.isEnabled(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY));
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

    public void testLRUMap() throws Exception
    {
        SimpleLookupCache<String,Integer> map = new SimpleLookupCache<String,Integer>(32, 32);
        map.put("a", 1);

        byte[] bytes = jdkSerialize(map);
        SimpleLookupCache<String,Integer> result = jdkDeserialize(bytes);
        // transient implementation, will be read as empty
        assertEquals(0, result.size());

        // but should be possible to re-populate
        result.put("a", 2);
        assertEquals(1, result.size());
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */
    
    protected byte[] jdkSerialize(Object o) throws IOException
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(1000);
        ObjectOutputStream obOut = new ObjectOutputStream(bytes);
        obOut.writeObject(o);
        obOut.close();
        return bytes.toByteArray();
    }

    @SuppressWarnings("unchecked")
    protected <T> T jdkDeserialize(byte[] raw) throws IOException
    {
        ObjectInputStream objIn = new ObjectInputStream(new ByteArrayInputStream(raw));
        try {
            return (T) objIn.readObject();
        } catch (ClassNotFoundException e) {
            fail("Missing class: "+e.getMessage());
            return null;
        } finally {
            objIn.close();
        }
    }
}
