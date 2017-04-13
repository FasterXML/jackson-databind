package com.fasterxml.jackson.databind.deser;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class NullHandlingTest extends BaseMapTest
{
    static class FunnyNullDeserializer extends JsonDeserializer<String>
    {
        @Override
        public String deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
            return "text";
        }

        @Override
        public String getNullValue(DeserializationContext ctxt) { return "funny"; }
    }

    static class AnySetter{

        private Map<String,String> any = new HashMap<String,String>();

        @JsonAnySetter
        public void setAny(String name, String value){
            this.any.put(name,value);
        }

        public Map<String,String> getAny(){
            return this.any;
        }
    }
    
    // [databind#1601]
    static class RootData {
        public String name;
        public String type;
        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
                property = "type")
        @JsonSubTypes({
                @Type(value = TypeA.class, name = "TypeA"),
                @Type(value = TypeB.class, name = "TypeB")})
        public Proxy proxy;

        public RootData() {}

        public RootData(String name, String type, Proxy proxy) {
            this.name = name;
            this.type = type;
            this.proxy = proxy;
        }
    }
    static interface Proxy { }

    static class TypeA implements Proxy {
        public String aValue;
        public TypeA() {}
        public TypeA(String a) {
            this.aValue = a;
        }
    }

    static class TypeB implements Proxy {
        public String bValue;
        public TypeB() {}
        public TypeB(String b) {
            this.bValue = b;
        }
    }

    private final ObjectMapper MAPPER = objectMapper();

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    public void testNull() throws Exception
    {
        // null doesn't really have a type, fake by assuming Object
        Object result = MAPPER.readValue("   null", Object.class);
        assertNull(result);
    }  
    
    public void testAnySetterNulls() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule("test", Version.unknownVersion());
        module.addDeserializer(String.class, new FunnyNullDeserializer());
        mapper.registerModule(module);

        String fieldName = "fieldName";
        String nullValue = "{\""+fieldName+"\":null}";

        // should get non-default null directly:
        AnySetter result = mapper.readValue(nullValue, AnySetter.class);

        assertEquals(1, result.getAny().size());
        assertNotNull(result.getAny().get(fieldName));
        assertEquals("funny", result.getAny().get(fieldName));

        // as well as via ObjectReader
        ObjectReader reader = mapper.readerFor(AnySetter.class);
        result = reader.readValue(nullValue);

        assertEquals(1, result.getAny().size());
        assertNotNull(result.getAny().get(fieldName));
        assertEquals("funny", result.getAny().get(fieldName));
    }

    public void testCustomRootNulls() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule("test", Version.unknownVersion());
        module.addDeserializer(String.class, new FunnyNullDeserializer());
        mapper.registerModule(module);

        // should get non-default null directly:
        String str = mapper.readValue("null", String.class);
        assertNotNull(str);
        assertEquals("funny", str);
        
        // as well as via ObjectReader
        ObjectReader reader = mapper.readerFor(String.class);
        str = reader.readValue("null");
        assertNotNull(str);
        assertEquals("funny", str);
    }

    // [databind#407]
    public void testListOfNulls() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule("test", Version.unknownVersion());
        module.addDeserializer(String.class, new FunnyNullDeserializer());
        mapper.registerModule(module);

        List<String> list = Arrays.asList("funny");
        JavaType type = mapper.getTypeFactory().constructCollectionType(List.class, String.class);

        // should get non-default null directly:
        List<?> deser = mapper.readValue("[null]", type);
        assertNotNull(deser);
        assertEquals(1, deser.size());
        assertEquals(list.get(0), deser.get(0));

        // as well as via ObjectReader
        ObjectReader reader = mapper.readerFor(type);
        deser = reader.readValue("[null]");
        assertNotNull(deser);
        assertEquals(1, deser.size());
        assertEquals(list.get(0), deser.get(0));
    }

    // Test for [#407]
    public void testMapOfNulls() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule("test", Version.unknownVersion());
        module.addDeserializer(String.class, new FunnyNullDeserializer());
        mapper.registerModule(module);

        JavaType type = mapper.getTypeFactory().constructMapType(Map.class, String.class, String.class);
        // should get non-default null directly:
        Map<?,?> deser = mapper.readValue("{\"key\":null}", type);
        assertNotNull(deser);
        assertEquals(1, deser.size());
        assertEquals("funny", deser.get("key"));

        // as well as via ObjectReader
        ObjectReader reader = mapper.readerFor(type);
        deser = reader.readValue("{\"key\":null}");
        assertNotNull(deser);
        assertEquals(1, deser.size());
        assertEquals("funny", deser.get("key"));
    }

    // [databind#1601]
    public void testPolymorphicDataNull() throws Exception
    {
        String typeA =
                "{\"name\":\"TypeAData\", \"type\":\"TypeA\", \"proxy\":{\"aValue\":\"This works!\"}}";
        RootData typeAData = MAPPER.readValue(typeA, RootData.class);
        assertEquals("No value for aValue!?", "This works!", ((TypeA) typeAData.proxy).aValue);
        String typeB =
                "{\"name\":\"TypeBData\", \"type\":\"TypeB\", \"proxy\":{\"bValue\":\"This works too!\"}}";
        RootData typeBData = MAPPER.readValue(typeB, RootData.class);
        assertEquals("No value for bValue!?", "This works too!", ((TypeB) typeBData.proxy).bValue);
        String typeBNull =
                "{\"name\":\"TypeBData\", \"type\":\"TypeB\", \"proxy\": null}";
        RootData typeBNullData = MAPPER.readValue(typeBNull, RootData.class);
        assertNull("Proxy should be null!", typeBNullData.proxy);
    }
}
