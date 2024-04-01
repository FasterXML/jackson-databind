package com.fasterxml.jackson.databind.jsontype.jdk;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

/**
 * Unit test proving that below issue is fixed.
 * <p>
 * [databind#3133] Map deserialization results in different numeric classes based on json
 * ordering (BigDecimal / Double) when used in combination with @JsonSubTypes
 */
public class BigDecimalForFloatDisabled3133Test
    extends DatabindTestUtil
{
    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.PROPERTY,
            property = "type")

    @JsonSubTypes({
            @JsonSubTypes.Type(value = TestMapContainer3133.class, name = "MAP"),
    })
    interface BaseType3133 { }

    static class TestMapContainer3133 implements BaseType3133 {

        private Map<String, ? extends Object> map = new HashMap<>();

        public Map<String, ? extends Object> getMap() {
            return map;
        }

        public void setMap(Map<String, ? extends Object> map) {
            this.map = map;
        }
    }

    private final ObjectMapper mapper = jsonMapperBuilder()
            .disable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .build();

    // [databind#3133]
    @Test
    public void testDeserializeWithDifferentOrdering3133() throws Exception
    {
        // case 1 : type first
        String ordering1 = a2q("{'type': 'MAP','map': { 'doubleValue': 0.1 }}");
        TestMapContainer3133 model1 = mapper.readValue(ordering1, TestMapContainer3133.class);
        Assertions.assertTrue(model1.getMap().get("doubleValue") instanceof Double);

        // case 2 : value first
        String ordering2 = a2q("{'map': { 'doubleValue': 0.1 }, 'type': 'MAP'}");
        TestMapContainer3133 model2 = mapper.readValue(ordering2, TestMapContainer3133.class);
        Assertions.assertTrue(model2.getMap().get("doubleValue") instanceof Double);
    }
}
