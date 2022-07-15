package tools.jackson.databind.ser;

import java.util.*;

import tools.jackson.core.*;
import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonSerialize;

public class TestJsonSerialize3 extends BaseMapTest
{
    // [JACKSON-829]
    static class FooToBarSerializer extends ValueSerializer<String> {
        @Override
        public void serialize(String value, JsonGenerator g, SerializerProvider provider)
        {
            if ("foo".equals(value)) {
                g.writeString("bar");
            } else {
                g.writeString(value);
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
    
    public void testCustomContentSerializer() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        MyObject object = new MyObject();
        object.list = Arrays.asList("foo");
        String json = m.writeValueAsString(object);
        assertEquals("{\"list\":[\"bar\"]}", json);
    }
}
