package com.fasterxml.jackson.databind.misc;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [databind#5335]
public class IPhoneStyleProperty5335Test
    extends DatabindTestUtil
{
    @JsonPropertyOrder({"aProp", "anotherProp"})
    static class TestPojo {
        private String aProp;
        private String anotherProp;

        public String getaProp() {
            return aProp;
        }

        public void setaProp(String aProp) {
            this.aProp = aProp;
        }

        public String getAnotherProp() {
            return anotherProp;
        }

        public void setAnotherProp(String anotherProp) {
            this.anotherProp = anotherProp;
        }
    }

    @Test
    public void featureEnabledTest() throws Exception
    {
      ObjectMapper mapper = JsonMapper.builder()
              .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
              .enable(MapperFeature.FIX_FIELD_NAME_UPPER_CASE_PREFIX)
              .build();
    
      String json = "{\"aProp\":\"aPropValue\",\"prop1\":\"prop1Value\"}";
      TestPojo result = mapper.readValue(json, TestPojo.class);
      assertEquals("aPropValue", result.getaProp());
      String serialized = mapper.writeValueAsString(result);
      assertEquals("{\"aProp\":\"aPropValue\",\"anotherProp\":null}",
              serialized);
    }
}
