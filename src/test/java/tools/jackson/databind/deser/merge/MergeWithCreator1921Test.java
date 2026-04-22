package tools.jackson.databind.deser.merge;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.*;
import tools.jackson.databind.exc.ValueInstantiationException;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

// For [databind#1921]: when merge target type is fully immutable (all properties
// assigned via @JsonCreator, no setters/fields/any-setter), update-mode falls
// back to creator-based construction so that creator-enforced invariants fire —
// instead of throwing InvalidDefinitionException for the missing fallback setter.
class MergeWithCreator1921Test extends DatabindTestUtil
{
    static class Account {
        @JsonMerge(value = OptBoolean.TRUE)
        private final Validity validity;

        @JsonCreator
        public Account(@JsonProperty(value = "validity", required = true) Validity validity) {
            this.validity = validity;
        }

        public Validity getValidity() {
            return validity;
        }
    }

    static class Validity {
        public static final String VALID_FROM_CANT_BE_NULL = "Valid from can't be null";
        public static final String VALID_TO_CANT_BE_BEFORE_VALID_FROM = "Valid to can't be before valid from";

        private final String _validFrom;
        private final String _validTo;

        @JsonCreator
        public Validity(@JsonProperty(value = "validFrom", required = true) String validFrom,
                        @JsonProperty("validTo") String validTo) {
            checkValidity(validFrom, validTo);

            this._validFrom = validFrom;
            this._validTo = validTo;
        }

        private void checkValidity(String from, String to) {
            Objects.requireNonNull(from, VALID_FROM_CANT_BE_NULL);
            if (to != null) {
                if (from.compareTo(to) > 0) {
                    throw new IllegalStateException(VALID_TO_CANT_BE_BEFORE_VALID_FROM);
                }
            }
        }

        public String getValidFrom() {
            return _validFrom;
        }

        public String getValidTo() {
            return _validTo;
        }
    }

    // Mergeable wrapper (no-arg ctor + setter) whose payload is fully-immutable,
    // used to exercise the fix at the inner-property level under a normal merge.
    static class MergeWrapper {
        @JsonMerge
        private OptionalFields payload;
        public OptionalFields getPayload() { return payload; }
        public void setPayload(OptionalFields p) { payload = p; }
    }

    static class OptionalFields {
        public final String a;
        public final String b;
        @JsonCreator
        public OptionalFields(@JsonProperty("a") String a, @JsonProperty("b") String b) {
            this.a = a;
            this.b = b;
        }
    }

    // Mixed: @JsonCreator param AND a setter/field for the same property.
    // The fallback setter means _hasUpdateableProperties() returns true,
    // so the new fix branch must NOT apply; update-mode keeps the existing
    // instance and assigns via the setter.
    static class CreatorPlusSetter {
        private String a;
        @JsonCreator
        public CreatorPlusSetter(@JsonProperty("a") String a) { this.a = a; }
        public String getA() { return a; }
        public void setA(String a) { this.a = a; }
    }

    // Creator-only + @JsonAnySetter. _anySetter != null flips
    // _hasUpdateableProperties() to true, so the fix must NOT apply;
    // any-setter can carry the update into the existing instance.
    static class CreatorPlusAnySetter {
        public final String a;
        public final Map<String, Object> extras = new LinkedHashMap<>();
        @JsonCreator
        public CreatorPlusAnySetter(@JsonProperty("a") String a) { this.a = a; }
        @JsonAnySetter
        public void put(String key, Object value) { extras.put(key, value); }
    }

    @Test
    void mergeWithCreator() throws Exception {
        final String JSON = "{ \"validity\": { \"validFrom\": \"2018-02-01\", \"validTo\": \"2018-01-31\" } }";

        final ObjectMapper mapper = newJsonMapper();

        try {
            mapper.readValue(JSON, Account.class);
            fail("Should not pass");
        } catch (ValueInstantiationException e) {
            verifyException(e, "Cannot construct");
            verifyException(e, Validity.VALID_TO_CANT_BE_BEFORE_VALID_FROM);
        }

        try {
            Account acc = new Account(new Validity("abc", "def"));
            mapper.readerForUpdating(acc)
                    .readValue(JSON);
            fail("Should not pass");
        } catch (ValueInstantiationException e) {
            verifyException(e, "Cannot construct");
            verifyException(e, Validity.VALID_TO_CANT_BE_BEFORE_VALID_FROM);
        }
    }

    // Partial JSON on fully-immutable merge target: narrow fix discards the
    // existing payload and rebuilds via creator, so fields absent from JSON
    // become null rather than inheriting the prior value. A fuller fix
    // (mirroring _deserializeRecordForUpdate for POJOs) would preserve them.
    @Test
    void partialMergeOfImmutableDiscardsExistingValues() throws Exception {
        final ObjectMapper mapper = newJsonMapper();
        MergeWrapper wrapper = new MergeWrapper();
        OptionalFields oldPayload = new OptionalFields("old-a", "old-b");
        wrapper.setPayload(oldPayload);

        MergeWrapper result = mapper.readerForUpdating(wrapper)
                .readValue("{\"payload\":{\"a\":\"new-a\"}}");

        assertSame(wrapper, result);
        assertNotSame(oldPayload, result.getPayload());
        assertEquals("new-a", result.getPayload().a);
        assertNull(result.getPayload().b);
    }

    // Empty JSON object on a fully-immutable merge target: the new branch is
    // positioned after the 3-arg path's `propName == null` short-circuit, so
    // empty `{}` still no-ops and the existing payload is preserved.
    @Test
    void emptyJsonOnImmutableMergeTargetPreservesExisting() throws Exception {
        final ObjectMapper mapper = newJsonMapper();
        MergeWrapper wrapper = new MergeWrapper();
        OptionalFields oldPayload = new OptionalFields("kept-a", "kept-b");
        wrapper.setPayload(oldPayload);

        MergeWrapper result = mapper.readerForUpdating(wrapper)
                .readValue("{\"payload\":{}}");

        assertSame(wrapper, result);
        assertSame(oldPayload, result.getPayload());
        assertEquals("kept-a", result.getPayload().a);
        assertEquals("kept-b", result.getPayload().b);
    }

    // Regression guard: when a creator property has a fallback setter, the
    // new branch must NOT trigger; existing instance is updated in-place.
    @Test
    void creatorWithFallbackSetterUpdatesExistingInstance() throws Exception {
        final ObjectMapper mapper = newJsonMapper();
        CreatorPlusSetter existing = new CreatorPlusSetter("old");
        CreatorPlusSetter result = mapper.readerForUpdating(existing)
                .readValue("{\"a\":\"new\"}");

        assertSame(existing, result);
        assertEquals("new", result.getA());
    }

    // Regression guard: @JsonAnySetter alone is enough to consider the type
    // updateable; existing instance is updated in-place via the any-setter.
    @Test
    void creatorWithAnySetterUpdatesExistingInstance() throws Exception {
        final ObjectMapper mapper = newJsonMapper();
        CreatorPlusAnySetter existing = new CreatorPlusAnySetter("kept");
        CreatorPlusAnySetter result = mapper.readerForUpdating(existing)
                .readValue("{\"x\":\"y\"}");

        assertSame(existing, result);
        assertEquals("kept", result.a);
        assertEquals("y", result.extras.get("x"));
    }
}
