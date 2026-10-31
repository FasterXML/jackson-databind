package tools.jackson.databind.interop;

import java.util.*;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonParser;
import tools.jackson.core.StreamReadCapability;
import tools.jackson.core.util.JacksonFeatureSet;
import tools.jackson.core.util.JsonParserDelegate;
import tools.jackson.databind.*;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Mostly for XML but can be tested via JSON with some trickery
public class UntypedObjectWithDupsTest extends DatabindTestUtil
{
    private final ObjectMapper JSON_MAPPER = newJsonMapper();

    @SuppressWarnings("serial")
    static class StringStringMap extends LinkedHashMap<String,String> { };

    private final String DOC_WITH_DUPS = a2q(
            """
            {'hello': 'world',
            'lists' : 1,
            'lists' : 2.5,
            'lists' : {
              'inner' : 'internal',
              'time' : 123
            },
            'lists' : true,
            'single' : 'one',
            'lists' : false,
            'lists' : null
            }
            """);

    // Testing the baseline non-merging behavior
    @Test
    public void testDocWithDupsNoMerging() throws Exception
    {
        _verifyDupsNoMerging(Object.class);
        _verifyDupsNoMerging(Map.class);
    }

    // For [dataformat-xml#???]
    @Test
    public void testDocWithDupsAsUntyped() throws Exception
    {
        _verifyDupsAreMerged(Object.class);
    }

    // For [dataformat-xml#498] / [databind#3484]
    @Test
    public void testDocWithDupsAsMap() throws Exception
    {
        _verifyDupsAreMerged(Map.class);
    }

    // And also verify that Maps with values other than `Object` will
    // NOT try merging no matter what
    @Test
    public void testDocWithDupsAsNonUntypedMap() throws Exception
    {
        final String DOC = a2q("{'key':'a','key':'b'}");
        assertEquals(a2q("{'key':'b'}"),
                _readWriteDupDoc(DOC, StringStringMap.class));
    }

    // [databind#6296]: with a custom key deserializer the non-vanilla deserializer
    // is used; duplicates of a later key must be merged under that key, not the first one
    @Test
    public void docWithDupsAsUntypedWithKeyDeserializer() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(new SimpleModule()
                        .addKeyDeserializer(Object.class, new KeyDeserializer() {
                            @Override
                            public Object deserializeKey(String key, DeserializationContext ctxt) {
                                return key;
                            }
                        }))
                .build();
        // First duplicate on the second key
        assertEquals(a2q("{'a':[1,2],'b':[3,5],'c':4}"),
                _readWriteDupDoc(mapper, a2q("{'a':1,'a':2,'b':3,'c':4,'b':5}"), Object.class));
        // First duplicate on a later key; then another key duplicated three times
        assertEquals(a2q("{'a':1,'b':[2,5,6],'c':[3,4]}"),
                _readWriteDupDoc(mapper, a2q("{'a':1,'b':2,'c':3,'c':4,'b':5,'b':6}"), Object.class));
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
"{'hello':'world','lists':null,'single':'one'}"),
                json);
    }

    /* Method that will verify alternate behavior (used by XML module f.ex)
     * in which duplicate "properties" are merged into `List`s as necessary
     */
    private void _verifyDupsAreMerged(Class<?> cls) throws Exception
    {
        assertEquals(a2q(
"{'hello':'world','lists':[1,2.5,"
+"{'inner':'internal','time':123},true,false,null],'single':'one'}"),
                _readWriteDupDoc(DOC_WITH_DUPS, cls));
    }

    private String _readWriteDupDoc(String doc, Class<?> cls) throws Exception
    {
        return _readWriteDupDoc(JSON_MAPPER, doc, cls);
    }

    private String _readWriteDupDoc(ObjectMapper mapper, String doc, Class<?> cls) throws Exception
    {
        // This is where need some trickery
        Object value;
        try (JsonParser p = new WithDupsParser(mapper.createParser(doc))) {
            value = mapper.readValue(p, cls);
        }
        return mapper.writeValueAsString(value);
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
        public JacksonFeatureSet<StreamReadCapability> streamReadCapabilities() {
            JacksonFeatureSet<StreamReadCapability> caps = super.streamReadCapabilities();
            caps = caps.with(StreamReadCapability.DUPLICATE_PROPERTIES);
            return caps;
        }
    }
}
