package tools.jackson.databind.deser.jdk;

import java.util.Map;

import org.junit.jupiter.api.Test;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [databind#4444]
public class CustomMapKeyDeserializer4444Test extends DatabindTestUtil
{
    @JsonDeserialize(keyUsing = ForClass.class)
    static class MyKey {
        private final String value;

        MyKey(String value) {
            this.value = value;
        }
    }

    static class ForClass extends KeyDeserializer {
        @Override
        public Object deserializeKey(String key, DeserializationContext ctxt) {
            return new MyKey(key + "-class");
        }
    }

    static class ForMapper extends KeyDeserializer {
        @Override
        public Object deserializeKey(String key, DeserializationContext ctxt) {
            return new MyKey(key + "-mapper");
        }
    }

    // It is not declared as new TypeReference<> because it causes a compile error in Java 8.
    TypeReference<Map<MyKey, String>> typeRef = new TypeReference<Map<MyKey, String>>() {
    };

    @Test
    void withoutForClass() throws Exception {
        ObjectMapper mapper = newJsonMapper();
        Map<MyKey, String> result = mapper.readValue("{\"foo\":null}", typeRef);

        assertEquals("foo-class", result.keySet().stream().findFirst().get().value);
    }

    // The KeyDeserializer set by the annotation must not be overwritten by the KeyDeserializer set in the mapper.
    @Test
    void withForClass() throws Exception {
        SimpleModule sm = new SimpleModule();
        sm.addKeyDeserializer(MyKey.class, new ForMapper());

        ObjectMapper mapper = jsonMapperBuilder().addModule(sm).build();
        Map<MyKey, String> result = mapper.readValue("{\"foo\":null}", typeRef);

        assertEquals("foo-class", result.keySet().stream().findFirst().get().value);
    }
}
