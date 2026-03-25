package tools.jackson.databind.records;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
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
}
