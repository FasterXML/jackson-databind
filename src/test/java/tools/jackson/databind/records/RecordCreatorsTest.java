package tools.jackson.databind.records;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import tools.jackson.databind.*;
import tools.jackson.databind.exc.UnrecognizedPropertyException;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

public class RecordCreatorsTest extends DatabindTestUtil
{
    record RecordWithCanonicalCtorOverride(int id, String name) {
        public RecordWithCanonicalCtorOverride(int id, String name) {
            this.id = id;
            this.name = "name";
        }
    }

    record RecordWithAltCtor(int id, String name) {
        @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
        public RecordWithAltCtor(@JsonProperty("id") int id) {
            this(id, "name2");
        }
    }

    // [databind#2980]
    record RecordWithDelegation(String value) {
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public RecordWithDelegation(String value) {
            this.value = "del:"+value;
        }

        @JsonValue
        public String getValue() {
            return "val:"+value;
        }

        public String accessValueForTest() { return value; }
    }

    // [databind#4452]
    public record PlainTestObject(
            @JsonProperty("strField") String testFieldName,
            @JsonProperty("intField") Integer testOtherField
    ) { }

    public record CreatorTestObject(
            String testFieldName,
            Integer testOtherField
    ) {
        @JsonCreator
        public CreatorTestObject(
                @JsonProperty("strField") String testFieldName,
                @JsonProperty("someOtherIntField") Integer testOtherIntField,
                @JsonProperty("intField") Integer testOtherField)
        {
            this(testFieldName, testOtherField + testOtherIntField);
        }
    }

    // [databind#4724]
    public record Something(String value) {
        public Something {
            if (value == null || value.isEmpty()) {
                throw new IllegalArgumentException("Value cannot be null or empty");
            }
        }

        // should be considered Delegating due to @JsonValue later on
        @JsonCreator
        public static Something of(String value) {
            if (value.isEmpty()) {
                return null;
            }
            return new Something(value);
        }

        @Override
        @JsonValue
        public String toString() {
            return value;
        }
    }

    // [databind#3938]
    private final static String ERROR_3938_PREFIX = "Non-null 'options' not allowed for ";

    interface NoOptionsCommand3938 {
        @JsonProperty("options")
        default void setOptions(JsonNode value) {
          if (value.isNull()) {
             return;
          }
          throw new IllegalArgumentException(ERROR_3938_PREFIX+getClass().getName());
        }
    }

    public record Command3938(int id, String filter) implements NoOptionsCommand3938 { }

    // [databind#562]
    record RecordWithAnySetterCtor562(int id,
            Map<String, Integer> additionalProperties) {
        @JsonCreator
        public RecordWithAnySetterCtor562(@JsonProperty("regular") int id,
                @JsonAnySetter Map<String, Integer> additionalProperties
        ) {
            this.id = id;
            this.additionalProperties = additionalProperties;
        }
    }

    // [databind#3439]
    record TestRecord3439(
            @JsonProperty String field,
            @JsonAnySetter Map<String, Object> anySetter
        ) {}

    // [databind#5952]
    record UserRecordWithAnySetter5952(
            String name,
            @JsonIgnore String sensitiveField,
            @JsonAnySetter Map<String, Object> extras
    ) {}

    // [databind#5923]
    public record Inner5923(@JsonProperty(required = true, value = "innerValue") boolean innerValue) {}

    // [databind#5923]
    public record Outer5923(Inner5923 bools) {
        @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
        public static Outer5923 fromJson(@JsonProperty(required = true, value = "renamed") boolean booleanValue) {
            return new Outer5923(new Inner5923(booleanValue));
        }

        @JsonValue
        public Map<String, Boolean> toJson() {
            return Map.of("renamed", bools.innerValue());
        }
    }

    private final ObjectMapper MAPPER = jsonMapperBuilder().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();

    /*
    /**********************************************************************
    /* Test methods, alternate constructors
    /**********************************************************************
     */

    @Test
    public void testDeserializeWithCanonicalCtorOverride() throws Exception {
        RecordWithCanonicalCtorOverride value = MAPPER.readValue("{\"id\":123,\"name\":\"Bob\"}",
                RecordWithCanonicalCtorOverride.class);
        assertEquals(123, value.id());
        assertEquals("name", value.name());
    }

    @Test
    public void testDeserializeWithAltCtor() throws Exception {
        RecordWithAltCtor value = MAPPER.readValue("{\"id\":2812}",
                RecordWithAltCtor.class);
        assertEquals(2812, value.id());
        assertEquals("name2", value.name());

        // "Implicit" canonical constructor can no longer be used when there's explicit constructor
        try {
            MAPPER.readValue("{\"id\":2812,\"name\":\"Bob\"}",
                    RecordWithAltCtor.class);
            fail("should not pass");
        } catch (UnrecognizedPropertyException e) {
            verifyException(e, "Unrecognized");
            verifyException(e, "\"name\"");
            verifyException(e, "RecordWithAltCtor");
        }
    }

    // [databind#2980]
    @Test
    public void testDeserializeWithDelegatingCtor() throws Exception {
        RecordWithDelegation value = MAPPER.readValue(q("foobar"),
                RecordWithDelegation.class);
        assertEquals("del:foobar", value.accessValueForTest());

        assertEquals(q("val:del:foobar"), MAPPER.writeValueAsString(value));
    }

    /*
    /**********************************************************************
    /* Test methods, @JsonProperty on @JsonCreator [databind#4452]
    /**********************************************************************
     */

    // [databind#4452]
    @Test
    public void testCreatorSerializationWithJsonProperty4452Plain() throws Exception
    {
        String result = MAPPER.writeValueAsString(new PlainTestObject("test", 1));
        assertEquals(a2q("{'strField':'test','intField':1}"), result);
    }

    @Test
    public void testCreatorSerializationWithJsonProperty4452WithCreator() throws Exception
    {
        String json = MAPPER.writeValueAsString(new CreatorTestObject("test", 2, 1));

        @SuppressWarnings("unchecked")
        Map<String, Object> asMap = (Map<String, Object>) MAPPER.readValue(json, Map.class);
        assertEquals(new HashSet<>(Arrays.asList("intField", "strField")), asMap.keySet());
    }

    /*
    /**********************************************************************
    /* Test methods, @JsonCreator with @JsonValue [databind#4724]
    /**********************************************************************
     */

    // [databind#4724]
    @Test
    void testDeserializeWithCreatorAndJsonValue4724() throws Exception {
        newJsonMapper().readValue("\"\"", Something.class);
    }

    /*
    /**********************************************************************
    /* Test methods, failing setter from interface [databind#3938]
    /**********************************************************************
     */

    // [databind#3938]: Should detect and use setters too
    @Test
    public void testFailingSetter3938() throws Exception
    {
        final ObjectMapper mapper = newJsonMapper();
        final ObjectReader R = mapper.readerFor(Command3938.class);

        // First, missing value and `null` are fine, as long as we have all fields
        assertNotNull(R.readValue(a2q("{'id':1, 'filter':'abc'}")));
        assertNotNull(R.readValue(a2q("{'id':2, 'filter':'abc', 'options':null}")));

        // But then failure for non-empty Array (f.ex)
        try {
            R.readValue(a2q("{'id':2,'options':[123]}}"));
            fail("Should not pass");
        } catch (DatabindException e) {
            verifyException(e, ERROR_3938_PREFIX);
        }
    }

    /*
    /**********************************************************************
    /* Test methods, @JsonAnySetter on creator [databind#562, #3439, #5952]
    /**********************************************************************
     */

    // [databind#562]: Allow @JsonAnySetter on Creator constructors
    @Test
    public void testRecordWithAnySetterCtor() throws Exception
    {
        final ObjectMapper mapper = newJsonMapper();
        // First, only regular property mapped
        RecordWithAnySetterCtor562 result = mapper.readValue(a2q("{'regular':13}"),
                RecordWithAnySetterCtor562.class);
        assertEquals(13, result.id);
        assertEquals(0, result.additionalProperties.size());

        // Then with unknown properties
        result = mapper.readValue(a2q("{'regular':13, 'unknown':99, 'extra':-1}"),
                RecordWithAnySetterCtor562.class);
        assertEquals(13, result.id);
        assertEquals(Integer.valueOf(99), result.additionalProperties.get("unknown"));
        assertEquals(Integer.valueOf(-1), result.additionalProperties.get("extra"));
        assertEquals(2, result.additionalProperties.size());
    }

    // [databind#3439] Java Record @JsonAnySetter value is null after deserialization
    @Test
    public void testJsonAnySetterOnRecord() throws Exception {
        final ObjectMapper mapper = newJsonMapper();
        String json = """
            {
                "field": "value",
                "unmapped1": "value1",
                "unmapped2": "value2"
            }
            """;

        TestRecord3439 result = mapper.readValue(json, TestRecord3439.class);

        assertEquals("value", result.field());
        assertEquals(Map.of("unmapped1", "value1", "unmapped2", "value2"),
                result.anySetter());
    }

    // [databind#5952]: per-property @JsonIgnore on a record component must block
    // routing to the any-setter
    @Test
    public void testJsonIgnoreOnRecordComponentNotPassedToAnySetter5952() throws Exception {
        final ObjectMapper mapper = newJsonMapper();
        UserRecordWithAnySetter5952 u = mapper.readValue(
                a2q("{'name':'alice','sensitiveField':'secret','other':'val'}"),
                UserRecordWithAnySetter5952.class);
        assertEquals("alice", u.name());
        assertEquals(Map.of("other", "val"), u.extras());
    }

    /*
    /**********************************************************************
    /* Test methods, @JsonCreator(PROPERTIES) + @JsonValue [databind#5923]
    /**********************************************************************
     */

    // [databind#5923]
    @Test
    public void testDeserialization5923True() throws Exception {
        Outer5923 bw = MAPPER.readValue("{ \"renamed\": true }", Outer5923.class);
        assertEquals(true, bw.bools.innerValue);
    }

    // [databind#5923]
    @Test
    public void testDeserialization5923False() throws Exception {
        Outer5923 bw = MAPPER.readValue("{ \"renamed\": false }", Outer5923.class);
        assertEquals(false, bw.bools.innerValue);
    }

    // [databind#5923]
    @Test
    public void testSerialization5923ViaJsonValue() throws Exception {
        assertEquals("{\"renamed\":true}",
                MAPPER.writeValueAsString(new Outer5923(new Inner5923(true))));
        assertEquals("{\"renamed\":false}",
                MAPPER.writeValueAsString(new Outer5923(new Inner5923(false))));
    }

    // [databind#5923]
    @Test
    public void testRoundTrip5923() throws Exception {
        Outer5923 original = new Outer5923(new Inner5923(true));
        Outer5923 roundTripped = MAPPER.readValue(MAPPER.writeValueAsString(original), Outer5923.class);
        assertEquals(original.bools.innerValue, roundTripped.bools.innerValue);
    }
}
