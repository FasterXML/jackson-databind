package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;

public class TestObjectId154 extends BaseMapTest
{
    @JsonIdentityInfo(generator=ObjectIdGenerators.IntSequenceGenerator.class, property="@id")
    public static class BaseEntity {  }

    @JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
    public static class Foo extends BaseEntity {
        public BaseEntity ref;
    }

    public static class Bar extends BaseEntity
    {
        public Foo next;
    }

    public void testObjectAndTypeId() throws Exception
    {
        final ObjectMapper mapper = new ObjectMapper();

        Bar bar = new Bar();
        Foo foo = new Foo();
        bar.next = foo;
        foo.ref = bar;

        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(bar);

        System.out.println("JSON: "+json);
        
        BaseEntity result = mapper.readValue(json, BaseEntity.class);
        assertNotNull(result);
        assertTrue(result instanceof Bar);
        Bar first = (Bar) result;

        assertNotNull(first.next);
        assertTrue(first.next instanceof Foo);
        Foo second = (Foo) first.next;
        assertNotNull(second.ref);
        assertSame(first, second.ref);
    }
}
