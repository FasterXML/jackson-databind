package com.fasterxml.jackson.databind.deser.jdk;

import static org.junit.jupiter.api.Assertions.assertThrows;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import java.util.Locale;

// [databind#4009] Locale "" is deserialised as NULL if ACCEPT_EMPTY_STRING_AS_NULL_OBJECT is true
public class LocaleDeser4009Test extends BaseMapTest 
{

    static class MyPojo {
        public String field;
    }
    final ObjectMapper mapper = newJsonMapper();
    
    final ObjectReader DISABLED_READER = mapper.reader()
            .without(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
    
    final ObjectReader ENABLED_READER = mapper.reader()
            .with(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);

    public void testPOJOWithFeatureDisabled() 
    {
        assertThrows(JsonProcessingException.class, () -> {
            DISABLED_READER.readValue("\"\"", MyPojo.class);
        });
    }

    public void testPOJOWithFeatureEnabled() throws Exception 
    {
        assertNull(ENABLED_READER.readValue("\"\"", MyPojo.class));
    }

    public void testLocaleWithFeatureDisabled() throws Exception 
    {
        assertEquals(Locale.ROOT, DISABLED_READER.readValue("\"\"", Locale.class));
    }

    public void testLocaleWithFeatureEnabled() throws Exception 
    {
        assertNull(ENABLED_READER.readValue("\"\"", Locale.class));
    }
}
