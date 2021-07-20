package com.fasterxml.jackson.databind.deser.filter;

import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.filter.FilteringParserDelegate;
import com.fasterxml.jackson.core.filter.TokenFilter;
import com.fasterxml.jackson.databind.*;

public class ParserFilterViaMapTest extends BaseMapTest
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
