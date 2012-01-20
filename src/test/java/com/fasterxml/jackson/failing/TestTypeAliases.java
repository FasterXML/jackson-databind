package com.fasterxml.jackson.failing;

import java.util.*;

import com.fasterxml.jackson.databind.*;

public class TestTypeAliases
    extends BaseMapTest
{

    // Helper types for [JACKSON-743]
    
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


    // Reproducing issue 743
    public void testResolution743() throws Exception
    {
        String s3 = "{\"dataObj\" : [ \"one\", \"two\", \"three\" ] }";
        ObjectMapper m = new ObjectMapper();
   
        Child.ChildData d = m.readValue(s3, Child.ChildData.class);
        assertNotNull(d.dataObj);
        assertEquals(3, d.dataObj.size());
    }
}
