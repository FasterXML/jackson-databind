package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.databind.*;

public class TestMultipleExternalIds extends BaseMapTest
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
        public String type;

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

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    // [Issue#291]
    public void testMultiple() throws Exception
    {
        final ObjectMapper mapper = objectMapper();
        final String JSON =
"{\"type\" : \"1\",\n"
+"\"field1\" : {\n"
+"  \"a\" : \"AAA\"\n"
+"}, \"field2\" : {\n"
+"  \"c\" : \"CCC\"\n"
+"}\n"
+"}";

        Container c = mapper.readValue(JSON, Container.class);
        assertNotNull(c);
        assertTrue(c.field1 instanceof A);
        assertTrue(c.field2 instanceof C);
    }
}
