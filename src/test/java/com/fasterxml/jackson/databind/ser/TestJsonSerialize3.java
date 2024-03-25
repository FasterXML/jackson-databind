package com.fasterxml.jackson.databind.ser;

import java.io.IOException;
import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

public class TestJsonSerialize3 extends DatabindTestUtil
{
    // [JACKSON-829]
    static class FooToBarSerializer extends JsonSerializer<String> {
        @Override
        public void serialize(String value, JsonGenerator jgen, SerializerProvider provider)
               throws IOException {
            if ("foo".equals(value)) {
                jgen.writeString("bar");
            } else {
                jgen.writeString(value);
            }
        }
    }

    static class MyObject {
        @JsonSerialize(contentUsing = FooToBarSerializer.class)
        List<String> list;
    }
    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    @Test
    public void testCustomContentSerializer() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        MyObject object = new MyObject();
        object.list = Arrays.asList("foo");
        String json = m.writeValueAsString(object);
        assertEquals("{\"list\":[\"bar\"]}", json);
    }
}
