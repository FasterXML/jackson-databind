package tools.jackson.databind.deser;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import tools.jackson.databind.*;
import tools.jackson.databind.exc.UnrecognizedPropertyException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class UnwrappedWithUnknown650Test extends DatabindTestUtil
{
    static class A {
        @JsonUnwrapped
        public B b;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class AWithUnknownsOk {
        @JsonUnwrapped
        public B b;
    }

    static class B {
        public String field;
    }

    // For prefix/suffix
    static class AWithPrefix {
        @JsonUnwrapped(prefix = "nested.")
        public B b;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class AWithPrefixUnknownsOk {
        @JsonUnwrapped(prefix = "nested.")
        public B b;
    }

    // For @JsonCreator + @JsonUnwrapped
    static class AWithCreator {
        public String name;

        @JsonUnwrapped
        public B b;

        @JsonCreator
        public AWithCreator(@JsonProperty("name") String name) {
            this.name = name;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class AWithCreatorUnknownsOk {
        public String name;

        @JsonUnwrapped
        public B b;

        @JsonCreator
        public AWithCreatorUnknownsOk(@JsonProperty("name") String name) {
            this.name = name;
        }
    }

    // For @JsonCreator + @JsonUnwrapped with prefix
    static class AWithCreatorAndPrefix {
        public String name;

        @JsonUnwrapped(prefix = "nested.")
        public B b;

        @JsonCreator
        public AWithCreatorAndPrefix(@JsonProperty("name") String name) {
            this.name = name;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class AWithCreatorAndPrefixUnknownsOk {
        public String name;

        @JsonUnwrapped(prefix = "nested.")
        public B b;

        @JsonCreator
        public AWithCreatorAndPrefixUnknownsOk(@JsonProperty("name") String name) {
            this.name = name;
        }
    }

    private final ObjectMapper MAPPER = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    @Test
    void failOnUnknownPropertyUnwrapped() throws Exception {
        final String json = a2q("{'field': 'value', 'bad': 'bad value'}");
        try {
            MAPPER.readValue(json, A.class);
            fail("Exception was not thrown on unknown property");
        } catch (UnrecognizedPropertyException e) {
            verifyException(e, "Unrecognized property");
        }
    }

    @Test
    void workOnUnknownWithAnnotation() throws Exception {
        final String json = a2q("{'field': 'value', 'bad': 'bad value'}");
        AWithUnknownsOk a = MAPPER.readValue(json, AWithUnknownsOk.class);
        assertEquals("value", a.b.field);
    }

    // Passing case, regular usage
    @Test
    void worksOnRegularPropertyUnwrapped() throws Exception {
        A value = MAPPER.readValue(a2q("{'field': 'value'}"), A.class);
        assertEquals("value", value.b.field);
    }

    // Tests for @JsonUnwrapped with prefix
    @Test
    void failOnUnknownPropertyUnwrappedWithPrefix() throws Exception {
        final String json = a2q("{'nested.field': 'value', 'bad': 'bad value'}");
        try {
            MAPPER.readValue(json, AWithPrefix.class);
            fail("Exception was not thrown on unknown property");
        } catch (UnrecognizedPropertyException e) {
            verifyException(e, "Unrecognized property");
        }
    }

    @Test
    void workOnUnknownWithPrefixAndAnnotation() throws Exception {
        final String json = a2q("{'nested.field': 'value', 'bad': 'bad value'}");
        AWithPrefixUnknownsOk a = MAPPER.readValue(json, AWithPrefixUnknownsOk.class);
        assertEquals("value", a.b.field);
    }

    @Test
    void worksOnRegularPropertyUnwrappedWithPrefix() throws Exception {
        AWithPrefix value = MAPPER.readValue(a2q("{'nested.field': 'value'}"), AWithPrefix.class);
        assertEquals("value", value.b.field);
    }

    // Tests for @JsonCreator + @JsonUnwrapped (deserializeUsingPropertyBasedWithUnwrapped)
    @Test
    void failOnUnknownPropertyWithCreator() throws Exception {
        final String json = a2q("{'name': 'test', 'field': 'value', 'bad': 'bad value'}");
        try {
            MAPPER.readValue(json, AWithCreator.class);
            fail("Exception was not thrown on unknown property");
        } catch (UnrecognizedPropertyException e) {
            verifyException(e, "Unrecognized property");
        }
    }

    @Test
    void workOnUnknownWithCreatorAndAnnotation() throws Exception {
        final String json = a2q("{'name': 'test', 'field': 'value', 'bad': 'bad value'}");
        AWithCreatorUnknownsOk a = MAPPER.readValue(json, AWithCreatorUnknownsOk.class);
        assertEquals("test", a.name);
        assertEquals("value", a.b.field);
    }

    @Test
    void worksOnRegularPropertyWithCreator() throws Exception {
        AWithCreator value = MAPPER.readValue(a2q("{'name': 'test', 'field': 'value'}"), AWithCreator.class);
        assertEquals("test", value.name);
        assertEquals("value", value.b.field);
    }

    // Tests for @JsonCreator + @JsonUnwrapped with prefix
    @Test
    void failOnUnknownPropertyWithCreatorAndPrefix() throws Exception {
        final String json = a2q("{'name': 'test', 'nested.field': 'value', 'bad': 'bad value'}");
        try {
            MAPPER.readValue(json, AWithCreatorAndPrefix.class);
            fail("Exception was not thrown on unknown property");
        } catch (UnrecognizedPropertyException e) {
            verifyException(e, "Unrecognized property");
        }
    }

    @Test
    void workOnUnknownWithCreatorAndPrefixAndAnnotation() throws Exception {
        final String json = a2q("{'name': 'test', 'nested.field': 'value', 'bad': 'bad value'}");
        AWithCreatorAndPrefixUnknownsOk a = MAPPER.readValue(json, AWithCreatorAndPrefixUnknownsOk.class);
        assertEquals("test", a.name);
        assertEquals("value", a.b.field);
    }

    @Test
    void worksOnRegularPropertyWithCreatorAndPrefix() throws Exception {
        AWithCreatorAndPrefix value = MAPPER.readValue(
                a2q("{'name': 'test', 'nested.field': 'value'}"), AWithCreatorAndPrefix.class);
        assertEquals("test", value.name);
        assertEquals("value", value.b.field);
    }
}
