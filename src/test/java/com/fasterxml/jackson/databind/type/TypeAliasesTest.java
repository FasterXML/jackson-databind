package com.fasterxml.jackson.databind.type;

import java.util.*;

import com.fasterxml.jackson.databind.*;

/**
 * Unit tests for more complicated type definitions where type name
 * aliasing can confuse naive resolution algorithms.
 */
public class TypeAliasesTest
    extends BaseMapTest
{
    public static abstract class Base<T> {
        public T inconsequential = null;
    }

    public static abstract class BaseData<T> {
        public T dataObj;
    }

    public static class Child extends Base<Long> {
        public static class ChildData extends BaseData<List<String>> { }
    }

    /*
    /*******************************************************
    /* Unit tests
    /*******************************************************
     */

    // Reproducing [databind#743]
    public void testAliasResolutionIssue743() throws Exception
    {
        String s3 = "{\"dataObj\" : [ \"one\", \"two\", \"three\" ] }";
        ObjectMapper m = new ObjectMapper();

        Child.ChildData d = m.readValue(s3, Child.ChildData.class);
        assertNotNull(d.dataObj);
        assertEquals(3, d.dataObj.size());
    }
}
