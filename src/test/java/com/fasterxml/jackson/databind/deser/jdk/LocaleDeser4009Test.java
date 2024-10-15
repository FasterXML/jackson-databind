package com.fasterxml.jackson.databind.deser.jdk;

import java.util.Locale;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.newJsonMapper;

// [databind#4009] Locale "" is deserialised as NULL if ACCEPT_EMPTY_STRING_AS_NULL_OBJECT is true
public class LocaleDeser4009Test
{
    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testLocaleWithFeatureDisabled() throws Exception 
    {
        assertEquals(Locale.ROOT,
                MAPPER.readerFor(Locale.class)
                    .without(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
                        .readValue("\"\""));
    }

    @Test
    public void testLocaleWithFeatureEnabled() throws Exception 
    {
        // 06-Jul-2023, tatu: as per [databind#4009] should not become 'null'
        //   just because
        assertEquals(Locale.ROOT,
            MAPPER.readerFor(Locale.class)
                .with(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
                    .readValue("\"\""));
    }
}
