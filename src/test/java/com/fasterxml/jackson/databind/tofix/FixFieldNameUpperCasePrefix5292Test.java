package com.fasterxml.jackson.databind.tofix;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;
import com.fasterxml.jackson.databind.testutil.failure.JacksonTestFailureExpected;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [databind#5292] Need support for creators `MapperFeature.FIX_FIELD_NAME_UPPER_CASE_PREFIX`
public class FixFieldNameUpperCasePrefix5292Test
        extends DatabindTestUtil
{
    static class AppleSingleNonTarget {
        private final String name;

        public AppleSingleNonTarget(@ImplicitName("name") String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    static class AppleSingleIsTarget {
        private final String iPhone;

        @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
        public AppleSingleIsTarget(@ImplicitName("iPhone") String iPhone) {
            this.iPhone = iPhone;
        }

        public String getIPhone() {
            return iPhone;
        }
    }

    // Creator order should be used but just in case, define explicit order
    @JsonPropertyOrder({ "iPhone", "name" })
    static class AppleDouble {
        private final String iPhone;
        private final String name;

        public AppleDouble(@ImplicitName("iPhone") String iPhone,
                @ImplicitName("name") String name) {
            this.iPhone = iPhone;
            this.name = name;
        }

        public String getIPhone() {
            return iPhone;
        }

        public String getName() {
            return name;
        }
    }

    private final ObjectMapper MAPPER = JsonMapper.builder()
        .annotationIntrospector(new ImplicitNameIntrospector())
        .enable(MapperFeature.FIX_FIELD_NAME_UPPER_CASE_PREFIX)
        .build();

    @JacksonTestFailureExpected
    @Test
    public void testDeserDouble() throws Exception
    {
        AppleDouble apple = new AppleDouble("iPhone 15", "Jay");
        String json = MAPPER.writeValueAsString(apple);
        assertEquals("{\"iPhone\":\"iPhone 15\",\"name\":\"Jay\"}", json);

        AppleDouble result = MAPPER.readValue(json, AppleDouble.class); // Error thrown

        assertEquals("Jay", result.getName());
        assertEquals("iPhone 15", result.getName());
    }

    @JacksonTestFailureExpected
    @Test
    public void testSingleArgCase() throws Exception
    {
        AppleSingleIsTarget apple = new AppleSingleIsTarget("iPhone 15");
        String json = MAPPER.writeValueAsString(apple);
        assertEquals("{\"iPhone\":\"iPhone 15\"}", json);

        AppleSingleIsTarget result = MAPPER.readValue(json, AppleSingleIsTarget.class); // Error thrown
        assertEquals("iPhone 15", result.getIPhone());
    }

    // Just for comparison
    @Test
    public void testHappyCaseSingleArgString() throws Exception
    {
        AppleSingleNonTarget apple = new AppleSingleNonTarget("Jay");
        String json = MAPPER.writeValueAsString(apple);
        assertEquals("{\"name\":\"Jay\"}", json);

        AppleSingleNonTarget result = MAPPER.readValue(json, AppleSingleNonTarget.class); // Error thrown
        assertEquals("Jay", result.getName());
    }
}
