package tools.jackson.databind.interop;

import java.util.Map;

import tools.jackson.core.JsonParser;
import tools.jackson.core.StreamReadCapability;
import tools.jackson.core.util.JacksonFeatureSet;
import tools.jackson.core.util.JsonParserDelegate;

import tools.jackson.databind.*;

// Mostly for XML but can be tested via JSON with some trickery
public class UntypedObjectWithDupsTest extends BaseMapTest
{
    private final ObjectMapper JSON_MAPPER = newJsonMapper();

    // For [dataformat-xml#???]
    public void testDocWithDupsAsUntyped() throws Exception
    {
        _readWriteDocAs(Object.class);
    }

    // For [dataformat-xml#498] / [databind#3484]
    public void testDocWithDupsAsMap() throws Exception
    {
//        _readWriteDocAs(Map.class);
    }

    private <T> void _readWriteDocAs(Class<T> cls) throws Exception
    {
        final String doc = a2q(
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
        // This is where need some trickery
        T value;
        try (JsonParser p = new WithDupsParser(JSON_MAPPER.createParser(doc))) {
            value = JSON_MAPPER.readValue(p, cls);
        }

        String json = JSON_MAPPER.writeValueAsString(value);
        assertEquals(a2q(
"{'hello':'world','lists':[1,2,"
+"{'inner':'internal','time':123},3],'single':'one'}"),
                json);
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
