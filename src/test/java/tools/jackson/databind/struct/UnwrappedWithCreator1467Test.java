package tools.jackson.databind.struct;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to verify [databind#1467].
 */
public class UnwrappedWithCreator1467Test extends DatabindTestUtil
{
    static class ExplicitWithoutName {
        private final String _unrelated;
        private final Inner _inner;

        @JsonCreator
        public ExplicitWithoutName(@JsonProperty("unrelated") String unrelated, @JsonUnwrapped Inner inner) {
            _unrelated = unrelated;
            _inner = inner;
        }

        public String getUnrelated() {
            return _unrelated;
        }

        @JsonUnwrapped
        public Inner getInner() {
            return _inner;
        }
    }

    static class ExplicitWithName {
        private final String _unrelated;
        private final Inner _inner;

        @JsonCreator
        public ExplicitWithName(@JsonProperty("unrelated") String unrelated, @JsonProperty("inner") @JsonUnwrapped Inner inner) {
            _unrelated = unrelated;
            _inner = inner;
        }

        public String getUnrelated() {
            return _unrelated;
        }

        public Inner getInner() {
            return _inner;
        }
    }

    static class ImplicitWithName {
        private final String _unrelated;
        private final Inner _inner;

        public ImplicitWithName(@JsonProperty("unrelated") String unrelated, @JsonProperty("inner") @JsonUnwrapped Inner inner) {
            _unrelated = unrelated;
            _inner = inner;
        }

        public String getUnrelated() {
            return _unrelated;
        }

        public Inner getInner() {
            return _inner;
        }
    }

    static class WithTwoUnwrappedProperties {
        private final String _unrelated;
        private final Inner _inner1;
        private final Inner _inner2;

        public WithTwoUnwrappedProperties(
                @JsonProperty("unrelated") String unrelated,
                @JsonUnwrapped(prefix = "first-") Inner inner1,
                @JsonUnwrapped(prefix = "second-") Inner inner2
        ) {
            _unrelated = unrelated;
            _inner1 = inner1;
            _inner2 = inner2;
        }

        public String getUnrelated() {
            return _unrelated;
        }

        @JsonUnwrapped(prefix = "first-")
        public Inner getInner1() {
            return _inner1;
        }

        @JsonUnwrapped(prefix = "second-")
        public Inner getInner2() {
            return _inner2;
        }
    }

    static class Inner {
        private final String _property1;
        private final String _property2;

        public Inner(@JsonProperty("property1") String property1, @JsonProperty("property2") String property2) {
            _property1 = property1;
            _property2 = property2;
        }

        public String getProperty1() {
            return _property1;
        }

        public String getProperty2() {
            return _property2;
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testUnwrappedWithJsonCreatorWithExplicitWithoutName() throws Exception
    {
        String json = "{\"unrelated\": \"unrelatedValue\", \"property1\": \"value1\", \"property2\": \"value2\"}";
        ExplicitWithoutName outer = MAPPER.readValue(json, ExplicitWithoutName.class);

        assertEquals("unrelatedValue", outer.getUnrelated());
        assertEquals("value1", outer.getInner().getProperty1());
        assertEquals("value2", outer.getInner().getProperty2());
    }

    @Test
    public void testUnwrappedWithJsonCreatorExplicitWithName() throws Exception
    {
        String json = "{\"unrelated\": \"unrelatedValue\", \"property1\": \"value1\", \"property2\": \"value2\"}";
        ExplicitWithName outer = MAPPER.readValue(json, ExplicitWithName.class);

        assertEquals("unrelatedValue", outer.getUnrelated());
        assertEquals("value1", outer.getInner().getProperty1());
        assertEquals("value2", outer.getInner().getProperty2());
    }

    @Test
    public void testUnwrappedWithJsonCreatorImplicitWithName() throws Exception
    {
        String json = "{\"unrelated\": \"unrelatedValue\", \"property1\": \"value1\", \"property2\": \"value2\"}";
        ImplicitWithName outer = MAPPER.readValue(json, ImplicitWithName.class);

        assertEquals("unrelatedValue", outer.getUnrelated());
        assertEquals("value1", outer.getInner().getProperty1());
        assertEquals("value2", outer.getInner().getProperty2());
    }

    @Test
    public void testUnwrappedWithTwoUnwrappedProperties() throws Exception
    {
        String json = "{\"unrelated\": \"unrelatedValue\", " +
                "\"first-property1\": \"first-value1\", \"first-property2\": \"first-value2\", " +
                "\"second-property1\": \"second-value1\", \"second-property2\": \"second-value2\"}";
        WithTwoUnwrappedProperties outer = MAPPER.readValue(json, WithTwoUnwrappedProperties.class);

        assertEquals("unrelatedValue", outer.getUnrelated());
        assertEquals("first-value1", outer.getInner1().getProperty1());
        assertEquals("first-value2", outer.getInner1().getProperty2());
        assertEquals("second-value1", outer.getInner2().getProperty1());
        assertEquals("second-value2", outer.getInner2().getProperty2());
    }
}
