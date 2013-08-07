package com.fasterxml.jackson.databind.struct;

import java.util.*;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;

public class TestObjectIdWithEquals extends BaseMapTest
{
    @JsonIdentityInfo(generator=ObjectIdGenerators.PropertyGenerator.class, property="id", scope=Foo.class)
    static class Foo {
        public int id;

        public List<Bar> bars = new ArrayList<Bar>();
        public List<Bar> otherBars = new ArrayList<Bar>();

        public Foo() { }
        public Foo(int i) { id = i; }
    }

    @JsonIdentityInfo(generator=ObjectIdGenerators.PropertyGenerator.class, property="id", scope=Bar.class)
    static class Bar
    {
        public int id;

        public Bar() { }
        public Bar(int i) {
            id = i;
        }
        
        @Override
        public int hashCode() {
            return id;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Bar)) {
                return false;
            }
            return ((Bar) obj).id == id;
        }
    }

    /*
    /******************************************************
    /* Test methods
    /******************************************************
     */

    public void testSimpleEquals() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        // Verify default state too
        assertFalse(mapper.isEnabled(SerializationFeature.USE_EQUALITY_FOR_OBJECT_ID));
        mapper.enable(SerializationFeature.USE_EQUALITY_FOR_OBJECT_ID);

        Foo foo = new Foo(1);

        Bar bar1 = new Bar(1);
        Bar bar2 = new Bar(2);
        // this is another bar which is supposed to be "equal" to bar1
        // due to the same ID and 
        // Bar class' equals() method will return true.
        Bar anotherBar1 = new Bar(1);

        foo.bars.add(bar1);
        foo.bars.add(bar2);
        // this anotherBar1 object will confuse the serializer.
        foo.otherBars.add(anotherBar1);
        foo.otherBars.add(bar2);

        String json = mapper.writeValueAsString(foo);
        assertEquals("{\"id\":1,\"bars\":[{\"id\":1},{\"id\":2}],\"otherBars\":[1,2]}", json);

        Foo foo2 = mapper.readValue(json, Foo.class);       
        assertNotNull(foo2);
        assertEquals(foo.id, foo2.id);
    }
}
