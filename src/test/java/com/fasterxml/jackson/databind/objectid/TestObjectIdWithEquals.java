package com.fasterxml.jackson.databind.objectid;

import java.net.URI;
import java.util.*;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;

public class TestObjectIdWithEquals extends BaseMapTest
{
    @JsonPropertyOrder({"id","bars","otherBars"})
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

    // for [databind#1002]
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "uri")
    static class Element {
        public URI uri;
        public String name;

        @Override
        public boolean equals(Object object) {
            if (object == this) {
                return true;
            } else if (object == null || !(object instanceof Element)) {
                return false;
            } else {
                Element element = (Element) object;
                if (element.uri.toString().equalsIgnoreCase(this.uri.toString())) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public int hashCode() {
            return uri.hashCode();
        }
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, include = JsonTypeInfo.As.WRAPPER_OBJECT, property = "_class_to_override")
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "uri")
    static class Element3943 {
        public URI uri;
        public String name;

        @Override
        public boolean equals(Object object) {
            if (object == this) {
                return true;
            } else if (object == null || !(object instanceof Element3943)) {
                return false;
            } else {
                Element3943 element = (Element3943) object;
                if (element.uri.toString().equalsIgnoreCase(this.uri.toString())) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public int hashCode() {
            return uri.hashCode();
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

    public void testEqualObjectIdsExternal() throws Exception
    {
        Element element = new Element();
        element.uri = URI.create("URI");
        element.name = "Element1";

        Element element2 = new Element();
        element2.uri = URI.create("URI");
        element2.name = "Element2";

        // 12-Nov-2015, tatu: array works fine regardless of Type Erasure, but if using List,
        //   must provide additional piece of type info
//        Element[] input = new Element[] { element, element2 };
        List<Element> input = Arrays.asList(element, element2);

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.USE_EQUALITY_FOR_OBJECT_ID);

//        String json = mapper.writeValueAsString(input);
        String json = mapper.writerFor(new TypeReference<List<Element>>() { })
                .writeValueAsString(input);

        Element[] output = mapper.readValue(json, Element[].class);
        assertNotNull(output);
        assertEquals(2, output.length);
    }

    // [databind#3943] Add config-override system for JsonTypeInfo.Value
    public void testEqualObjectIdsExternalWithOverrides() throws Exception
    {
        Element3943 element = new Element3943();
        element.uri = URI.create("URI");
        element.name = "Element39431";

        Element3943 element2 = new Element3943();
        element2.uri = URI.create("URI");
        element2.name = "Element39432";

        // 12-Nov-2015, tatu: array works fine regardless of Type Erasure, but if using List,
        //   must provide additional piece of type info
//        Element3943[] input = new Element3943[] { element, element2 };
        List<Element3943> input = Arrays.asList(element, element2);

        final JsonTypeInfo.Value typeInfo = JsonTypeInfo.Value.construct(JsonTypeInfo.Id.CLASS, JsonTypeInfo.As.PROPERTY,
                "@class", null, false, null);
        ObjectMapper mapper = jsonMapperBuilder()
                .enable(SerializationFeature.USE_EQUALITY_FOR_OBJECT_ID)
                .withConfigOverride(Element3943.class, cfg -> cfg.setPolymorphicTypeHandling(typeInfo)).build();
                
                

//        String json = mapper.writeValueAsString(input);
        String json = mapper.writerFor(new TypeReference<List<Element3943>>() { })
                .writeValueAsString(input);

        Element3943[] output = mapper.readValue(json, Element3943[].class);
        assertNotNull(output);
        assertEquals(2, output.length);
    }
}
