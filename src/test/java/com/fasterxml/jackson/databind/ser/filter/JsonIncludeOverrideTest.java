package com.fasterxml.jackson.databind.ser.filter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Unit tests for checking that overridden settings for
 * {@link com.fasterxml.jackson.databind.annotation.JsonSerialize#include} annotation property work
 * as expected.
 */
public class JsonIncludeOverrideTest
    extends BaseMapTest
{
    @JsonPropertyOrder({"list", "map"})
    static class EmptyListMapBean
    {
        public List<String> list = Collections.emptyList();

        public Map<String,String> map = Collections.emptyMap();
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonPropertyOrder({"num", "annotated", "plain"})
    static class MixedTypeAlwaysBean
    {
        @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
        public Integer num = null;

        @JsonInclude(JsonInclude.Include.NON_NULL)
        public String annotated = null;

        public String plain = null;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyOrder({"num", "annotated", "plain"})
    static class MixedTypeNonNullBean
    {
        @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
        public Integer num = null;

        @JsonInclude(JsonInclude.Include.ALWAYS)
        public String annotated = null;

        public String plain = null;
    }

    public void testPropConfigOverridesForInclude() throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        // First, with defaults, both included:
        JsonIncludeOverrideTest.EmptyListMapBean empty = new JsonIncludeOverrideTest.EmptyListMapBean();
        assertEquals(a2q("{'list':[],'map':{}}"),
                mapper.writeValueAsString(empty));

        // and then change inclusion criteria for either
        mapper = new ObjectMapper();
        mapper.configOverride(Map.class)
            .setInclude(JsonInclude.Value.construct(JsonInclude.Include.NON_EMPTY, null));
        assertEquals(a2q("{'list':[]}"),
                mapper.writeValueAsString(empty));

        mapper = new ObjectMapper();
        mapper.configOverride(List.class)
            .setInclude(JsonInclude.Value.construct(JsonInclude.Include.NON_EMPTY, null));
        assertEquals(a2q("{'map':{}}"),
                mapper.writeValueAsString(empty));
    }

    public void testOverrideForIncludeAsPropertyNonNull() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        // First, with defaults, all but NON_NULL annotated included
        JsonIncludeOverrideTest.MixedTypeAlwaysBean nullValues = new JsonIncludeOverrideTest.MixedTypeAlwaysBean();
        assertEquals(a2q("{'num':null,'plain':null}"),
                mapper.writeValueAsString(nullValues));

        // and then change inclusion as property criteria for either
        mapper = new ObjectMapper();
        mapper.configOverride(String.class)
                .setIncludeAsProperty(JsonInclude.Value
                        .construct(JsonInclude.Include.NON_NULL, null));
        assertEquals("{\"num\":null}",
                mapper.writeValueAsString(nullValues));

        mapper = new ObjectMapper();
        mapper.configOverride(Integer.class)
                .setIncludeAsProperty(JsonInclude.Value
                        .construct(JsonInclude.Include.NON_NULL, null));
        assertEquals("{\"plain\":null}",
                mapper.writeValueAsString(nullValues));
    }

    public void testOverrideForIncludeAsPropertyAlways() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        // First, with defaults, only ALWAYS annotated included
        JsonIncludeOverrideTest.MixedTypeNonNullBean nullValues = new JsonIncludeOverrideTest.MixedTypeNonNullBean();
        assertEquals("{\"annotated\":null}",
                mapper.writeValueAsString(nullValues));

        // and then change inclusion as property criteria for either
        mapper = new ObjectMapper();
        mapper.configOverride(String.class)
                .setIncludeAsProperty(JsonInclude.Value
                        .construct(JsonInclude.Include.ALWAYS, null));
        assertEquals(a2q("{'annotated':null,'plain':null}"),
                mapper.writeValueAsString(nullValues));

        mapper = new ObjectMapper();
        mapper.configOverride(Integer.class)
                .setIncludeAsProperty(JsonInclude.Value
                        .construct(JsonInclude.Include.ALWAYS, null));
        assertEquals(a2q("{'num':null,'annotated':null}"),
                mapper.writeValueAsString(nullValues));
    }

    public void testOverridesForIncludeAndIncludeAsPropertyNonNull() throws Exception
    {
        // First, with ALWAYS override on containing bean, all included
        JsonIncludeOverrideTest.MixedTypeNonNullBean nullValues = new JsonIncludeOverrideTest.MixedTypeNonNullBean();
        ObjectMapper mapper = new ObjectMapper();
        mapper.configOverride(JsonIncludeOverrideTest.MixedTypeNonNullBean.class)
                .setInclude(JsonInclude.Value
                        .construct(JsonInclude.Include.ALWAYS, null));
        assertEquals(a2q("{'num':null,'annotated':null,'plain':null}"),
                mapper.writeValueAsString(nullValues));

        // and then change inclusion as property criteria for either
        mapper = new ObjectMapper();
        mapper.configOverride(JsonIncludeOverrideTest.MixedTypeNonNullBean.class)
                .setInclude(JsonInclude.Value
                        .construct(JsonInclude.Include.ALWAYS, null));
        mapper.configOverride(String.class)
                .setIncludeAsProperty(JsonInclude.Value
                        .construct(JsonInclude.Include.NON_NULL, null));
        assertEquals(a2q("{'num':null,'annotated':null}"),
                mapper.writeValueAsString(nullValues));

        mapper = new ObjectMapper();
        mapper.configOverride(JsonIncludeOverrideTest.MixedTypeNonNullBean.class)
                .setInclude(JsonInclude.Value
                        .construct(JsonInclude.Include.ALWAYS, null));
        mapper.configOverride(Integer.class)
                .setIncludeAsProperty(JsonInclude.Value
                        .construct(JsonInclude.Include.NON_NULL, null));
        assertEquals(a2q("{'annotated':null,'plain':null}"),
                mapper.writeValueAsString(nullValues));
    }

    public void testOverridesForIncludeAndIncludeAsPropertyAlways() throws Exception
    {
        // First, with NON_NULL override on containing bean, empty
        JsonIncludeOverrideTest.MixedTypeAlwaysBean nullValues = new JsonIncludeOverrideTest.MixedTypeAlwaysBean();
        ObjectMapper mapper = new ObjectMapper();
        mapper.configOverride(JsonIncludeOverrideTest.MixedTypeAlwaysBean.class)
                .setInclude(JsonInclude.Value
                        .construct(JsonInclude.Include.NON_NULL, null));
        assertEquals("{}",
                mapper.writeValueAsString(nullValues));

        // and then change inclusion as property criteria for either
        mapper = new ObjectMapper();
        mapper.configOverride(JsonIncludeOverrideTest.MixedTypeAlwaysBean.class)
                .setInclude(JsonInclude.Value
                        .construct(JsonInclude.Include.NON_NULL, null));
        mapper.configOverride(String.class)
                .setIncludeAsProperty(JsonInclude.Value
                        .construct(JsonInclude.Include.ALWAYS, null));
        assertEquals("{\"plain\":null}",
                mapper.writeValueAsString(nullValues));

        mapper = new ObjectMapper();
        mapper.configOverride(JsonIncludeOverrideTest.MixedTypeAlwaysBean.class)
                .setInclude(JsonInclude.Value
                        .construct(JsonInclude.Include.NON_NULL, null));
        mapper.configOverride(Integer.class)
                .setIncludeAsProperty(JsonInclude.Value
                        .construct(JsonInclude.Include.ALWAYS, null));
        assertEquals("{\"num\":null}",
                mapper.writeValueAsString(nullValues));
    }
}
