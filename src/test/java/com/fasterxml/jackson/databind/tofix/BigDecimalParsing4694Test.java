package com.fasterxml.jackson.databind.tofix;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.Assert.assertEquals;

public class BigDecimalParsing4694Test extends DatabindTestUtil
{
    private final String BIG_DEC_STR;
    {
        StringBuilder sb = new StringBuilder("-1234.");
        // Above 500 chars we get a problem:
        for (int i = 520; --i >= 0; ) {
            sb.append('0');
        }
        BIG_DEC_STR = sb.toString();
    }
    private final BigDecimal BIG_DEC = new BigDecimal(BIG_DEC_STR);

    private final ObjectMapper MAPPER = newJsonMapper();

    // [databind#4694]: decoded wrong by jackson-core/FDP for over 500 char numbers
    @Test
    public void bigDecimal4694FromString() throws Exception
    {
        assertEquals(BIG_DEC, MAPPER.readValue(BIG_DEC_STR, BigDecimal.class));
    }

    @Test
    public void bigDecimal4694FromBytes() throws Exception
    {
        byte[] b = utf8Bytes(BIG_DEC_STR);
        assertEquals(BIG_DEC, MAPPER.readValue(b, 0, b.length, BigDecimal.class));
    }
}
