package com.fasterxml.jackson.databind.deser;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [databind#5355] Mapper in 3.0.0 does not match property with
//  second letter capitalized to getter/setter for certain naming convention
public class FIxFieldNameCase5355Test
    extends DatabindTestUtil
{
    public static class TestPojo {
        private String aProp;
        private String anotherProp;

        public String getaProp() {return aProp;}
        public void setaProp(String aProp) {this.aProp = aProp;}
        public String getAnotherProp() {return anotherProp;}
        public void setAnotherProp(String anotherProp) {this.anotherProp = anotherProp;}
    }

    @Test
    public void featureEnabled5355Test()
        throws Exception
    {
        ObjectMapper mapper = JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(MapperFeature.FIX_FIELD_NAME_UPPER_CASE_PREFIX)
                .build();

        String json = "{\"aProp\":\"aPropValue\", \"prop1\":\"prop1Value\"}";
        TestPojo result = mapper.readValue(json, TestPojo.class);
        assertEquals("aPropValue",result.getaProp()); //fails

        // org.opentest4j.AssertionFailedError:
        //Expected :{"aProp":"aPropValue", "prop1":"prop1Value"}
        //Actual   :{"aProp":"aPropValue","anotherProp":null}
        String serialized = mapper.writeValueAsString(result);
        assertEquals(json, serialized); //fails
    }

    @Test
    public void featureDisabled5355Test()
        throws Exception
    {
        ObjectMapper mapper = JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(MapperFeature.FIX_FIELD_NAME_UPPER_CASE_PREFIX)
                .build();

        String json = "{\"aProp\":\"aPropValue\", \"prop1\":\"prop1Value\"}";
        TestPojo result = mapper.readValue(json, TestPojo.class);

        // org.opentest4j.AssertionFailedError:
        //Expected :{"aProp":"aPropValue", "prop1":"prop1Value"}
        //Actual   :{"aProp":"aPropValue","anotherProp":null}
        String serialized = mapper.writeValueAsString(result);
        assertEquals(json, serialized); //fails
    }

}
