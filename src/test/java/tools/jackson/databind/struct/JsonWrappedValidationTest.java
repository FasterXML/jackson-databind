package tools.jackson.databind.struct;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonWrapped;
import tools.jackson.databind.exc.InvalidDefinitionException;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link JsonWrapped} validation and error cases.
 */
public class JsonWrappedValidationTest extends DatabindTestUtil
{
    private final ObjectMapper MAPPER = JsonMapper.builder().build();

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
        @DisplayName("should throw InvalidDefinitionException when wrapper name is empty")
        void emptyWrapperName() {
            // setup
            EmptyName bean = new EmptyName();
            bean.x = "test";

            // when/then
            assertThatThrownBy(() -> MAPPER.writeValueAsString(bean))
                .isInstanceOf(InvalidDefinitionException.class)
                .hasMessageContaining("must not be empty");
        }

        @Test
        @DisplayName("should throw InvalidDefinitionException when property is a List")
        void nonScalarList() {
            // setup
            NonScalarList bean = new NonScalarList();

            // when/then
            assertThatThrownBy(() -> MAPPER.writeValueAsString(bean))
                .isInstanceOf(InvalidDefinitionException.class)
                .hasMessageContaining("only supported on scalar");
        }

        @Test
        @DisplayName("should throw InvalidDefinitionException when property is a Map")
        void nonScalarMap() {
            // setup
            NonScalarMap bean = new NonScalarMap();

            // when/then
            assertThatThrownBy(() -> MAPPER.writeValueAsString(bean))
                .isInstanceOf(InvalidDefinitionException.class)
                .hasMessageContaining("only supported on scalar");
        }

        @Test
        @DisplayName("should throw InvalidDefinitionException when property is an array")
        void nonScalarArray() {
            // setup
            NonScalarArray bean = new NonScalarArray();

            // when/then
            assertThatThrownBy(() -> MAPPER.writeValueAsString(bean))
                .isInstanceOf(InvalidDefinitionException.class)
                .hasMessageContaining("only supported on scalar");
        }

        @Test
        @DisplayName("should throw InvalidDefinitionException when property is a POJO")
        void nonScalarPojo() {
            // setup
            NonScalarPojo bean = new NonScalarPojo();
            bean.address = new Address();

            // when/then
            assertThatThrownBy(() -> MAPPER.writeValueAsString(bean))
                .isInstanceOf(InvalidDefinitionException.class)
                .hasMessageContaining("only supported on scalar");
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
        @DisplayName("should throw InvalidDefinitionException when property is a POJO during deserialization")
        void nonScalarPojoDeser() {
            // setup
            String json = "{\"w\":{\"address\":{\"street\":\"Main St\",\"city\":\"Springfield\"}}}";

            // when/then
            assertThatThrownBy(() -> MAPPER.readValue(json, NonScalarPojo.class))
                .isInstanceOf(InvalidDefinitionException.class)
                .hasMessageContaining("only supported on scalar");
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
