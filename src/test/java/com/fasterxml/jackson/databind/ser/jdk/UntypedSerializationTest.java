package com.fasterxml.jackson.databind.ser.jdk;

import java.util.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This unit test suite tries verify simplest aspects of
 * "Native" java type mapper; basically that is can properly serialize
 * core JDK objects to JSON.
 */
public class UntypedSerializationTest
    extends BaseMapTest
{
    public void testFromArray()
        throws Exception
    {
        ArrayList<Object> doc = new ArrayList<Object>();
        doc.add("Elem1");
        doc.add(Integer.valueOf(3));
        Map<String,Object> struct = new LinkedHashMap<String, Object>();
        struct.put("first", Boolean.TRUE);
        struct.put("Second", new ArrayList<Object>());
        doc.add(struct);
        doc.add(Boolean.FALSE);

        ObjectMapper mapper = objectMapper();

        // loop more than once, just to ensure caching works ok (during second round)
        for (int i = 0; i < 3; ++i) {
            String str = mapper.writeValueAsString(doc);
            
            JsonParser p = mapper.createParser(str);
            assertEquals(JsonToken.START_ARRAY, p.nextToken());
            
            assertEquals(JsonToken.VALUE_STRING, p.nextToken());
            assertEquals("Elem1", getAndVerifyText(p));
            
            assertEquals(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(3, p.getIntValue());
            
            assertEquals(JsonToken.START_OBJECT, p.nextToken());
            assertEquals(JsonToken.FIELD_NAME, p.nextToken());
            assertEquals("first", getAndVerifyText(p));
            
            assertEquals(JsonToken.VALUE_TRUE, p.nextToken());
            assertEquals(JsonToken.FIELD_NAME, p.nextToken());
            assertEquals("Second", getAndVerifyText(p));
            
            if (p.nextToken() != JsonToken.START_ARRAY) {
                fail("Expected START_ARRAY: JSON == '"+str+"'");
            }
            assertEquals(JsonToken.END_ARRAY, p.nextToken());
            assertEquals(JsonToken.END_OBJECT, p.nextToken());
            
            assertEquals(JsonToken.VALUE_FALSE, p.nextToken());
            
            assertEquals(JsonToken.END_ARRAY, p.nextToken());
            assertNull(p.nextToken());
            p.close();
        }
    }

    public void testFromMap() throws Exception
    {
        LinkedHashMap<String,Object> doc = new LinkedHashMap<String,Object>();

        doc.put("a1", "\"text\"");
        doc.put("int", Integer.valueOf(137));
        doc.put("foo bar", Long.valueOf(1234567890L));

        ObjectMapper mapper = objectMapper();
        for (int i = 0; i < 3; ++i) {
            String str = mapper.writeValueAsString(doc);
            JsonParser p = mapper.createParser(str);
            
            assertEquals(JsonToken.START_OBJECT, p.nextToken());
            
            assertEquals(JsonToken.FIELD_NAME, p.nextToken());
            assertEquals("a1", getAndVerifyText(p));
            assertEquals(JsonToken.VALUE_STRING, p.nextToken());
            assertEquals("\"text\"", getAndVerifyText(p));
            
            assertEquals(JsonToken.FIELD_NAME, p.nextToken());
            assertEquals("int", getAndVerifyText(p));
            assertEquals(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(137, p.getIntValue());
            
            assertEquals(JsonToken.FIELD_NAME, p.nextToken());
            assertEquals("foo bar", getAndVerifyText(p));
            assertEquals(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(1234567890L, p.getLongValue());
            
            assertEquals(JsonToken.END_OBJECT, p.nextToken());

            assertNull(p.nextToken());
            p.close();
        }
    }
}
