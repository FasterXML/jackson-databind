package com.fasterxml.jackson.databind.util;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonSerializable;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

class RawValueTest extends DatabindTestUtil
{
    @Test
    void equality()
    {
        RawValue raw1 = new RawValue("foo");
        RawValue raw1b = new RawValue("foo");
        RawValue raw2 = new RawValue("bar");

        assertEquals(raw1, raw1);
        assertEquals(raw1, raw1b);

        assertNotEquals(raw1, raw2);
        assertNotEquals(null, raw1);

        assertNotEquals(new RawValue((JsonSerializable) null), raw1);

        assertNotNull(raw1.toString());
    }
}
