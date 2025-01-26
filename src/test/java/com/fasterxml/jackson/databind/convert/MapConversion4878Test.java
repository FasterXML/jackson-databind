package com.fasterxml.jackson.databind.convert;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.module.SimpleSerializers;
import com.fasterxml.jackson.databind.ser.std.StdDelegatingSerializer;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;
import com.fasterxml.jackson.databind.util.StdConverter;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MapConversion4878Test extends DatabindTestUtil
{
    // [databind#4878]
    static class MapWrapper4878 {
        final Map<String, Object> value;

        MapWrapper4878(Map<String, Object> value) {
            this.value = value;
        }
    }

    static class WrapperConverter4878 extends StdConverter<MapWrapper4878, Object> {
        @Override
        public Object convert(MapWrapper4878 value) {
            return value.value;
        }
    }

    @SuppressWarnings("serial")
    static class Serializers4878 extends SimpleSerializers {
        @Override
        public JsonSerializer<?> findSerializer(SerializationConfig config, JavaType type, BeanDescription beanDesc) {
            Class<?> rawClass = type.getRawClass();
            if (MapWrapper4878.class.isAssignableFrom(rawClass)) {
                return new StdDelegatingSerializer(new WrapperConverter4878());
            }
            return super.findSerializer(config, type, beanDesc);
        }
    }

    // [databind#4878]
    @Test
    public void testMapConverter() throws Exception
    {
        SimpleModule sm = new SimpleModule();
        sm.setSerializers(new Serializers4878());
        final ObjectMapper mapper = jsonMapperBuilder()
                .addModule(sm)
                .build();
        String json = mapper.writeValueAsString(new MapWrapper4878(Collections.singletonMap("a", 1)));
        assertEquals(a2q("{'a':1}"), json);
    }
}
