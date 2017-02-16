package com.fasterxml.jackson.databind.deser.filter;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonSetter.Nulls;
import com.fasterxml.jackson.databind.*;

// for [databind#1402]; configurable null handling, specifically with SKIP
public class NullConversionsSkipTest extends BaseMapTest
{
    static class NullSkip {
        public String nullsOk = "a";

        @JsonSetter(nulls=JsonSetter.Nulls.SKIP)
        public String noNulls = "b";
    }

    static class StringValue {
        String value = "default";

        @JsonSetter(nulls=JsonSetter.Nulls.SKIP)
        public void setValue(String v) {
            value = v;
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = new ObjectMapper();

    public void testSkipNull() throws Exception
    {
        /*
        // first, ok if assigning non-null to not-nullable, null for nullable
        NullSkip result = MAPPER.readValue(aposToQuotes("{'noNulls':'foo', 'nullsOk':null}"),
                NullSkip.class);
        assertEquals("foo", result.noNulls);
        assertNull(result.nullsOk);

        // and then see that nulls are not ok for non-nullable
        result = MAPPER.readValue(aposToQuotes("{'noNulls':null}"),
                NullSkip.class);
        assertEquals("foo", result.noNulls);
        assertNull(result.nullsOk);
    }

    public void testSkipNullWithDefaults() throws Exception
    {
        String json = aposToQuotes("{'value':null}");
        StringValue result = MAPPER.readValue(json, StringValue.class);
        assertNull(result.value);

        ObjectMapper mapper = new ObjectMapper();
        mapper.configOverride(String.class)
            .setSetterInfo(JsonSetter.Value.forValueNulls(Nulls.SKIP));
        result = mapper.readValue(json, StringValue.class);
        assertEquals("default", result.value);
        */
    }
}
