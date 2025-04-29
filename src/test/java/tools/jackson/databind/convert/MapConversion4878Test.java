package tools.jackson.databind.convert;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonFormat;

import tools.jackson.databind.*;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.module.SimpleSerializers;
import tools.jackson.databind.ser.std.StdDelegatingSerializer;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.util.StdConverter;

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
        public ValueSerializer<?> findSerializer(SerializationConfig config,
                JavaType type, BeanDescription.Supplier beanDescRef, JsonFormat.Value formatOverrides) {
            Class<?> rawClass = type.getRawClass();
            if (MapWrapper4878.class.isAssignableFrom(rawClass)) {
                return new StdDelegatingSerializer(new WrapperConverter4878());
            }
            return super.findSerializer(config, type, beanDescRef, formatOverrides);
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
