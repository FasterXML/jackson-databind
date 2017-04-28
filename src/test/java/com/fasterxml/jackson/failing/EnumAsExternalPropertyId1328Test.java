package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;

// Test for [databind#1328]; does not actually reproduce the issue at this point.
public class EnumAsExternalPropertyId1328Test extends BaseMapTest
{
    static class Bean1328 {
        public Type1328 type;

        @JsonTypeInfo(property = "type", include = JsonTypeInfo.As.EXTERNAL_PROPERTY, use = JsonTypeInfo.Id.NAME,
                visible=true)
        @JsonSubTypes({
                  @JsonSubTypes.Type(value = A.class,name = "A"),
                  @JsonSubTypes.Type(value = B.class,name = "B")
        })
        public Interface1328 obj;
    }

    static interface Interface1328 { }

    enum Type1328 {
        A, B;
    }

   static class A implements Interface1328 {
       public int a;
   }

   static class B implements Interface1328 {
       public int b;
   }

   /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = new ObjectMapper();

    public void testExternalTypeIdAsEnum() throws Exception
    {
        final String JSON = aposToQuotes("{ 'type':'A', 'obj': { 'a' : 4 } }'");
        Bean1328 result = MAPPER.readValue(JSON, Bean1328.class);
        assertNotNull(result.obj);
        assertSame(A.class, result.obj.getClass());
        assertEquals(4, ((A) result.obj).a);

        assertSame(Type1328.A, result.type);
    }
}
