package tools.jackson.databind.deser.filter;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;

import tools.jackson.databind.exc.InvalidDefinitionException;

import static org.junit.jupiter.api.Assertions.*;

// [databind#2572]: "empty" setter, POJO with no 0-arg constructor
//   (only has @JsonCreator with args)
class NullConversionsAsEmptyPOJO2572Test extends DatabindTestUtil {
    static class Outer {
        @JsonProperty("inner")
        private final Inner inner;

        @JsonCreator
        public Outer(@JsonProperty("inner") Inner inner) {
            this.inner = inner;
        }
    }

    static class Inner {
        @JsonProperty("field")
        private final String field;

        @JsonCreator
        public Inner(@JsonProperty("field") String field) {
            this.field = field;
        }
    }

    // [databind#2572]: round-trip with non-null values should preserve data
    @Test
    void roundTripWithNonNullValues() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder()
                .changeDefaultNullHandling(h -> h.withOverrides(JsonSetter.Value.construct(Nulls.AS_EMPTY, Nulls.AS_EMPTY)))
                .build();
        final String json = mapper.writeValueAsString(new Outer(new Inner("inner")));
        Outer result = mapper.readValue(json, Outer.class);
        assertNotNull(result);
        assertNotNull(result.inner);
        assertEquals("inner", result.inner.field);
    }

    // [databind#2572]: null POJO value should become "empty" instance (created
    //   via @JsonCreator with null args) instead of throwing
    @Test
    void nullPOJOBecomesEmptyInstance() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder()
                .changeDefaultNullHandling(h -> h.withOverrides(JsonSetter.Value.construct(Nulls.AS_EMPTY, Nulls.AS_EMPTY)))
                .build();
        Outer result = mapper.readValue(a2q("{'inner':null}"), Outer.class);
        assertNotNull(result);
        // inner should be an "empty" Inner (created with null field), not null
        assertNotNull(result.inner);
        assertNull(result.inner.field);
    }

    // [databind#2572]: POJO with only non-public args-taking constructor (no
    //   @JsonCreator, no default ctor) cannot produce empty instance: verify
    //   that proper exception is thrown
    static class InnerNonPublicCtor {
        @JsonProperty("field")
        private final String field;

        private InnerNonPublicCtor(String field) {
            this.field = field;
        }
    }

    static class OuterWithNonPublicInner {
        @JsonProperty("inner")
        private final InnerNonPublicCtor inner;

        @JsonCreator
        public OuterWithNonPublicInner(@JsonProperty("inner") InnerNonPublicCtor inner) {
            this.inner = inner;
        }
    }

    @Test
    void asEmptyFailsWithNoUsableCreator() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder()
                .changeDefaultNullHandling(h -> h.withOverrides(
                        JsonSetter.Value.construct(Nulls.AS_EMPTY, Nulls.AS_EMPTY)))
                .build();
        try {
            mapper.readValue(a2q("{'inner':null}"), OuterWithNonPublicInner.class);
            fail("Should not pass");
        } catch (InvalidDefinitionException e) {
            verifyException(e, "Cannot create empty instance");
        }
    }
}
