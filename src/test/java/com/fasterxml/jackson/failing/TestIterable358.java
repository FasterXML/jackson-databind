package com.fasterxml.jackson.failing;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.core.JsonGenerator;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public class TestIterable358 extends BaseMapTest
{
    // [Issue#358]
    static class A {
        public String unexpected = "Bye.";
    }

    static class B {
        @JsonSerialize(as = Iterable.class, contentUsing = ASerializer.class)
        public List<A> list = Arrays.asList(new A());
    }
    static class ASerializer extends JsonSerializer<A> {
        @Override
        public void serialize(A a, JsonGenerator jsonGenerator, SerializerProvider provider) throws IOException {
            jsonGenerator.writeStartArray();
            jsonGenerator.writeString("Hello world.");
            jsonGenerator.writeEndArray();
        }
    }

    final ObjectMapper MAPPER = new ObjectMapper();
    
    public void testIterable358() throws Exception {
        String json = MAPPER.writeValueAsString(new B());
        assertEquals("{\"list\":[[\"Hello world.\"]]}", json);
    }

}
