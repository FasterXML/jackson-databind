package com.fasterxml.jackson.databind.records;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test to verify that both {@link JsonSerialize} and {@link com.fasterxml.jackson.databind.annotation.JsonDeserialize}
 * work on records, as opposed to
 * [jackson#188 discussions](https://github.com/FasterXML/jackson/discussions/188#discussioncomment-11082943)
 */
public class RecordJsonSerDeser188Test
        extends DatabindTestUtil
{

    record Animal(
            @JsonDeserialize(using = PrefixStringDeserializer.class)
            @JsonSerialize(using = PrefixStringSerializer.class)
            String name,
            Integer age
    ) { }

    @SuppressWarnings("serial")
    static class PrefixStringSerializer extends StdScalarSerializer<String> {

        protected PrefixStringSerializer() {
            super(String.class);
        }

        @Override
        public void serialize(String value, JsonGenerator jgen, SerializerProvider provider)
                throws IOException
        {
            jgen.writeString("custom " + value);
        }
    }

    static class PrefixStringDeserializer extends StdScalarDeserializer<String>
    {
        private static final long serialVersionUID = 1L;

        protected PrefixStringDeserializer() {
            super(String.class);
        }

        @Override
        public String deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException
        {
            return "custom-deser" + jp.getText();
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    void testJsonSerializeOnRecord()
            throws Exception
    {
        Animal input = new Animal("dog", 3);

        String JSON = MAPPER.writeValueAsString(input);

        assertEquals(a2q("{'name':'custom dog','age':3}"), JSON);
    }

    @Test
    void testJsonDeserializeOnRecord()
            throws Exception
    {
        String JSON = a2q("{'name':'cat','age':4}");

        Animal result = MAPPER.readValue(JSON, Animal.class);

        assertEquals("custom-desercat", result.name());
        assertEquals(4, result.age());
    }

}
