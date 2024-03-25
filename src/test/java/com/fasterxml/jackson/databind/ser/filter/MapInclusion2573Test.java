package com.fasterxml.jackson.databind.ser.filter;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

public class MapInclusion2573Test extends DatabindTestUtil
{
    @JsonPropertyOrder({ "model", "properties" })
    static class Car
    {
        public String model;
        public Map<String, Integer> properties;
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final Map<String, Integer> CAR_PROPERTIES = new LinkedHashMap<>();
    {
        CAR_PROPERTIES.put("Speed", 100);
        CAR_PROPERTIES.put("Weight", null);
    }

    private final Car CAR = new Car();
    {
        CAR.model = "F60";
        CAR.properties = CAR_PROPERTIES;
    }

    private final JsonInclude.Value BOTH_NON_NULL = JsonInclude.Value.construct(JsonInclude.Include.NON_NULL,
            JsonInclude.Include.NON_NULL);

//    final private ObjectMapper MAPPER = newJsonMapper();

    // [databind#2572]
    @Test
    public void test2572MapDefault() throws Exception
    {

        ObjectMapper mapper = JsonMapper.builder()
                .defaultPropertyInclusion(BOTH_NON_NULL)
                .build();
        assertEquals(a2q("{'Speed':100}"),
                mapper.writeValueAsString(CAR_PROPERTIES));
        assertEquals(a2q("{'model':'F60','properties':{'Speed':100}}"),
                mapper.writeValueAsString(CAR));
    }

    // [databind#2572]
    @Test
    public void test2572MapOverrideUseDefaults() throws Exception
    {
        ObjectMapper mapper = JsonMapper.builder()
                .defaultPropertyInclusion(BOTH_NON_NULL)
                .build();
        mapper.configOverride(Map.class)
            .setInclude(JsonInclude.Value.construct(JsonInclude.Include.USE_DEFAULTS,
                    JsonInclude.Include.USE_DEFAULTS));
        assertEquals(a2q("{'Speed':100}"),
                mapper.writeValueAsString(CAR_PROPERTIES));
        assertEquals(a2q("{'model':'F60','properties':{'Speed':100}}"),
                mapper.writeValueAsString(CAR));
    }

    // [databind#2572]
    @Test
    public void test2572MapOverrideInclAlways() throws Exception
    {
        ObjectMapper mapper = JsonMapper.builder()
                .defaultPropertyInclusion(BOTH_NON_NULL)
                .build();
        mapper.configOverride(Map.class)
            .setInclude(JsonInclude.Value.construct(JsonInclude.Include.ALWAYS,
                    JsonInclude.Include.ALWAYS));
        assertEquals(a2q("{'Speed':100,'Weight':null}"),
                mapper.writeValueAsString(CAR_PROPERTIES));
        assertEquals(a2q("{'model':'F60','properties':{'Speed':100,'Weight':null}}"),
                mapper.writeValueAsString(CAR));
    }
}
