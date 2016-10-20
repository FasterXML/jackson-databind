package com.fasterxml.jackson.databind.util;

import com.fasterxml.jackson.databind.BaseMapTest;

public class BeanUtilTest extends BaseMapTest
{
    public void testNameMangle()
    {
        assertEquals("foo", BeanUtil.legacyManglePropertyName("getFoo", 3));
        assertEquals("foo", BeanUtil.stdManglePropertyName("getFoo", 3));

        assertEquals("url", BeanUtil.legacyManglePropertyName("getURL", 3));
        assertEquals("URL", BeanUtil.stdManglePropertyName("getURL", 3));
    }
}
