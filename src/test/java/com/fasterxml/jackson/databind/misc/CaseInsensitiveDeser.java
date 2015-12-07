package com.fasterxml.jackson.databind.misc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;

public class CaseInsensitiveDeser extends BaseMapTest
{
    // [databind#1036]
    static class BaseResponse {
        public int errorCode;
        public String debugMessage;

        /*
        public String getDebugMessage() {
            return debugMessage;
        }

        public void setDebugMessage(String debugMessage) {
            this.debugMessage = debugMessage;
        }

        public int getErrorCode() {
            return errorCode;
        }

        public void setErrorCode(int errorCode) {
            this.errorCode = errorCode;
        }
        */
    }

    static class Issue476Bean {
        public Issue476Type value1, value2;
    }
    static class Issue476Type {
        public String name, value;
    }

    /*
    /********************************************************
    /* Test methods
    /********************************************************
     */
    
    // [databind#566]
    public void testCaseInsensitiveDeserialization() throws Exception
    {
     final String JSON = "{\"Value1\" : {\"nAme\" : \"fruit\", \"vALUe\" : \"apple\"}, \"valUE2\" : {\"NAME\" : \"color\", \"value\" : \"red\"}}";
        
        // first, verify default settings which do not accept improper case
        ObjectMapper mapper = new ObjectMapper();
        assertFalse(mapper.isEnabled(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES));
        
        try {
            mapper.readValue(JSON, Issue476Bean.class);
            
            fail("Should not accept improper case properties by default");
        } catch (JsonProcessingException e) {
            verifyException(e, "Unrecognized field");
            assertValidLocation(e.getLocation());
        }

        // Definitely not OK to enable dynamically - the BeanPropertyMap (which is the consumer of this particular feature) gets cached.
        mapper = new ObjectMapper();
        mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
        ObjectReader r = mapper.readerFor(Issue476Bean.class);
        Issue476Bean result = r.readValue(JSON);
        assertEquals(result.value1.name, "fruit");
        assertEquals(result.value1.value, "apple");
    }

    // [databind#1036]
    public void testCaseInsensitive1036() throws Exception
    {
        final String json = "{\"ErrorCode\":2,\"DebugMessage\":\"Signature not valid!\"}";
//        final String json = "{\"errorCode\":2,\"debugMessage\":\"Signature not valid!\"}";

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES);
        BaseResponse response = mapper.readValue(json, BaseResponse.class);
        assertEquals(2, response.errorCode);
        assertEquals("Signature not valid!", response.debugMessage);
    }
}
