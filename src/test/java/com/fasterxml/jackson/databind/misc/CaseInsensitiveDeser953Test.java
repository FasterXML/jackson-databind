package com.fasterxml.jackson.databind.misc;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.*;

public class CaseInsensitiveDeser953Test extends BaseMapTest
{
    static class Id953 {
        @JsonProperty("someId")
        public int someId;
    }

    private final Locale LOCALE_EN = new Locale("en", "US");

    private final ObjectMapper INSENSITIVE_MAPPER_EN = jsonMapperBuilder()
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
            .defaultLocale(LOCALE_EN)
            .build();

    private final Locale LOCALE_TR = new Locale("tr", "TR");

    private final ObjectMapper INSENSITIVE_MAPPER_TR = jsonMapperBuilder()
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
            .defaultLocale(LOCALE_TR)
            .build();

    public void testTurkishILetterDeserializationWithEn() throws Exception {
        _testTurkishILetterDeserialization(INSENSITIVE_MAPPER_EN, LOCALE_EN);
    }

    public void testTurkishILetterDeserializationWithTr() throws Exception {
        _testTurkishILetterDeserialization(INSENSITIVE_MAPPER_TR, LOCALE_TR);
    }

    private void _testTurkishILetterDeserialization(ObjectMapper mapper, Locale locale) throws Exception
    {
        // Sanity check first
        assertEquals(locale, mapper.getDeserializationConfig().getLocale());

        final String ORIGINAL_KEY = "someId";

        Id953 result;
        result = mapper.readValue("{\""+ORIGINAL_KEY+"\":1}", Id953.class);
        assertEquals(1, result.someId);

        result = mapper.readValue("{\""+ORIGINAL_KEY.toUpperCase(locale)+"\":1}", Id953.class);
        assertEquals(1, result.someId);

        result = mapper.readValue("{\""+ORIGINAL_KEY.toLowerCase(locale)+"\":1}", Id953.class);
        assertEquals(1, result.someId);

        // and finally round-trip too...
        final Id953 input = new Id953();
        input.someId = 1;
        final String json = mapper.writeValueAsString(input);

        result = mapper.readValue(json, Id953.class);
        assertEquals(1, result.someId);
    }
}
