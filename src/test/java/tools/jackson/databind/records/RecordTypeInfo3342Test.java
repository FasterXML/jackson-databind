package tools.jackson.databind.records;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.testutil.NoCheckSubTypeValidator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

// [databind#3102]
public class RecordTypeInfo3342Test extends DatabindTestUtil
{
    public enum SpiceLevel {
        LOW,
        HIGH
    }

    public interface SpiceTolerance {
    }

    public record LowSpiceTolerance(String food) implements SpiceTolerance {
    }

    public record HighSpiceTolerance(String food) implements SpiceTolerance {
    }

    public record Example(
            SpiceLevel level,
            @JsonTypeInfo(
                    use = JsonTypeInfo.Id.NAME,
                    include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
                    property = "level")
            @JsonSubTypes({
                    @JsonSubTypes.Type(value = LowSpiceTolerance.class, name = "LOW"),
                    @JsonSubTypes.Type(value = HighSpiceTolerance.class, name = "HIGH")
            })
            SpiceTolerance tolerance) { }

    // Test from https://github.com/FasterXML/jackson-modules-base/pull/249

    static record RootRecord249(AbstractMember249 member) {
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@class")
    @JsonSubTypes({
        @JsonSubTypes.Type(value = StringMember.class, name = "string"),
        @JsonSubTypes.Type(value = IntMember.class, name = "int")
    })
    static abstract class AbstractMember249 { }

    static final class StringMember extends AbstractMember249 {
        final String val;

        @JsonCreator
        public StringMember(@JsonProperty("val") String val) {
          this.val = val;
        }
    }

    static final class IntMember extends AbstractMember249 {
        final int val;

        @JsonCreator
        public IntMember(@JsonProperty("val") int val) {
            this.val = val;
        }
    }

    // [databind#4327]
    @JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
    @JsonSubTypes({
        @JsonSubTypes.Type(value = DeductionBean1_4327.class),
        @JsonSubTypes.Type(value = DeductionBean2_4327.class)
    })
    interface Deduction4327 { }

    record DeductionBean1_4327(int x) implements Deduction4327 { }

    record DeductionBean2_4327(
        @JsonAlias(value = {"Y", "yy", "ff", "X"}) int y
    ) implements Deduction4327 { }

    // [databind#3786]
    record Container3786<T>(
        int id,
        @JsonTypeInfo(
            use = JsonTypeInfo.Id.CLASS,
            include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
            property = "type"
        )
        T value
    ) { }

    record MyObject3786(
        String foo,
        String bar
    ) { }

    private final ObjectMapper MAPPER = newJsonMapper();

    // For [databind#3786]: needs to allow Object as base type for CLASS-based type id
    private final ObjectMapper MAPPER_3786 = jsonMapperBuilder()
            .polymorphicTypeValidator(NoCheckSubTypeValidator.instance)
            .build();

    @Test
    public void testSerializeDeserializeJsonSubType_LOW() throws Exception {
        Example record = new Example(SpiceLevel.LOW, new LowSpiceTolerance("Tomato"));

        String json = MAPPER.writeValueAsString(record);
        assertEquals("{\"level\":\"LOW\",\"tolerance\":{\"food\":\"Tomato\"}}", json);

        Example value = MAPPER.readValue(json, Example.class);
        assertEquals(record, value);
    }

    @Test
    public void testSerializeDeserializeJsonSubType_HIGH() throws Exception {
        Example record = new Example(SpiceLevel.HIGH, new HighSpiceTolerance("Chilli"));

        String json = MAPPER.writeValueAsString(record);
        assertEquals("{\"level\":\"HIGH\",\"tolerance\":{\"food\":\"Chilli\"}}", json);

        Example value = MAPPER.readValue(json, Example.class);
        assertEquals(record, value);
    }

    // Test from https://github.com/FasterXML/jackson-modules-base/pull/249
    @Test
    public void testDeserializeRecordWithAbstractMember() throws Exception {
        RootRecord249 value = MAPPER.readValue(
                "{\"member\":{\"@class\":\"string\",\"val\":\"Hello, abstract member!\"}}",
                RootRecord249.class);
        assertNotNull(value.member());
        assertEquals(StringMember.class, value.member().getClass());
        assertEquals("Hello, abstract member!", ((StringMember)value.member()).val);
      }

    /*
    /**********************************************************************
    /* Test methods, @JsonAlias with polymorphic deduction [databind#4327]
    /**********************************************************************
     */

    // [databind#4327] JsonAlias should respect Polymorphic Deduction
    @ParameterizedTest
    @ValueSource(strings = {"Y", "yy", "ff", "X"})
    public void testAliasWithPolymorphicDeduction(String field) throws Exception {
        String json = a2q("{'%s': 2 }".formatted(field));
        Deduction4327 value = MAPPER.readValue(json, Deduction4327.class);
        assertNotNull(value);
        assertEquals(2, ((DeductionBean2_4327) value).y());
    }

    /*
    /**********************************************************************
    /* Test methods, EXTERNAL_PROPERTY with generic record [databind#3786]
    /**********************************************************************
     */

    // [databind#3786]: Deserialization of generic container (Record type) using
    // EXTERNAL_PROPERTY: custom object has type info written
    @Test
    public void testCustomObjectRoundTrip3786() {
        Container3786<MyObject3786> myContainer = new Container3786<>(1, new MyObject3786("foo", "bar"));
        String json = MAPPER_3786.writeValueAsString(myContainer);

        Container3786<?> result = MAPPER_3786.readValue(json, new TypeReference<Container3786<?>>() { });
        assertNotNull(result);
        assertEquals(1, result.id());
        assertNotNull(result.value());
    }

    // [databind#3786]: String value has no type info written, but deserialization requires it
    @Test
    public void testStringValueRoundTrip3786() {
        Container3786<String> strContainer = new Container3786<>(1, "Hello");
        String json = MAPPER_3786.writeValueAsString(strContainer);

        Container3786<?> result = MAPPER_3786.readValue(json, new TypeReference<Container3786<?>>() { });
        assertNotNull(result);
        assertEquals(1, result.id());
        assertEquals("Hello", result.value());
    }

    // [databind#3786]
    @Test
    public void testIntegerValueRoundTrip3786() {
        Container3786<Integer> intContainer = new Container3786<>(1, 42);
        String json = MAPPER_3786.writeValueAsString(intContainer);

        Container3786<?> result = MAPPER_3786.readValue(json, new TypeReference<Container3786<?>>() { });
        assertNotNull(result);
        assertEquals(1, result.id());
        assertEquals(42, result.value());
    }

    // [databind#3786]
    @Test
    public void testBooleanValueRoundTrip3786() {
        Container3786<Boolean> boolContainer = new Container3786<>(1, true);
        String json = MAPPER_3786.writeValueAsString(boolContainer);

        Container3786<?> result = MAPPER_3786.readValue(json, new TypeReference<Container3786<?>>() { });
        assertNotNull(result);
        assertEquals(1, result.id());
        assertEquals(true, result.value());
    }
}
