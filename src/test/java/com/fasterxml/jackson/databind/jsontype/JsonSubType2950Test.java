package com.fasterxml.jackson.databind.jsontype;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonSubType2950Test extends BaseMapTest {
    
    /*
    /**********************************************************
    /* Set up
    /**********************************************************
    */

    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            property = "#type")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = SubDuperAnnotated.class, name = "sub1"),
            @JsonSubTypes.Type(value = SubDuperAnnotated.class, name = "sub2")
    })
    static class SuperDuper {
    }

    static class SubDuperAnnotated extends SuperDuper {
        public int a;

        public SubDuperAnnotated(int a) {
            this.a = a;
        }
    }

    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            property = "#type")
    static class Mover {
    }

    static class SubMoverRegistered extends Mover {
        public int a;

        public SubMoverRegistered(int a) {
            this.a = a;
        }
    }

    /*
    /**********************************************************
    /* Tet
    /**********************************************************
    */

    // annotated serialization : the first registered annotated subtype name is used for serialization
    public void testAnnotatedSubytpesWorksBothDeserialization() throws Exception {
        ObjectMapper mapper = newJsonMapper();

        SubDuperAnnotated sub = new SubDuperAnnotated(15);
        
        assertEquals("{\"#type\":\"sub1\",\"a\":15}", mapper.writeValueAsString(sub));
    }

    // annotated deserialization : both type names work
    public void testAnnotatedSubtypesFailsFirst() throws Exception {
        ObjectMapper mapper = newJsonMapper();

        SubDuperAnnotated sub1 =
                (SubDuperAnnotated) mapper.readValue("{\"#type\":\"sub1\",\"a\":15}", SuperDuper.class);

        assertEquals(15, sub1.a);
    }

    // annotated deserialization : both type names work
    public void testAnnotatedSubtypesFailsSecond() throws Exception {
        ObjectMapper mapper = newJsonMapper();

        SubDuperAnnotated sub2 =
                (SubDuperAnnotated) mapper.readValue("{\"#type\":\"sub2\",\"a\":15}", SuperDuper.class);

        assertEquals(15, sub2.a);
    }

    // serialization : the first registered annotated subtype name is used for serialization
    public void testRegisteredSubytpesWorksBothSerialization() throws Exception {
        ObjectMapper mapper = newJsonMapper();
        mapper.registerSubtypes(new NamedType(SubMoverRegistered.class, "sub1"));
        mapper.registerSubtypes(new NamedType(SubMoverRegistered.class, "sub2"));

        SubMoverRegistered sub = new SubMoverRegistered(15);
        
        assertEquals("{\"#type\":\"sub1\",\"a\":15}", mapper.writeValueAsString(sub));
    }

    // registered deserialization : both type names work
    public void testRegisteredSubtypesFailsFirst() throws Exception {
        ObjectMapper mapper = newJsonMapper();
        mapper.registerSubtypes(new NamedType(SubMoverRegistered.class, "sub1"));
        mapper.registerSubtypes(new NamedType(SubMoverRegistered.class, "sub2"));

        SubMoverRegistered sub1 = (SubMoverRegistered) mapper.readValue("{\"#type\":\"sub1\",\"a\":15}", Mover.class);
        
        assertEquals(15, sub1.a);
    }

    // registered deserialization : both type names work
    public void testRegisteredSubtypesFailsSecond() throws Exception {
        ObjectMapper mapper = newJsonMapper();
        mapper.registerSubtypes(new NamedType(SubMoverRegistered.class, "sub1"));
        mapper.registerSubtypes(new NamedType(SubMoverRegistered.class, "sub2"));

        SubMoverRegistered sub2 = (SubMoverRegistered) mapper.readValue("{\"#type\":\"sub2\",\"a\":15}", Mover.class);
        
        assertEquals(15, sub2.a);
    }

    // serialization : the first registered annotated subtype name is used for serialization
    public void testRegisteredSubytpesWorksBothSerializationReverse() throws Exception {
        ObjectMapper mapper = newJsonMapper();
        mapper.registerSubtypes(new NamedType(SubMoverRegistered.class, "sub2"));
        mapper.registerSubtypes(new NamedType(SubMoverRegistered.class, "sub1"));

        SubMoverRegistered sub = new SubMoverRegistered(15);
        assertEquals("{\"#type\":\"sub2\",\"a\":15}", mapper.writeValueAsString(sub));
    }

    // registered deserialization : both type names work
    public void testRegisteredSubtypesFailsFirstReverse() throws Exception {
        ObjectMapper mapper = newJsonMapper();
        mapper.registerSubtypes(new NamedType(SubMoverRegistered.class, "sub2"));
        mapper.registerSubtypes(new NamedType(SubMoverRegistered.class, "sub1"));

        SubMoverRegistered sub1 = (SubMoverRegistered) mapper.readValue("{\"#type\":\"sub1\",\"a\":15}", Mover.class);
        assertEquals(15, sub1.a);
    }

    // registered deserialization : both type names work
    public void testRegisteredSubtypesFailsSecondReverse() throws Exception {
        ObjectMapper mapper = newJsonMapper();
        mapper.registerSubtypes(new NamedType(SubMoverRegistered.class, "sub2"));
        mapper.registerSubtypes(new NamedType(SubMoverRegistered.class, "sub1"));
        
        SubMoverRegistered sub2 = (SubMoverRegistered) mapper.readValue("{\"#type\":\"sub2\",\"a\":15}", Mover.class);
        assertEquals(15, sub2.a);
    }
}
