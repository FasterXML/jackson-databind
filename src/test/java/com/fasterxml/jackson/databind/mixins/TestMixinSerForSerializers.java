package com.fasterxml.jackson.databind.mixins;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;
import java.util.Map;

public class TestMixinSerForSerializers
    extends BaseMapTest
{
    /*
    /**********************************************************
    /* Helper bean classes
    /**********************************************************
     */

    @Retention(RetentionPolicy.RUNTIME)
    @JsonProperty("dummy")
    @JacksonAnnotationsInside
    @JsonSerialize(using = DummySerializer.class)
    static @interface Dummy {}

    static class DummySerializer extends JsonSerializer<String>
    {

        @Override
        public void serialize(String field, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
            String a = field == null ? "null-field" : "not-null-field";
            jsonGenerator.writeObject(a);
        }
    }


    abstract class MixIn
    {
        @Dummy
        public String a;
    }

    static class BaseClass
    {
        public String a;
        protected String b;

        public BaseClass(String a, String b) {
            this.a = a;
            this.b = b;
        }
    }
    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    public void testWithSerializerMixIns() throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        HashMap<Class<?>,Class<?>> mixins = new HashMap<Class<?>,Class<?>>();
        mixins.put(BaseClass.class, MixIn.class);
        mapper.setMixInAnnotations(mixins);

        Map<String,Object> result;
        result = writeAndMap(mapper, new BaseClass("1", "2"));
        assertEquals(1, result.size());
        assertEquals("not-null-field", result.get("dummy"));

        result = writeAndMap(mapper, new BaseClass(null, "2"));
        assertEquals(1, result.size());
        assertEquals("null-field", result.get("dummy"));
    }
}
