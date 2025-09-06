package com.fasterxml.jackson.databind.tofix;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [databind#5292] Need support for creators `MapperFeature.FIX_FIELD_NAME_UPPER_CASE_PREFIX`
public class FixFieldNameUpperCasePrefix5292Test
        extends DatabindTestUtil
{
    public static class AppleSingleNonTarget {

        private final String name;

        public AppleSingleNonTarget(@ImplicitName("name") String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public static class AppleSingleIsTarget {

        private final String iPhone;

        public AppleSingleIsTarget(@ImplicitName("iPhone") String iPhone) {
            this.iPhone = iPhone;
        }

        public String getIPhone() {
            return iPhone;
        }
    }

    public static class AppleDouble {
        private final String iPhone;
        private final String name;

        public AppleDouble(String iPhone, String name) {
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

    private ObjectMapper MAPPER = JsonMapper.builder()
        .annotationIntrospector(new ImplicitNameIntrospector())
        .enable(MapperFeature.FIX_FIELD_NAME_UPPER_CASE_PREFIX)
        .build();

    @Test
    public void testDeserDOuble()
            throws Exception {

        AppleDouble apple = new AppleDouble("iPhone 15", "Jay");
        String json = MAPPER.writeValueAsString(apple);
        assertEquals("{\"iPhone\":\"iPhone 15\",\"name\":\"Jay\"}", json);

        AppleDouble result = MAPPER.readValue(json, AppleDouble.class); // Error thrown

        assertEquals("Jay", result.getName());
        assertEquals("iPhone 15", result.getName());
    }

    @Test
    public void testSingleArgCase()
            throws Exception {

        AppleSingleIsTarget apple = new AppleSingleIsTarget("iPhone 15");
        String json = MAPPER.writeValueAsString(apple);
        assertEquals("{\"iPhone\":\"iPhone 15\"}", json);

        AppleSingleIsTarget result = MAPPER.readValue(json, AppleSingleIsTarget.class); // Error thrown
        assertEquals("iPhone 15", result.getIPhone());
    }

    // Just for comparison
    @Test
    public void testHappyCaseSingleArgString()
            throws Exception
    {
        AppleSingleNonTarget apple = new AppleSingleNonTarget("Jay");
        String json = MAPPER.writeValueAsString(apple);
        assertEquals("{\"name\":\"Jay\"}", json);

        AppleSingleNonTarget result = MAPPER.readValue(json, AppleSingleNonTarget.class); // Error thrown
        assertEquals("Jay", result.getName());
    }

}
