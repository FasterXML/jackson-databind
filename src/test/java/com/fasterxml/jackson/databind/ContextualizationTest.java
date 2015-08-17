package com.fasterxml.jackson.databind;

public class ContextualizationTest extends BaseMapTest
{
    static class Foo {
        public int a;
    }

    /*
    public void testSerialize() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        System.out.println("Step 1/s");
        String json = m.writeValueAsString(new Foo());
        assertNotNull(json);
        System.out.println("Step 2/s");
        json = m.writeValueAsString(new Foo());
        assertNotNull(json);
    }
    */

    public void testDeserialize() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        System.out.println("Step 1/d");
        Foo f = m.readValue("{\"a\":3}", Foo.class);
        assertNotNull(f);
        System.out.println("Step 2/d");
        f = m.readValue("{\"a\":3}", Foo.class);
        assertNotNull(f);
    }
}
