package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;

public class TestObjectIdWithInjectables538 extends BaseMapTest
{
    @JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class)
    public static class A {
        public B b;

        public A(@JacksonInject("i1") String injected) {}
    }

    @JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class)
    public static class B {
        public A a;

        public B(@JacksonInject("i2") String injected) {}
    } 

    /*
    /*****************************************************
    /* Test methods
    /*****************************************************
     */
    
    private final ObjectMapper MAPPER = new ObjectMapper();

    public void testWithInjectables538() throws Exception
    {
        A a = new A(null);
        B b = new B(null);
        a.b = b;
        b.a = a;

        String json = MAPPER.writeValueAsString(a);

System.out.println("JSON = "+json);
        
        InjectableValues.Std inject = new InjectableValues.Std();
        inject.addValue("i1", "e1");
        inject.addValue("i2", "e2");
        A output = MAPPER.reader(inject).withType(A.class).readValue(json);
        assertNotNull(output);

        assertNotNull(output.b);
    }
}

