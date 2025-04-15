package tools.jackson.databind.records;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.deser.std.StdScalarDeserializer;
import tools.jackson.databind.ser.std.StdScalarSerializer;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test to verify that both {@link JsonSerialize} and {@link tools.jackson.databind.annotation.JsonDeserialize}
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

    static class PrefixStringSerializer extends StdScalarSerializer<String> {

        protected PrefixStringSerializer() {
            super(String.class);
        }

        @Override
        public void serialize(String value, JsonGenerator jgen, SerializationContext provider)
        {
            jgen.writeString("custom " + value);
        }
    }

    static class PrefixStringDeserializer extends StdScalarDeserializer<String>
    {
        protected PrefixStringDeserializer() {
            super(String.class);
        }

        @Override
        public String deserialize(JsonParser jp, DeserializationContext ctxt)
        {
            return "custom-deser" + jp.getString();
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
