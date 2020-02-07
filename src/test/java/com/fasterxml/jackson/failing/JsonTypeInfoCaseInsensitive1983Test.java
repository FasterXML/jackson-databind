package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;

public class JsonTypeInfoCaseInsensitive1983Test extends BaseMapTest {

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "operation")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = Equal.class, name = "eq"),
            @JsonSubTypes.Type(value = NotEqual.class, name = "noteq"),
    })
    static abstract class Filter {
    }

    static class Equal extends Filter {
    }

    static class NotEqual extends Filter {
    }

    public void testReadMixedCaseSubclass() throws IOException {
//        There is also "ACCEPT_CASE_INSENSITIVE_VALUES" feature. Is it more suitable here, or I should leave it as is?
        ObjectMapper mapper = objectMapper().enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES);
        String serialised = "{\"operation\":\"NoTeQ\"}";

        Filter result = mapper.readValue(serialised, Filter.class);

        assertEquals(NotEqual.class, result.getClass());
    }

    public void testReadMixedCasePropertyName() throws IOException {
        ObjectMapper mapper = objectMapper().enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES);
        String serialised = "{\"oPeRaTioN\":\"noteq\"}";

        Filter result = mapper.readValue(serialised, Filter.class);

        assertEquals(NotEqual.class, result.getClass());
    }
}
