package tools.jackson.databind.records;

import java.util.*;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class RecordWithJsonIgnoreTest extends DatabindTestUtil
{
    record RecordWithIgnore(int id, @JsonIgnore String name) {
    }

    record RecordWithIgnoreJsonProperty(int id, @JsonIgnore @JsonProperty("name") String name) {
    }

    record RecordWithIgnoreJsonPropertyDifferentName(int id, @JsonIgnore @JsonProperty("name2") String name) {
    }

    record RecordWithIgnoreAccessor(int id, String name) {
        @JsonIgnore
        @Override
        public String name() {
            return name;
        }
    }

    record RecordWithIgnorePrimitiveType(@JsonIgnore int id, String name) {
    }

    // [databind#3992]
    public record HelloRecord(String text, @JsonIgnore Recursion hidden) {
        // Before fix: works if this override is removed
        // After fix: works either way
        @Override
        public Recursion hidden() {
            return hidden;
        }
    }

    static class Recursion {
        public List<Recursion> all = new ArrayList<>();

        void add(Recursion recursion) {
            all.add(recursion);
        }
    }

    // [databind#5184]
    record TestData5184(@JsonProperty("test_property") String value) {
        @JsonIgnore
        public Optional<String> getValue() {
            return Optional.ofNullable(value);
        }
    }

    record TestData5184Alternate(@JsonProperty("test_property") String value) {
        @JsonIgnore
        public Optional<String> optionalValue() {
            return Optional.ofNullable(value);
        }
    }

    static final class TestData5184Class {
        private final String value;

        public TestData5184Class(@JsonProperty("test_property") String value) {
            this.value = value;
        }

        @JsonIgnore
        public Optional<String> getValue() {
            return Optional.ofNullable(value);
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    /*
    /**********************************************************************
    /* Test methods, @JsonIgnore on constructor parameter
    /**********************************************************************
     */

    @Test
    public void testSerializeJsonIgnoreRecord() throws Exception {
        String json = MAPPER.writeValueAsString(new RecordWithIgnore(123, "Bob"));
        assertEquals("{\"id\":123}", json);
    }

    @Test
    public void testDeserializeJsonIgnoreRecord() throws Exception {
        RecordWithIgnore value = MAPPER.readValue("{\"id\":123,\"name\":\"Bob\"}",
                RecordWithIgnore.class);
        assertEquals(new RecordWithIgnore(123, null), value);
    }

    /*
    /**********************************************************************
    /* Test methods, @JsonIgnore + @JsonProperty
    /**********************************************************************
     */

    @Test
    public void testSerializeJsonIgnoreAndJsonPropertyRecord() throws Exception {
        String json = MAPPER.writeValueAsString(new RecordWithIgnoreJsonProperty(123, "Bob"));
        assertEquals("{\"id\":123}", json);
    }

    @Test
    public void testDeserializeJsonIgnoreAndJsonPropertyRecord() throws Exception {
        RecordWithIgnoreJsonProperty value = MAPPER.readValue("{\"id\":123,\"name\":\"Bob\"}",
                RecordWithIgnoreJsonProperty.class);
        assertEquals(new RecordWithIgnoreJsonProperty(123, null), value);
    }

    @Test
    public void testDeserializeJsonIgnoreRecordWithDifferentName() throws Exception {
        RecordWithIgnoreJsonPropertyDifferentName value = MAPPER.readValue("{\"id\":123,\"name\":\"Bob\"}",
                RecordWithIgnoreJsonPropertyDifferentName.class);
        assertEquals(new RecordWithIgnoreJsonPropertyDifferentName(123, null), value);
    }

    /*
    /**********************************************************************
    /* Test methods, @JsonIgnore on accessor
    /**********************************************************************
     */

    @Test
    public void testSerializeJsonIgnoreAccessorRecord() throws Exception {
        assertEquals("{\"id\":123}",
                MAPPER.writeValueAsString(new RecordWithIgnoreAccessor(123, "Bob")));
    }

    // [databind#4628]
    @Test
    public void testDeserializeJsonIgnoreAccessorRecord() throws Exception {
        RecordWithIgnoreAccessor expected = new RecordWithIgnoreAccessor(123, null);

        assertEquals(expected, MAPPER.readValue("{\"id\":123}", RecordWithIgnoreAccessor.class));
        assertEquals(expected, MAPPER.readValue("{\"id\":123,\"name\":null}", RecordWithIgnoreAccessor.class));
        assertEquals(expected, MAPPER.readValue("{\"id\":123,\"name\":\"Bob\"}", RecordWithIgnoreAccessor.class));
    }

    /*
    /**********************************************************************
    /* Test methods, @JsonIgnore on primitive-type parameter
    /**********************************************************************
     */

    @Test
    public void testSerializeJsonIgnorePrimitiveTypeRecord() throws Exception {
        String json = MAPPER.writeValueAsString(new RecordWithIgnorePrimitiveType(123, "Bob"));
        assertEquals("{\"name\":\"Bob\"}", json);
    }

    @Test
    public void testDeserializeJsonIgnorePrimitiveTypeRecord() throws Exception {
        RecordWithIgnorePrimitiveType value = jsonMapperBuilder()
                .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
                .build()
                .readValue("{\"id\":123,\"name\":\"Bob\"}", RecordWithIgnorePrimitiveType.class);
        assertEquals(new RecordWithIgnorePrimitiveType(0, "Bob"), value);
    }

    /*
    /**********************************************************************
    /* Test methods, @JsonIgnore with overridden accessor [databind#3992]
    /**********************************************************************
     */

    // [databind#3992]
    @Test
    public void testJsonIgnoreWithOverriddenAccessor3992() throws Exception {
        Recursion beanWithRecursion = new Recursion();
        beanWithRecursion.add(beanWithRecursion);
        String json = MAPPER.writer()
                .writeValueAsString(new HelloRecord("hello", beanWithRecursion));
        assertEquals(a2q("{'text':'hello'}"), json);

        HelloRecord result = MAPPER.readValue(json, HelloRecord.class);
        assertNotNull(result);
    }

    // [databind#4626]
    @Test
    public void testDeserializeJsonIgnoreWithOverride4626() throws Exception {
        HelloRecord expected = new HelloRecord("hello", null);

        assertEquals(expected, MAPPER.readValue(a2q("{'text':'hello'}"), HelloRecord.class));
        assertEquals(expected, MAPPER.readValue(a2q("{'text':'hello','hidden':null}"), HelloRecord.class));
        assertEquals(expected, MAPPER.readValue(a2q("{'text':'hello','hidden':{'all': []}}"), HelloRecord.class));
    }

    /*
    /**********************************************************************
    /* Test methods, @JsonIgnore on non-component getter [databind#5184]
    /**********************************************************************
     */

    // [databind#5184]
    @Test
    void testDeserializeWithIgnoredNonComponentGetter5184() throws Exception {
        String json = "{\"test_property\":\"test value\"}";

        var testData = MAPPER.readValue(json, TestData5184.class);

        assertThat(testData.value()).isEqualTo("test value");
    }

    @Test
    void testDeserializeWithIgnoredNonComponentGetterOnClass5184() throws Exception {
        String json = "{\"test_property\":\"test value\"}";

        var testData = MAPPER.readValue(json, TestData5184Class.class);

        assertThat(testData.getValue()).contains("test value");
    }

    @Test
    void testDeserializeWithIgnoredAlternateNonComponentGetter5184() throws Exception {
        String json = "{\"test_property\":\"test value\"}";

        var testData = MAPPER.readValue(json, TestData5184Alternate.class);

        assertThat(testData.value()).isEqualTo("test value");
    }

    @Test
    void testDeserializeIgnoresWrongPropertyName5184() throws Exception {
        String json = "{\"value\":\"test value\"}";

        TestData5184 testData = MAPPER.readValue(json, TestData5184.class);

        assertThat(testData.value()).isNull();
    }
}
