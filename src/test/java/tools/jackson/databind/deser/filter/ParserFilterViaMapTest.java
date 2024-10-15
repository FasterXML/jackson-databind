package tools.jackson.databind.deser.filter;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import tools.jackson.core.*;
import tools.jackson.core.filter.FilteringParserDelegate;
import tools.jackson.core.filter.TokenFilter;
import tools.jackson.databind.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static tools.jackson.databind.testutil.DatabindTestUtil.newJsonMapper;

public class ParserFilterViaMapTest
{
    private final ObjectMapper MAPPER = newJsonMapper();

    static class NoTypeFilter extends TokenFilter {
        @Override
        public TokenFilter includeProperty(String name) {
            if ("@type".equals(name)) {
                return null;
            }
            return this;
        }
    }

    // From [core#700], to verify at databind level
    // (and actually found a bug in doing so -- fixed for 2.13.0)
    @Test
    public void testSimplePropertyExcludeFilter() throws Exception
    {
        final String json = "{\"@type\":\"xxx\",\"a\":{\"@type\":\"yyy\",\"b\":11}}";
        try (JsonParser p = new FilteringParserDelegate(
                MAPPER.createParser(json),
                new NoTypeFilter(),
                TokenFilter.Inclusion.INCLUDE_ALL_AND_PATH,
                true)) {
            Map<?,?> map = MAPPER.readValue(p, Map.class);
            Map<String, Object> EXP = Collections.singletonMap("a",
                    Collections.singletonMap("b", Integer.valueOf(11)));
            assertEquals(EXP, map);
        }
    }
}
