package com.fasterxml.jackson.databind.ser.filter;

import java.util.*;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonStreamContext;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.databind.ser.PropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;

// [databind#3160]
public class CurrentObject3160Test extends BaseMapTest
{
    @JsonFilter("myFilter")
    @JsonPropertyOrder({ "id", "strategy", "set" })
    public static class Item3160 {
        public Collection<String> set;
        public Strategy strategy;
        public String id;

        public Item3160(Collection<String> set, String id) {
            this.set = set;
            this.strategy = new Foo(42);
            this.id = id;
        }
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
    @JsonSubTypes({ @JsonSubTypes.Type(name = "Foo", value = Foo.class) })
    interface Strategy { }

    static class Foo implements Strategy {
        public int foo;

        @JsonCreator
        Foo(@JsonProperty("foo") int foo) {
            this.foo = foo;
        }
    }

    // from [databind#2475] test/filter
    static class MyFilter3160 extends SimpleBeanPropertyFilter {
        @Override
        public void serializeAsField(Object pojo, JsonGenerator jgen, SerializerProvider provider, PropertyWriter writer) throws Exception {
            // Ensure that "current value" remains pojo
            final JsonStreamContext ctx = jgen.getOutputContext();
            final Object curr = ctx.getCurrentValue();

            if (!(curr instanceof Item3160)) {
                throw new RuntimeException("Field '"+writer.getName()
                    +"', context not that of `Item3160` instance but: "+curr.getClass().getName());
            }
            super.serializeAsField(pojo, jgen, provider, writer);
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    // [databind#2475]
    public void testIssue2475() throws Exception
    {
        SimpleFilterProvider provider = new SimpleFilterProvider().addFilter("myFilter",
                new MyFilter3160());
        ObjectWriter writer = MAPPER.writer(provider);

        // contents don't really matter that much as verification within filter but... let's
        // check anyway
        assertEquals(a2q("{'id':'ID-1','strategy':{'type':'Foo','foo':42},'set':[]}"),
               writer.writeValueAsString(new Item3160(Arrays.asList(), "ID-1")));

        assertEquals(a2q("{'id':'ID-2','strategy':{'type':'Foo','foo':42},'set':[]}"),
               writer.writeValueAsString(new Item3160(Collections.emptySet(), "ID-2")));
    }
}
