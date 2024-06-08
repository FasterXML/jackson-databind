package com.fasterxml.jackson.databind.util;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonSerializable;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

public class RawValueTest extends DatabindTestUtil
{
    @Test
    public void testEquality()
    {
        RawValue raw1 = new RawValue("foo");
        RawValue raw1b = new RawValue("foo");
        RawValue raw2 = new RawValue("bar");

        assertTrue(raw1.equals(raw1));
        assertTrue(raw1.equals(raw1b));

        assertFalse(raw1.equals(raw2));
        assertFalse(raw1.equals(null));

        assertFalse(new RawValue((JsonSerializable) null).equals(raw1));

        assertNotNull(raw1.toString());
    }
}
