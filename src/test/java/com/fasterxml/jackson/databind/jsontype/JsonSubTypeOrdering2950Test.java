package com.fasterxml.jackson.databind.jsontype;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

public class JsonSubTypeOrdering2950Test extends BaseMapTest {
    
    /*
    /**********************************************************
    /* Set up
    /**********************************************************
    */

    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            property = "_type"
    )
    @JsonSubTypes({
            @JsonSubTypes.Type(value = DuperAnnotated.class, name = "sub1"),
            @JsonSubTypes.Type(value = DuperAnnotated.class, name = "sub2")
    })
    static interface SuperDuper {
    }

    static class DuperAnnotated implements SuperDuper {
        public int a;
    }

    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            property = "_type")
    static abstract class Mover {
    }

    static class SubMoverRegistered extends Mover {
        public int a;
    }
    

    /*
    /**********************************************************
    /* Tests
    /**********************************************************
    */

    public void testAnnotatedSubytpesSerialization() throws Exception {
        ObjectMapper mapper = newJsonMapper();

        DuperAnnotated mover = new DuperAnnotated();
        mover.a = 15;
        String jsonStr = mapper.writeValueAsString(mover);

        assertEquals(a2q("{'_type':'sub1','a':15}"), jsonStr);
    }

    public void testRegisteredSubytpesSerializationOrdered() throws Exception {
        ObjectMapper mapper = newJsonMapper();
        mapper.registerSubtypes(new NamedType(SubMoverRegistered.class, "sub1"));
        mapper.registerSubtypes(new NamedType(SubMoverRegistered.class, "sub2"));

        SubMoverRegistered mover = new SubMoverRegistered();
        mover.a = 15;
        String jsonStr = mapper.writeValueAsString(mover);

        assertEquals(a2q("{'_type':'sub1','a':15}"), jsonStr);
    }

    public void testRegisteredSubytpesSerializationOrderedReverse() throws Exception {
        ObjectMapper mapper = newJsonMapper();
        mapper.registerSubtypes(new NamedType(SubMoverRegistered.class, "sub2"));
        mapper.registerSubtypes(new NamedType(SubMoverRegistered.class, "sub1"));

        SubMoverRegistered mover = new SubMoverRegistered();
        mover.a = 15;
        String jsonStr = mapper.writeValueAsString(mover);

        assertEquals(a2q("{'_type':'sub2','a':15}"), jsonStr);
    }

    // annotated deserialization : both type names work
    public void testAnnotatedSubtypesFailsFirst() throws Exception {
        ObjectMapper mapper = newJsonMapper();

        DuperAnnotated sub1 =
                (DuperAnnotated) mapper.readValue("{\"_type\":\"sub1\",\"a\":15}", SuperDuper.class);

        assertEquals(15, sub1.a);
    }

    // annotated deserialization : both type names work
    public void testAnnotatedSubtypesFailsFirst2() throws Exception {
        ObjectMapper mapper = JsonMapper.builder().disable(MapperFeature.REQUIRE_TYPE_ID_FOR_SUBTYPES).build();

        DuperAnnotated sub1 =
                (DuperAnnotated) mapper.readValue("{\"_type\":\"sub1\",\"a\":15}", SuperDuper.class);

        assertEquals(15, sub1.a);
    }

    // annotated deserialization : both type names work
    public void testAnnotatedSubtypesFailsSecond() throws Exception {
        ObjectMapper mapper = newJsonMapper();

        DuperAnnotated sub2 =
                (DuperAnnotated) mapper.readValue("{\"_type\":\"sub2\",\"a\":15}", SuperDuper.class);

        assertEquals(15, sub2.a);
    }

    // registered deserialization : both type names work
    public void testRegisteredSubtypesFailsFirst() throws Exception {
        ObjectMapper mapper = newJsonMapper();
        mapper.registerSubtypes(new NamedType(SubMoverRegistered.class, "sub1"));
        mapper.registerSubtypes(new NamedType(SubMoverRegistered.class, "sub2"));

        SubMoverRegistered sub1 = (SubMoverRegistered) mapper.readValue("{\"_type\":\"sub1\",\"a\":15}", Mover.class);

        assertEquals(15, sub1.a);
    }

    // registered deserialization : both type names work
    public void testRegisteredSubtypesFailsSecond() throws Exception {
        ObjectMapper mapper = newJsonMapper();
        mapper.registerSubtypes(new NamedType(SubMoverRegistered.class, "sub1"));
        mapper.registerSubtypes(new NamedType(SubMoverRegistered.class, "sub2"));

        SubMoverRegistered sub2 = (SubMoverRegistered) mapper.readValue("{\"_type\":\"sub2\",\"a\":15}", Mover.class);

        assertEquals(15, sub2.a);
    }

    // registered deserialization : both type names work
    public void testRegisteredSubtypesFailsFirstReverse() throws Exception {
        ObjectMapper mapper = newJsonMapper();
        mapper.registerSubtypes(new NamedType(SubMoverRegistered.class, "sub2"));
        mapper.registerSubtypes(new NamedType(SubMoverRegistered.class, "sub1"));

        SubMoverRegistered sub1 = (SubMoverRegistered) mapper.readValue("{\"_type\":\"sub1\",\"a\":15}", Mover.class);
        assertEquals(15, sub1.a);
    }

    // registered deserialization : both type names work
    public void testRegisteredSubtypesFailsSecondReverse() throws Exception {
        ObjectMapper mapper = newJsonMapper();
        mapper.registerSubtypes(new NamedType(SubMoverRegistered.class, "sub2"));
        mapper.registerSubtypes(new NamedType(SubMoverRegistered.class, "sub1"));

        SubMoverRegistered sub2 = (SubMoverRegistered) mapper.readValue("{\"_type\":\"sub2\",\"a\":15}", Mover.class);
        assertEquals(15, sub2.a);
    }
}
