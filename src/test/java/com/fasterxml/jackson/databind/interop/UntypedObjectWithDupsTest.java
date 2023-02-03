package com.fasterxml.jackson.databind.interop;

import java.util.*;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.StreamReadCapability;
import com.fasterxml.jackson.core.util.JacksonFeatureSet;
import com.fasterxml.jackson.core.util.JsonParserDelegate;
import com.fasterxml.jackson.databind.*;

// Mostly for XML but can be tested via JSON with some trickery
public class UntypedObjectWithDupsTest extends BaseMapTest
{
    private final ObjectMapper JSON_MAPPER = newJsonMapper();

    @SuppressWarnings("serial")
    static class StringStringMap extends LinkedHashMap<String,String> { };

    private final String DOC_WITH_DUPS = a2q(
            "{'hello': 'world',\n"
            + "'lists' : 1,\n"
            + "'lists' : 2,\n"
            + "'lists' : {\n"
            + "  'inner' : 'internal',\n"
            + "  'time' : 123\n"
            + "},\n"
            + "'lists' : 3,\n"
            + "'single' : 'one'\n"
            + "}");

    // Testing the baseline non-merging behavior
    public void testDocWithDupsNoMerging() throws Exception
    {
        _verifyDupsNoMerging(Object.class);
        _verifyDupsNoMerging(Map.class);
    }

    // For [dataformat-xml#???]
    public void testDocWithDupsAsUntyped() throws Exception
    {
        _verifyDupsAreMerged(Object.class);
    }

    // For [dataformat-xml#498] / [databind#3484]
    public void testDocWithDupsAsMap() throws Exception
    {
        _verifyDupsAreMerged(Map.class);
    }

    // And also verify that Maps with values other than `Object` will
    // NOT try merging no matter what
    public void testDocWithDupsAsNonUntypedMap() throws Exception
    {
        final String DOC = a2q("{'key':'a','key':'b'}");
        assertEquals(a2q("{'key':'b'}"),
                _readWriteDupDoc(DOC, StringStringMap.class));
    }

    /*
    ///////////////////////////////////////////////////////////////////////
    // Helper methods
    ///////////////////////////////////////////////////////////////////////
     */

    /* Method that will verify default JSON behavior of overwriting value
     * (no merging).
     */
    private <T> void _verifyDupsNoMerging(Class<T> cls) throws Exception
    {
        // This is where need some trickery
        T value;
        try (JsonParser p = JSON_MAPPER.createParser(DOC_WITH_DUPS)) {
            value = JSON_MAPPER.readValue(p, cls);
        }

        String json = JSON_MAPPER.writeValueAsString(value);
        assertEquals(a2q(
"{'hello':'world','lists':3,'single':'one'}"),
                json);
    }

    /* Method that will verify alternate behavior (used by XML module f.ex)
     * in which duplicate "properties" are merged into `List`s as necessary
     */
    private void _verifyDupsAreMerged(Class<?> cls) throws Exception
    {
        assertEquals(a2q(
"{'hello':'world','lists':[1,2,"
+"{'inner':'internal','time':123},3],'single':'one'}"),
                _readWriteDupDoc(DOC_WITH_DUPS, cls));
    }

    private String _readWriteDupDoc(String doc, Class<?> cls) throws Exception
    {
        // This is where need some trickery
        Object value;
        try (JsonParser p = new WithDupsParser(JSON_MAPPER.createParser(doc))) {
            value = JSON_MAPPER.readValue(p, cls);
        }
        return JSON_MAPPER.writeValueAsString(value);
    }

    /**
     * Helper class to fake "DUPLICATE_PROPERTIES" on JSON parser
     * (which usually does not expose this).
     */
    static class WithDupsParser extends JsonParserDelegate
    {
        public WithDupsParser(JsonParser p) {
            super(p);
        }

        @Override
        public JacksonFeatureSet<StreamReadCapability> getReadCapabilities() {
            JacksonFeatureSet<StreamReadCapability> caps = super.getReadCapabilities();
            caps = caps.with(StreamReadCapability.DUPLICATE_PROPERTIES);
            return caps;
        }
    }
}
