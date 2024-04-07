package com.fasterxml.jackson.databind.ser.filter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [databind#4464] Since 2.15, NON_DEFAULT should be extension of NON_EMPTY
public class JsonInclude4464Test {

    public static class BarSerializer extends JsonSerializer<Bar> {

        public BarSerializer() {
        }

        @Override
        public void serialize(Bar value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeObject(value);
        }

        @Override
        public boolean isEmpty(SerializerProvider provider, Bar value) {
            return "I_AM_EMPTY".equals(value.getName());
        }
    }

    public static class Bar {
        public String getName() {
            return "I_AM_EMPTY";
        }
    }

    public static class Foo {
        @JsonSerialize(using = BarSerializer.class)
        public Bar getBar() {
            return new Bar();
        }
    }

    @Test
    public void test86() throws IOException {
        ObjectMapper mapper = JsonMapper.builder().serializationInclusion(JsonInclude.Include.NON_DEFAULT).build();
        String json = mapper.writeValueAsString(new Foo());
        assertEquals("{}", json);
    }
}
