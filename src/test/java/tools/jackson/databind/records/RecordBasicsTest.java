package tools.jackson.databind.records;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonNaming;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.deser.std.StdScalarDeserializer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.ser.std.StdScalarSerializer;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.type.TypeFactory;
import tools.jackson.databind.util.Converter;

import static org.junit.jupiter.api.Assertions.*;

public class RecordBasicsTest extends DatabindTestUtil
{
    record EmptyRecord() { }

    record SimpleRecord(int id, String name) { }

    record RecordOfRecord(SimpleRecord record) { }

    record RecordWithRename(int id, @JsonProperty("rename")String name) { }

    record RecordWithHeaderInject(int id, @JacksonInject String name) { }

    record RecordWithConstructorInject(int id, String name) {

        RecordWithConstructorInject(int id, @JacksonInject String name) {
            this.id = id;
            this.name = name;
        }
    }

    // [databind#2992]
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    record SnakeRecord(String myId, String myValue){}

    record RecordWithJsonDeserialize(int id, @JsonDeserialize(converter = StringTrimmer.class) String name) { }

    record RecordSingleWriteOnly(@JsonProperty(access = JsonProperty.Access.WRITE_ONLY) int id) { }

    record RecordSomeWriteOnly(
            @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) int id,
            @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) String name,
            String email) {
    }

    record RecordAllWriteOnly(
            @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) int id,
            @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) String name,
            @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) String email) {
    }

    // [databind#4175]
    private static record PrivateTextRecord4175(String text) { }

    // [databind#5967]
    public record RenamedRecord5967(@JsonProperty("renamedProp") String prop) {}

    // [jackson#188]
    record Animal188(
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
        public void serialize(String value, JsonGenerator jgen, SerializationContext provider) {
            jgen.writeString("custom " + value);
        }
    }

    static class PrefixStringDeserializer extends StdScalarDeserializer<String> {
        protected PrefixStringDeserializer() {
            super(String.class);
        }

        @Override
        public String deserialize(JsonParser jp, DeserializationContext ctxt) {
            return "custom-deser" + jp.getString();
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    /*
    /**********************************************************************
    /* Test methods, Record type introspection
    /**********************************************************************
     */

    @Test
    public void testRecordJavaType() {
        assertFalse(MAPPER.constructType(getClass()).isRecordType());

        assertTrue(MAPPER.constructType(SimpleRecord.class).isRecordType());
        assertTrue(MAPPER.constructType(RecordOfRecord.class).isRecordType());
        assertTrue(MAPPER.constructType(RecordWithRename.class).isRecordType());
    }

    /*
    /**********************************************************************
    /* Test methods, default reading/writing Record values
    /**********************************************************************
     */

    @Test
    public void testSerializeSimpleRecord() throws Exception {
        String json = MAPPER.writeValueAsString(new SimpleRecord(123, "Bob"));
        final Object EXP = map("id", Integer.valueOf(123), "name", "Bob");
        assertEquals(EXP, MAPPER.readValue(json, Object.class));
    }

    @Test
    public void testDeserializeSimpleRecord() throws Exception {
        assertEquals(new SimpleRecord(123, "Bob"),
                MAPPER.readValue("{\"id\":123,\"name\":\"Bob\"}", SimpleRecord.class));
    }

    @Test
    public void testSerializeEmptyRecord() throws Exception {
        assertEquals("{}", MAPPER.writeValueAsString(new EmptyRecord()));
    }

    @Test
    public void testDeserializeEmptyRecord() throws Exception {
        assertEquals(new EmptyRecord(),
                MAPPER.readValue("{}", EmptyRecord.class));
    }

    @Test
    public void testSerializeRecordOfRecord() throws Exception {
        RecordOfRecord record = new RecordOfRecord(new SimpleRecord(123, "Bob"));
        String json = MAPPER.writeValueAsString(record);
        final Object EXP = Collections.singletonMap("record",
                map("id", Integer.valueOf(123), "name", "Bob"));
        assertEquals(EXP, MAPPER.readValue(json, Object.class));
    }

    @Test
    public void testDeserializeRecordOfRecord() throws Exception {
        assertEquals(new RecordOfRecord(new SimpleRecord(123, "Bob")),
                MAPPER.readValue("{\"record\":{\"id\":123,\"name\":\"Bob\"}}",
                        RecordOfRecord.class));
    }

    /*
    /**********************************************************************
    /* Test methods, reading/writing Record values with different config
    /**********************************************************************
     */

    @Test
    public void testSerializeSimpleRecord_DisableAnnotationIntrospector() throws Exception {
        SimpleRecord record = new SimpleRecord(123, "Bob");

        JsonMapper mapper = JsonMapper.builder()
                .configure(MapperFeature.USE_ANNOTATIONS, false)
                .build();
        String json = mapper.writeValueAsString(record);

        assertEquals("{\"id\":123,\"name\":\"Bob\"}", json);
    }

    @Test
    public void testDeserializeSimpleRecord_DisableAnnotationIntrospector() throws Exception {
        JsonMapper mapper = JsonMapper.builder()
                .configure(MapperFeature.USE_ANNOTATIONS, false)
                .build();
        SimpleRecord value = mapper.readValue("{\"id\":123,\"name\":\"Bob\"}", SimpleRecord.class);

        assertEquals(new SimpleRecord(123, "Bob"), value);
    }

    /*
    /**********************************************************************
    /* Test methods, renames, injects
    /**********************************************************************
     */

    @Test
    public void testSerializeJsonRename() throws Exception {
        String json = MAPPER.writeValueAsString(new RecordWithRename(123, "Bob"));
        final Object EXP = map("id", Integer.valueOf(123), "rename", "Bob");
        assertEquals(EXP, MAPPER.readValue(json, Object.class));
    }

    @Test
    public void testDeserializeJsonRename() throws Exception {
        RecordWithRename value = MAPPER.readValue("{\"id\":123,\"rename\":\"Bob\"}",
                RecordWithRename.class);
        assertEquals(new RecordWithRename(123, "Bob"), value);
    }

    // Confirmation of fix of [databind#4218]
    @Test
    public void testDeserializeHeaderInjectRecord4218() throws Exception {
        ObjectReader reader = MAPPER.readerFor(RecordWithHeaderInject.class)
                .with(new InjectableValues.Std().addValue(String.class, "Bob"));
        assertNotNull(reader.readValue("{\"id\":123}"));
    }

    @Test
    public void testDeserializeConstructorInjectRecord4218() throws Exception {
        ObjectReader reader = MAPPER.readerFor(RecordWithConstructorInject.class)
                .with(new InjectableValues.Std().addValue(String.class, "Bob"));
        RecordWithConstructorInject value = reader.readValue("{\"id\":123}");
        assertEquals(new RecordWithConstructorInject(123, "Bob"), value);
    }

    /*
    /**********************************************************************
    /* Test methods, naming strategy
    /**********************************************************************
     */

    // [databind#2992]
    @Test
    public void testNamingStrategy() throws Exception
    {
        SnakeRecord input = new SnakeRecord("123", "value");

        String json = MAPPER.writeValueAsString(input);
        assertEquals("{\"my_id\":\"123\",\"my_value\":\"value\"}", json);

        SnakeRecord output = MAPPER.readValue(json, SnakeRecord.class);
        assertEquals(input, output);
    }

    /*
    /**********************************************************************
    /* Test methods, JsonDeserialize
    /**********************************************************************
     */

    @Test
    public void testDeserializeJsonDeserializeRecord() throws Exception {
        RecordWithJsonDeserialize value = MAPPER.readValue("{\"id\":123,\"name\":\"   Bob   \"}", RecordWithJsonDeserialize.class);

        assertEquals(new RecordWithJsonDeserialize(123, "Bob"), value);
    }

    /*
    /**********************************************************************
    /* Test methods, JsonProperty(access=WRITE_ONLY)
    /**********************************************************************
     */

    @Test
    public void testSerialize_SingleWriteOnlyParameter() throws Exception {
        String json = MAPPER.writeValueAsString(new RecordSingleWriteOnly(123));

        assertEquals("{}", json);
    }

    // [databind#3897]
    @Test
    public void testDeserialize_SingleWriteOnlyParameter() throws Exception {
        RecordSingleWriteOnly value = MAPPER.readValue("{\"id\":123}", RecordSingleWriteOnly.class);

        assertEquals(new RecordSingleWriteOnly(123), value);
    }

    @Test
    public void testSerialize_SomeWriteOnlyParameter() throws Exception {
        String json = MAPPER.writeValueAsString(new RecordSomeWriteOnly(123, "Bob", "bob@example.com"));

        assertEquals("{\"email\":\"bob@example.com\"}", json);
    }

    @Test
    public void testDeserialize_SomeWriteOnlyParameter() throws Exception {
        RecordSomeWriteOnly value = MAPPER.readValue(
                "{\"id\":123,\"name\":\"Bob\",\"email\":\"bob@example.com\"}",
                RecordSomeWriteOnly.class);

        assertEquals(new RecordSomeWriteOnly(123, "Bob", "bob@example.com"), value);
    }

    @Test
    public void testSerialize_AllWriteOnlyParameter() throws Exception {
        String json = MAPPER.writeValueAsString(new RecordAllWriteOnly(123, "Bob", "bob@example.com"));

        assertEquals("{}", json);
    }

    @Test
    public void testDeserialize_AllWriteOnlyParameter() throws Exception {
        RecordAllWriteOnly value = MAPPER.readValue(
                "{\"id\":123,\"name\":\"Bob\",\"email\":\"bob@example.com\"}",
                RecordAllWriteOnly.class);

        assertEquals(new RecordAllWriteOnly(123, "Bob", "bob@example.com"), value);
    }

    /*
    /**********************************************************************
    /* Test method(s), MapperFeature.REQUIRE_SETTERS_FOR_GETTERS
    /**********************************************************************
     */

    // [databind#4678]
    @Test
    public void testSerializeWithSettersForGetters() throws Exception {
        ObjectMapper mapperWithSetters = JsonMapper.builder()
                .configure(MapperFeature.REQUIRE_SETTERS_FOR_GETTERS, true)
                .build();
        var input = new SimpleRecord(123, "Bob");
        assertEquals(MAPPER.writeValueAsString(input),
                mapperWithSetters.writeValueAsString(input));
    }

    /*
    /**********************************************************************
    /* Test methods, private records [databind#4175]
    /**********************************************************************
     */

    // [databind#4175]
    @Test
    public void testSerializePrivateTextRecord() throws Exception {
        PrivateTextRecord4175 textRecord = new PrivateTextRecord4175("anything");
        String json = MAPPER.writeValueAsString(textRecord);
        final Object EXP = Collections.singletonMap("text", "anything");
        assertEquals(EXP, MAPPER.readValue(json, Object.class));
    }

    // [databind#4175]
    @Test
    public void testDeserializePrivateTextRecord() throws Exception {
        assertEquals(new PrivateTextRecord4175("anything"),
                MAPPER.readValue("{\"text\":\"anything\"}", PrivateTextRecord4175.class));
    }

    /*
    /**********************************************************************
    /* Test methods, renamed-property regression [databind#5967]
    /**********************************************************************
     */

    // [databind#5967] Records take a different path in
    // POJOPropertiesCollector#_renameProperties (always skipped by isRecordType()
    // when the property name is in _ignoredPropertyNames), so the field-stripping
    // fix added for non-records must not regress record renaming.
    @Test
    public void recordWithRenamedPropertyRoundTrips() throws Exception
    {
        RenamedRecord5967 original = new RenamedRecord5967("someValue");
        String json = MAPPER.writeValueAsString(original);
        assertEquals("{\"renamedProp\":\"someValue\"}", json);

        RenamedRecord5967 result = MAPPER.readValue(json, RenamedRecord5967.class);
        assertEquals("someValue", result.prop());
    }

    /*
    /**********************************************************************
    /* Test methods, custom @JsonSerialize/@JsonDeserialize [jackson#188]
    /**********************************************************************
     */

    // [jackson#188]
    @Test
    void testJsonSerializeOnRecord() throws Exception
    {
        Animal188 input = new Animal188("dog", 3);
        String JSON = MAPPER.writeValueAsString(input);
        assertEquals(a2q("{'name':'custom dog','age':3}"), JSON);
    }

    // [jackson#188]
    @Test
    void testJsonDeserializeOnRecord() throws Exception
    {
        String JSON = a2q("{'name':'cat','age':4}");
        Animal188 result = MAPPER.readValue(JSON, Animal188.class);
        assertEquals("custom-desercat", result.name());
        assertEquals(4, result.age());
    }

    /*
    /**********************************************************************
    /* Internal helper methods
    /**********************************************************************
     */

    private Map<String,Object> map(String key1, Object value1,
            String key2, Object value2) {
        final Map<String, Object> result = new LinkedHashMap<>();
        result.put(key1, value1);
        result.put(key2, value2);
        return result;
    }

    public static class StringTrimmer implements Converter<String, String>
    {
        @Override
        public String convert(DeserializationContext ctxt, String value) {
            return _convert(value);
        }

        @Override
        public String convert(SerializationContext ctxt, String value) {
            return _convert(value);
        }

        String _convert(String value) {
            return value.trim();
        }

        @Override
        public JavaType getInputType(TypeFactory typeFactory) {
            return typeFactory.constructType(String.class);
        }

        @Override
        public JavaType getOutputType(TypeFactory typeFactory) {
            return typeFactory.constructType(String.class);
        }
    }
}
