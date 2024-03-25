package tools.jackson.databind.ser;

import java.util.*;

import org.junit.jupiter.api.Test;

import tools.jackson.core.*;
import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

public class TestJsonSerialize3 extends DatabindTestUtil
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
