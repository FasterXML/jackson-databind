package com.fasterxml.jackson.databind.ser.jdk;

import java.math.BigDecimal;
import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BigDecimalAsString2519Test extends DatabindTestUtil
{
    static class Bean2519Typed {
        public List<BigDecimal> values = new ArrayList<>();
    }

    static class Bean2519Untyped {
        public Collection<BigDecimal> values = new HashSet<>();
    }

    @Test
    public void testBigDecimalAsString2519Typed() throws Exception
    {
        Bean2519Typed foo = new Bean2519Typed();
        foo.values.add(new BigDecimal("2.34"));
        final ObjectMapper mapper = newJsonMapper();
        mapper.configOverride(BigDecimal.class)
            .setFormat(JsonFormat.Value.forShape(JsonFormat.Shape.STRING));
        String json = mapper.writeValueAsString(foo);
        assertEquals(a2q("{'values':['2.34']}"), json);
    }

    @Test
    public void testBigDecimalAsString2519Untyped() throws Exception
    {
        Bean2519Untyped foo = new Bean2519Untyped();
        foo.values.add(new BigDecimal("2.34"));
        final ObjectMapper mapper = newJsonMapper();
        mapper.configOverride(BigDecimal.class)
            .setFormat(JsonFormat.Value.forShape(JsonFormat.Shape.STRING));
        String json = mapper.writeValueAsString(foo);
        assertEquals(a2q("{'values':['2.34']}"), json);
    }
}
