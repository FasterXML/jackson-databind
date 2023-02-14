package com.fasterxml.jackson.databind.deser.filter;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.exc.InvalidNullException;

import java.util.Collection;

// for [databind#1402]; configurable null handling, for values themselves
public class NullConversionsPojoTest extends BaseMapTest
{
    static class NullFail {
        public String nullsOk = "a";

        @JsonSetter(nulls=Nulls.FAIL)
        public String noNulls = "b";
    }

    static class NullFailCtor {
        String value;

        @JsonCreator
        public NullFailCtor(@JsonSetter(nulls=Nulls.FAIL)
            @JsonProperty("noNulls") String v)
        {
            value = v;
        }
    }

    static class NullAsEmpty {
        public String nullsOk = "a";

        @JsonSetter(nulls=Nulls.AS_EMPTY)
        public String nullAsEmpty = "b";
    }

    static class NullAsEmptyCtor {
        String _nullsOk;

        String _nullAsEmpty;

        @JsonCreator
        public NullAsEmptyCtor(
                @JsonProperty("nullsOk") String nullsOk,
                @JsonSetter(nulls=Nulls.AS_EMPTY)
                @JsonProperty("nullAsEmpty") String nullAsEmpty)
        {
            _nullsOk = nullsOk;
            _nullAsEmpty = nullAsEmpty;
        }
    }

    static class NullsForString {
        String n = "foo";

        public void setName(String n0) { n = n0; }
        public String getName() { return n; }
    }

    // [databind#3645]
    static class Issue3645BeanA {
        private String name;
        private Collection<Integer> prices;

        public Issue3645BeanA(
            @JsonProperty("name") String name,
            @JsonProperty("prices")
            @JsonSetter(nulls = Nulls.AS_EMPTY) Collection<Integer> prices
        ) {
            this.name = name;
            this.prices = prices;
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    public void testFailOnNull() throws Exception
    {
        // first, ok if assigning non-null to not-nullable, null for nullable
        NullFail result = MAPPER.readValue(a2q("{'noNulls':'foo', 'nullsOk':null}"),
                NullFail.class);
        assertEquals("foo", result.noNulls);
        assertNull(result.nullsOk);

        // and then see that nulls are not ok for non-nullable
        try {
            result = MAPPER.readValue(a2q("{'noNulls':null}"),
                    NullFail.class);
            fail("Should not pass");
        } catch (InvalidNullException e) {
            verifyException(e, "property \"noNulls\"");
        }

        // Ditto via constructor; first explicit
        try {
            /* NullFailCtor r =*/ MAPPER.readValue(a2q("{'noNulls':null}"),
                    NullFailCtor.class);
            fail("Should not pass");
        } catch (InvalidNullException e) {
            verifyException(e, "property \"noNulls\"");
        }

        // and then implicit (missing -> null)
        try {
            /* NullFailCtor r =*/ MAPPER.readValue("{ }", NullFailCtor.class);
            fail("Should not pass");
        } catch (InvalidNullException e) {
            verifyException(e, "property \"noNulls\"");
        }
    }

    public void testFailOnNullWithDefaults() throws Exception
    {
        // also: config overrides by type should work
        String json = a2q("{'name':null}");
        NullsForString def = MAPPER.readValue(json, NullsForString.class);
        assertNull(def.getName());

        ObjectMapper mapper = newJsonMapper();
        mapper.configOverride(String.class)
            .setSetterInfo(JsonSetter.Value.forValueNulls(Nulls.FAIL));
        try {
            mapper.readValue(json, NullsForString.class);
            fail("Should not pass");
        } catch (InvalidNullException e) {
            verifyException(e, "property \"name\"");
        }
    }

    public void testNullsToEmptyScalar() throws Exception
    {
        NullAsEmpty result = MAPPER.readValue(a2q("{'nullAsEmpty':'foo', 'nullsOk':null}"),
                NullAsEmpty.class);
        assertEquals("foo", result.nullAsEmpty);
        assertNull(result.nullsOk);

        // and then see that nulls are not ok for non-nullable
        result = MAPPER.readValue(a2q("{'nullAsEmpty':null}"),
                NullAsEmpty.class);
        assertEquals("", result.nullAsEmpty);

        // also: config overrides by type should work
        String json = a2q("{'name':null}");
        NullsForString def = MAPPER.readValue(json, NullsForString.class);
        assertNull(def.getName());

        ObjectMapper mapper = newJsonMapper();
        mapper.configOverride(String.class)
            .setSetterInfo(JsonSetter.Value.forValueNulls(Nulls.AS_EMPTY));
        NullsForString named = mapper.readValue(json, NullsForString.class);
        assertEquals("", named.getName());
    }

    public void testNullsToEmptyViaCtor() throws Exception
    {
        NullAsEmptyCtor result = MAPPER.readValue(a2q("{'nullAsEmpty':'foo', 'nullsOk':null}"),
                NullAsEmptyCtor.class);
        assertEquals("foo", result._nullAsEmpty);
        assertNull(result._nullsOk);

        // and then see that nulls are not ok for non-nullable
        result = MAPPER.readValue(a2q("{'nullAsEmpty':null}"),
                NullAsEmptyCtor.class);
        assertEquals("", result._nullAsEmpty);

        // and get coerced from "missing", as well
        result = MAPPER.readValue(a2q("{}"), NullAsEmptyCtor.class);
        assertEquals("", result._nullAsEmpty);
    }

    // [databind#3645]
    public void testDeserializeMissingCollectionFieldAsEmpty() throws Exception {
        String json = "{\"name\": \"Computer\"}";

        Issue3645BeanA actual = MAPPER.readValue(json, Issue3645BeanA.class);

        assertEquals(actual.name, "Computer");
        assertTrue(actual.prices.isEmpty());
    }

    // [databind#3645]
    public void testDeserializeNullAsEmpty() throws Exception {
        String json = "{\"name\": \"Computer\", \"prices\" : null}";

        Issue3645BeanA actual = MAPPER.readValue(json, Issue3645BeanA.class);

        assertEquals(actual.name, "Computer");
        assertTrue(actual.prices.isEmpty());
    }
}
