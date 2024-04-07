package com.fasterxml.jackson.failing;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.*;

// Tests for
//
// [databind#2644]
// [databind#2785]
public class JDKNumberDeser2644Test extends BaseMapTest
{
    // [databind#2644]
    static class NodeRoot2644 {
        public String type;

        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "type")
        @JsonSubTypes(value = {
                @JsonSubTypes.Type(value = NodeParent2644.class, name = "NodeParent")
        })
        public Node2644 node;
    }

    public static class NodeParent2644 extends Node2644 { }

    public static abstract class Node2644 {
        @JsonProperty("amount")
        BigDecimal val;

        public BigDecimal getVal() {
            return val;
        }

        public void setVal(BigDecimal val) {
            this.val = val;
        }
    }

    // [databind#2785]
    static class BigDecimalHolder2785 {
        public BigDecimal value;
    }

    static class NestedBigDecimalHolder2785 {
        @JsonUnwrapped
        public BigDecimalHolder2785 holder;
    }
    
    // [databind#2644]
    public void testBigDecimalSubtypes() throws Exception
    {
        ObjectMapper mapper = newJsonMapper();

        // NOTE: uncommenting this does work around the issue:
//        mapper.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
        mapper.registerSubtypes(NodeParent2644.class);

        NodeRoot2644 root = mapper.readValue(
                "{\"type\": \"NodeParent\",\"node\": {\"amount\": 9999999999999999.99} }",
                NodeRoot2644.class
        );

        assertEquals(new BigDecimal("9999999999999999.99"), root.node.getVal());
    }

    // [databind#2785]
   
    public void testBigDecimalUnwrapped() throws Exception
    {
        final ObjectMapper mapper = newJsonMapper();
        // mapper.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
        final String JSON = "{\"value\": 5.00}";

        // first simple working case:
        BigDecimalHolder2785 holder = mapper.readValue(JSON, BigDecimalHolder2785.class);
        assertEquals(new BigDecimal("5.00"), holder.value);

        // and then one that doesn't
        NestedBigDecimalHolder2785 result = mapper.readValue(JSON, NestedBigDecimalHolder2785.class);
        assertEquals(new BigDecimal("5.00"), result.holder.value);
    }
}
