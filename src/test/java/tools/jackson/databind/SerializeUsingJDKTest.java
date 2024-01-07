package tools.jackson.databind;

import java.util.*;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.core.Version;

import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.type.TypeFactory;

/**
 * Tests to verify that most core Jackson components can be serialized
 * using default JDK serialization: this feature is useful for some
 * platforms, such as Android, where memory management is handled
 * much more aggressively.
 */
public class SerializeUsingJDKTest extends BaseMapTest
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

    /*
    /**********************************************************************
    /* Tests for individual objects
    /**********************************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    public void testConfigs() throws Exception
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

        // plus, ensure objects are usable:
        String json2 = mapper2.writerFor(EnumPOJO.class)
                .writeValueAsString(new EnumPOJO());
        assertEquals(json, json2);
        EnumPOJO result2 = mapper2.readValue(json2, EnumPOJO.class);
        assertNotNull(result2);
    }

    public void testObjectMapper() throws Exception
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

    public void testObjectWriter() throws Exception
    {
        // 20-Apr-2018, tatu: ObjectReader no longer JDK serializable so
        //    can only check via thawed ObjectMapper

        byte[] bytes = jdkSerialize(MAPPER);
        ObjectMapper mapper2 = jdkDeserialize(bytes);
        ObjectWriter origWriter = mapper2.writer();
        final String EXP_JSON = "{\"x\":2,\"y\":3}";
        final MyPojo p = new MyPojo(2, 3);
        assertEquals(EXP_JSON, origWriter.writeValueAsString(p));
        String json = origWriter.writeValueAsString(new AnyBean()
                .addEntry("a", "b"));
        assertEquals("{\"a\":\"b\"}", json);
    }

    public void testObjectReader() throws Exception
    {
        // 20-Apr-2018, tatu: ObjectReader no longer JDK serializable so
        //    can only check via thawed ObjectMapper

        byte[] bytes = jdkSerialize(MAPPER);
        ObjectMapper mapper2 = jdkDeserialize(bytes);

        ObjectReader origReader = mapper2.readerFor(MyPojo.class);
        String JSON = "{\"x\":1,\"y\":2}";
        MyPojo p1 = origReader.readValue(JSON);
        assertEquals(2, p1.y);
        ObjectReader anyReader = mapper2.readerFor(AnyBean.class);
        AnyBean any = anyReader.readValue(JSON);
        assertEquals(Integer.valueOf(2), any.properties().get("y"));
    }

    public void testMapperWithModule() throws Exception
    {
        SimpleModule module = new SimpleModule("JDKSerTestModule", Version.unknownVersion());
        {
            byte[] b = jdkSerialize(module);
            assertNotNull(b);
        }

        JsonMapper mapper = JsonMapper.builder()
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
}
