package com.fasterxml.jackson.databind.jsontype.ext;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.databind.*;

public class MultipleExternalIds291Test extends BaseMapTest
{
    // For [Issue#291]
    interface F1 {}

    static class A implements F1 {
        public String a;
    }

    static class B implements F1 {
        public String b;
    }

    static interface F2 {}

    static class C implements F2 {
        public String c;
    }

    static class D implements F2{
        public String d;
    }

    static class Container {
        @JsonTypeInfo(use = Id.NAME, property = "type", include = As.EXTERNAL_PROPERTY)
        @JsonSubTypes({
                @JsonSubTypes.Type(value = A.class, name = "1"),
                @JsonSubTypes.Type(value = B.class, name = "2")})
        public F1 field1;

        @JsonTypeInfo(use = Id.NAME, property = "type", include = As.EXTERNAL_PROPERTY)
        @JsonSubTypes({
                @JsonSubTypes.Type(value = C.class, name = "1"),
                @JsonSubTypes.Type(value = D.class, name = "2")})
        public F2 field2;
    }

    static class ContainerWithExtra extends Container {
        public String type;
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    final ObjectMapper MAPPER = objectMapper();

    // [databind#291]
    public void testMultipleValuesSingleExtId() throws Exception
    {
        // first with ext-id before values
        _testMultipleValuesSingleExtId(
"{'type' : '1',\n"
+"'field1' : { 'a' : 'AAA' },\n"
+"'field2' : { 'c' : 'CCC' }\n"
+"}"
);

        // then after
        _testMultipleValuesSingleExtId(
"{\n"
+"'field1' : { 'a' : 'AAA' },\n"
+"'field2' : { 'c' : 'CCC' },\n"
+"'type' : '1'\n"
+"}"
);
        // and then in-between
        _testMultipleValuesSingleExtId(
"{\n"
+"'field1' : { 'a' : 'AAA' },\n"
+"'type' : '1',\n"
+"'field2' : { 'c' : 'CCC' }\n"
+"}"
);
    }

    public void _testMultipleValuesSingleExtId(String json) throws Exception
    {
        json = a2q(json);

        // First, with base class, no type id field separately
        {
            Container c = MAPPER.readValue(json, Container.class);
            assertNotNull(c);
            assertTrue(c.field1 instanceof A);
            assertEquals("AAA", ((A) c.field1).a);
            assertTrue(c.field2 instanceof C);
            assertEquals("CCC", ((C) c.field2).c);
        }

        // then with sub-class that does have similarly named property
        {
            ContainerWithExtra c = MAPPER.readValue(json, ContainerWithExtra.class);
            assertNotNull(c);
            assertEquals("1", c.type);
            assertTrue(c.field1 instanceof A);
            assertEquals("AAA", ((A) c.field1).a);
            assertTrue(c.field2 instanceof C);
            assertEquals("CCC", ((C) c.field2).c);
        }
    }
}
