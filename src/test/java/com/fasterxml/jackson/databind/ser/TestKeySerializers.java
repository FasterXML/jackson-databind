package com.fasterxml.jackson.databind.ser;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class TestKeySerializers extends BaseMapTest
{
    public static class KarlSerializer extends JsonSerializer<String>
    {
        @Override
        public void serialize(String value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
            jgen.writeFieldName("Karl");
        }
    }

    public static class NotKarlBean
    {
        public Map<String,Integer> map = new HashMap<String,Integer>();
        {
            map.put("Not Karl", 1);
        }
    }

    public static class KarlBean
    {
        @JsonSerialize(keyUsing = KarlSerializer.class)
        public Map<String,Integer> map = new HashMap<String,Integer>();
        {
            map.put("Not Karl", 1);
        }
    }

    enum ABC {
        A, B, C
    }

    static class ABCKeySerializer extends JsonSerializer<ABC> {
        @Override
        public void serialize(ABC value, JsonGenerator jgen,
                SerializerProvider provider) throws IOException {
            jgen.writeFieldName("xxx"+value);
        }
    }

    static class ABCMapWrapper {
        public Map<ABC,String> stuff = new HashMap<ABC,String>();
        public ABCMapWrapper() {
            stuff.put(ABC.B, "bar");
        }
    }

    static class BAR<T>{
        T value;

        public BAR(T value) {
            this.value = value;
        }

        @JsonValue
        public T getValue() {
            return value;
        }

        @Override
        public String toString() {
            return this.getClass().getSimpleName()
                    + ", value:" + value
                    ;
        }
    }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */
    
    public void testNotKarl() throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        final String serialized = mapper.writeValueAsString(new NotKarlBean());
        assertEquals("{\"map\":{\"Not Karl\":1}}", serialized);
    }

    public void testKarl() throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        final String serialized = mapper.writeValueAsString(new KarlBean());
        assertEquals("{\"map\":{\"Karl\":1}}", serialized);
    }

    // [Issue#75]: caching of KeySerializers
    public void testBoth() throws IOException
    {
        final ObjectMapper mapper = new ObjectMapper();
        final String value1 = mapper.writeValueAsString(new NotKarlBean());
        assertEquals("{\"map\":{\"Not Karl\":1}}", value1);
        final String value2 = mapper.writeValueAsString(new KarlBean());
        assertEquals("{\"map\":{\"Karl\":1}}", value2);
    }

    // Test custom key serializer for enum
    public void testCustomForEnum() throws IOException
    {
        final ObjectMapper mapper = new ObjectMapper();
        SimpleModule mod = new SimpleModule("test");
        mod.addKeySerializer(ABC.class, new ABCKeySerializer());
        mapper.registerModule(mod);

        String json = mapper.writeValueAsString(new ABCMapWrapper());
        assertEquals("{\"stuff\":{\"xxxB\":\"bar\"}}", json);
    }

    // [databind#838]
    public void testUnWrappedMapWithDefaultType() throws Exception{
        final ObjectMapper mapper = new ObjectMapper();
        SimpleModule mod = new SimpleModule("test");
        mod.addKeySerializer(ABC.class, new ABCKeySerializer());
        mapper.registerModule(mod);

        TypeResolverBuilder<?> typer = new ObjectMapper.DefaultTypeResolverBuilder(ObjectMapper.DefaultTyping.NON_FINAL);
        typer = typer.init(JsonTypeInfo.Id.NAME, null);
        typer = typer.inclusion(JsonTypeInfo.As.PROPERTY);
        //typer = typer.typeProperty(TYPE_FIELD);
        typer = typer.typeIdVisibility(true);
        mapper.setDefaultTyping(typer);

        Map<ABC,String> stuff = new HashMap<ABC,String>();
        stuff.put(ABC.B, "bar");
        String json = mapper.writerFor(new TypeReference<Map<ABC, String>>() {})
                .writeValueAsString(stuff);
        assertEquals("{\"@type\":\"HashMap\",\"xxxB\":\"bar\"}", json);
    }

    // [databind#838]
    public void testUnWrappedMapWithKeySerializer() throws Exception{
        SimpleModule mod = new SimpleModule("test");
        mod.addKeySerializer(ABC.class, new ABCKeySerializer());
        final ObjectMapper mapper = new ObjectMapper()
            .registerModule(mod)
            .enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(SerializationFeature.WRITE_NULL_MAP_VALUES)
            .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
            ;

        Map<ABC,BAR<?>> stuff = new HashMap<ABC,BAR<?>>();
        stuff.put(ABC.B, new BAR<String>("bar"));
        String json = mapper.writerFor(new TypeReference<Map<ABC,BAR<?>>>() {})
                .writeValueAsString(stuff);
        assertEquals("{\"xxxB\":\"bar\"}", json);
    }
}
