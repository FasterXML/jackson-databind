package com.fasterxml.jackson.databind.jsontype;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;

public class TestPolymorphicWithDefaultImpl1565 extends BaseMapTest
{
    // [databind#1565]
    @JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.PROPERTY,
        property="typeInfo",  defaultImpl = CBaseClass1565.class)
    @JsonSubTypes({
        @JsonSubTypes.Type(CDerived1565.class)
    })
    public static interface CTestInterface1565
    {
         public String getName();
         public void setName(String name);
         public String getTypeInfo();
    }

    static class CBaseClass1565 implements CTestInterface1565
    {
         private String mName;

         @Override
         public String getName() {
              return(mName);
         }

         @Override
         public void setName(String name) {
              mName = name;
         }

         @Override
         public String getTypeInfo() {
              return "base";
         }
    }

    @JsonTypeName("derived")
    static class CDerived1565 extends CBaseClass1565
    {
         public String description;

         @Override
         public String getTypeInfo() {
              return "derived";
         }
    }

    // [databind#1861]
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", defaultImpl = DefaultImpl1861.class)
    @JsonSubTypes({
            @JsonSubTypes.Type(name = "a", value = Impl1861A.class)
    })
    static abstract class Bean1861 {
        public String base;
    }

    static class DefaultImpl1861 extends Bean1861 {
        public int id;
    }

    static class Impl1861A extends Bean1861 {
        public int valueA;
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final ObjectMapper MAPPER = new ObjectMapper();

    // [databind#1565]
    public void testIncompatibleDefaultImpl1565() throws Exception
    {
        String value = "{\"typeInfo\": \"derived\", \"name\": \"John\", \"description\": \"Owner\"}";
        CDerived1565 result = MAPPER.readValue(value, CDerived1565.class);
        assertNotNull(result);
    }

    // [databind#1861]
    public void testWithIncompatibleTargetType1861() throws Exception
    {
        // Should allow deserialization even if `defaultImpl` incompatible
        Impl1861A result = MAPPER.readValue(a2q("{'type':'a','base':'foo','valueA':3}"),
                Impl1861A.class);
        assertNotNull(result);
    }
}
