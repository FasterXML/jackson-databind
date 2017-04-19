package com.fasterxml.jackson.databind.deser.filter;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.databind.*;

// for [databind#1402]; configurable null handling, specifically with SKIP
public class NullConversionsSkipTest extends BaseMapTest
{
    static class NullSkipField {
        public String nullsOk = "a";

        @JsonSetter(nulls=Nulls.SKIP)
        public String noNulls = "b";
    }

    static class NullSkipMethod {
        String _nullsOk = "a";
        String _noNulls = "b";

        public void setNullsOk(String v) {
            _nullsOk = v;
        }

        @JsonSetter(nulls=Nulls.SKIP)
        public void setNoNulls(String v) {
            _noNulls = v;
        }
    }
    
    static class StringValue {
        String value = "default";

        public void setValue(String v) {
            value = v;
        }
    }

    /*
    /**********************************************************
    /* Test methods, straight annotation
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newObjectMapper();

    public void testSkipNullField() throws Exception
    {
        // first, ok if assigning non-null to not-nullable, null for nullable
        NullSkipField result = MAPPER.readValue(aposToQuotes("{'noNulls':'foo', 'nullsOk':null}"),
                NullSkipField.class);
        assertEquals("foo", result.noNulls);
        assertNull(result.nullsOk);

        // and then see that nulls are not ok for non-nullable
        result = MAPPER.readValue(aposToQuotes("{'noNulls':null}"),
                NullSkipField.class);
        assertEquals("b", result.noNulls);
        assertEquals("a", result.nullsOk);
    }

    public void testSkipNullMethod() throws Exception
    {
        NullSkipMethod result = MAPPER.readValue(aposToQuotes("{'noNulls':'foo', 'nullsOk':null}"),
                NullSkipMethod.class);
        assertEquals("foo", result._noNulls);
        assertNull(result._nullsOk);

        result = MAPPER.readValue(aposToQuotes("{'noNulls':null}"),
                NullSkipMethod.class);
        assertEquals("b", result._noNulls);
        assertEquals("a", result._nullsOk);
    }

    /*
    /**********************************************************
    /* Test methods, defaulting
    /**********************************************************
     */
    
    public void testSkipNullWithDefaults() throws Exception
    {
        String json = aposToQuotes("{'value':null}");
        StringValue result = MAPPER.readValue(json, StringValue.class);
        assertNull(result.value);

        ObjectMapper mapper = newObjectMapper();
        mapper.configOverride(String.class)
            .setSetterInfo(JsonSetter.Value.forValueNulls(Nulls.SKIP));
        result = mapper.readValue(json, StringValue.class);
        assertEquals("default", result.value);
    }
}
