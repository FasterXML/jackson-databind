package tools.jackson.databind.struct;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.*;
import com.fasterxml.jackson.annotation.JsonWrapped;
import tools.jackson.databind.exc.InvalidDefinitionException;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.testutil.DatabindTestUtil;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link JsonWrapped} validation and error cases.
 */
public class JsonWrappedValidationTest extends DatabindTestUtil
{
    private final ObjectMapper MAPPER = newJsonMapper();

    // -- Inner classes for test beans --

    static class EmptyName {
        @JsonWrapped("")
        public String x;
    }

    static class NonScalarList {
        @JsonWrapped("w")
        public List<String> items;
    }

    static class NonScalarMap {
        @JsonWrapped("w")
        public Map<String, Object> data;
    }

    static class NonScalarArray {
        @JsonWrapped("w")
        public String[] items;
    }

    static class Address {
        public String street;
        public String city;
    }

    static class NonScalarPojo {
        @JsonWrapped("w")
        public Address address;
    }

    static class NameConflict {
        public String name;

        @JsonWrapped("name")
        public String x;
    }

    static class CreatorParam {
        public String name;

        @JsonCreator
        public CreatorParam(@JsonWrapped("w") @JsonProperty("x") String x) {
            this.name = x;
        }
    }

    static class Inner {
        public String street;
    }

    static class MixedWrappedAndUnwrapped {
        @JsonUnwrapped
        public Inner inner;

        @JsonWrapped("w")
        public String value;
    }

    // -- Tests --

    @Nested
    @DisplayName("serialization validation")
    class SerializationValidationTests {

        @Test
        @DisplayName("should treat @JsonWrapped(\"\") as disabled (property appears at top level)")
        void emptyWrapperNameIsDisabled() throws Exception {
            // setup
            EmptyName bean = new EmptyName();
            bean.x = "test";

            // when/then: no exception; wrapping is disabled — property appears at top level
            String json = MAPPER.writeValueAsString(bean);
            assertThat(json).isEqualTo("{\"x\":\"test\"}");
            // deserialization also works flat
            EmptyName result = MAPPER.readValue("{\"x\":\"round-trip\"}", EmptyName.class);
            assertThat(result.x).isEqualTo("round-trip");
        }

        @Test
        @DisplayName("should wrap List field under wrapper name")
        void nonScalarList() throws Exception {
            // setup
            NonScalarList bean = new NonScalarList();
            bean.items = java.util.Arrays.asList("a", "b");

            // when
            String json = MAPPER.writeValueAsString(bean);

            // then
            assertThat(json).isEqualTo("{\"w\":{\"items\":[\"a\",\"b\"]}}");
        }

        @Test
        @DisplayName("should wrap Map field under wrapper name")
        void nonScalarMap() throws Exception {
            // setup
            NonScalarMap bean = new NonScalarMap();
            bean.data = new java.util.LinkedHashMap<>();
            bean.data.put("k", "v");

            // when
            String json = MAPPER.writeValueAsString(bean);

            // then
            assertThat(json).isEqualTo("{\"w\":{\"data\":{\"k\":\"v\"}}}");
        }

        @Test
        @DisplayName("should wrap array field under wrapper name")
        void nonScalarArray() throws Exception {
            // setup
            NonScalarArray bean = new NonScalarArray();
            bean.items = new String[]{"x", "y"};

            // when
            String json = MAPPER.writeValueAsString(bean);

            // then
            assertThat(json).isEqualTo("{\"w\":{\"items\":[\"x\",\"y\"]}}");
        }

        @Test
        @DisplayName("should wrap POJO field under wrapper name")
        void nonScalarPojo() throws Exception {
            // setup
            NonScalarPojo bean = new NonScalarPojo();
            bean.address = new Address();
            bean.address.street = "Main St";
            bean.address.city = "NYC";

            // when
            String json = MAPPER.writeValueAsString(bean);

            // then
            // Field order may vary, so check contains rather than exact match
            assertThat(json).contains("\"w\":{\"address\":");
            assertThat(json).contains("\"street\":\"Main St\"");
            assertThat(json).contains("\"city\":\"NYC\"");
        }

        @Test
        @DisplayName("should throw InvalidDefinitionException when @JsonWrapped and @JsonUnwrapped are both present on the same bean")
        void mixedWrappedAndUnwrapped() {
            // setup
            MixedWrappedAndUnwrapped bean = new MixedWrappedAndUnwrapped();
            bean.inner = new Inner();
            bean.inner.street = "Main St";
            bean.value = "x";

            // when/then
            assertThatThrownBy(() -> MAPPER.writeValueAsString(bean))
                .isInstanceOf(InvalidDefinitionException.class)
                .hasMessageContaining("@JsonWrapped")
                .hasMessageContaining("@JsonUnwrapped");
        }

        @Test
        @DisplayName("should throw InvalidDefinitionException when wrapper name conflicts with existing property")
        void nameConflict() {
            // setup
            NameConflict bean = new NameConflict();
            bean.name = "test";
            bean.x = "value";

            // when/then
            assertThatThrownBy(() -> MAPPER.writeValueAsString(bean))
                .isInstanceOf(InvalidDefinitionException.class)
                .hasMessageContaining("conflicts with");
        }
    }

    @Nested
    @DisplayName("deserialization validation")
    class DeserializationValidationTests {

        @Test
        @DisplayName("should deserialize POJO field from wrapper object")
        void nonScalarPojoDeser() throws Exception {
            // setup
            String json = "{\"w\":{\"address\":{\"street\":\"Main St\",\"city\":\"Springfield\"}}}";

            // when
            NonScalarPojo bean = MAPPER.readValue(json, NonScalarPojo.class);

            // then
            assertThat(bean.address).isNotNull();
            assertThat(bean.address.street).isEqualTo("Main St");
            assertThat(bean.address.city).isEqualTo("Springfield");
        }

        @Test
        @DisplayName("should throw InvalidDefinitionException when @JsonWrapped used on creator parameter")
        void creatorParam() {
            // setup
            String json = "{\"w\":{\"x\":\"test\"}}";

            // when/then
            assertThatThrownBy(() -> MAPPER.readValue(json, CreatorParam.class))
                .isInstanceOf(InvalidDefinitionException.class)
                .hasMessageContaining("creator parameter");
        }

        @Test
        @DisplayName("should throw InvalidDefinitionException when @JsonWrapped and @JsonUnwrapped are both present on the same bean")
        void mixedWrappedAndUnwrapped() {
            // setup
            String json = "{\"street\":\"Main St\",\"w\":{\"value\":\"x\"}}";

            // when/then
            assertThatThrownBy(() -> MAPPER.readValue(json, MixedWrappedAndUnwrapped.class))
                .isInstanceOf(InvalidDefinitionException.class)
                .hasMessageContaining("@JsonWrapped")
                .hasMessageContaining("@JsonUnwrapped");
        }

        @Test
        @DisplayName("should throw MismatchedInputException when wrapper value is a number instead of object")
        void nonObjectWrapperNumber() {
            // setup
            String json = "{\"symbol\":\"TP53\",\"chr\":123}";

            // when/then
            assertThatThrownBy(() -> MAPPER.readValue(json, JsonWrappedDeserializationTest.Gene.class))
                .isInstanceOf(MismatchedInputException.class)
                .hasMessageContaining("Expected JSON Object");
        }

        @Test
        @DisplayName("should throw MismatchedInputException when wrapper value is an array instead of object")
        void nonObjectWrapperArray() {
            // setup
            String json = "{\"symbol\":\"TP53\",\"chr\":[1,2]}";

            // when/then
            assertThatThrownBy(() -> MAPPER.readValue(json, JsonWrappedDeserializationTest.Gene.class))
                .isInstanceOf(MismatchedInputException.class)
                .hasMessageContaining("Expected JSON Object");
        }
    }
}
