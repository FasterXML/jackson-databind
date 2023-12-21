package com.fasterxml.jackson.databind.format;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;

import java.math.BigDecimal;

import java.util.Locale;
import java.util.TimeZone;

public class NumberFormatTest extends BaseMapTest
{
    protected static class NumberWrapper {

        public BigDecimal value;

        public NumberWrapper() {}
        public NumberWrapper(BigDecimal v) { value = v; }
    }

    public void testTypeDefaults() throws Exception
    {
        ObjectMapper mapper = newJsonMapper();
        mapper.configOverride(BigDecimal.class)
            .setFormat(new JsonFormat.Value("00,000.00", JsonFormat.Shape.STRING, (Locale) null, (TimeZone) null, null, null));
        String json = mapper.writeValueAsString(new NumberWrapper(new BigDecimal("1234")));
        assertEquals(a2q("{'value':'01,234.00'}"), json);

        // and then read back is not supported yet.
        /*NumberWrapper w = mapper.readValue(a2q("{'value':'01,234.00'}"), NumberWrapper.class);
        assertNotNull(w);
        assertEquals(new BigDecimal("1234"), w.value);*/
    }

    protected static class InvalidPatternWrapper {
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "#,##0.#.#")
        public BigDecimal value;

        public InvalidPatternWrapper(BigDecimal value) {
            this.value = value;
        }
    }

    public void testInvalidPattern() throws Exception {
        ObjectMapper mapper = newJsonMapper();
        Assert.assertThrows(JsonMappingException.class, () -> {
            mapper.writeValueAsString(new InvalidPatternWrapper(BigDecimal.ZERO));
        });
    }
}
