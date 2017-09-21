package com.fasterxml.jackson.databind.introspect;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;

public class TestAutoDetect
    extends BaseMapTest
{
    // 21-Sep-2017, tatu: With 2.x, private delegating ctor was acceptable; with 3.x
    //    must be non-private OR annotated
    static class ProtectedBean {
        String a;

        protected ProtectedBean(String a) { this.a = a; }
    }

    // Private scalar constructor ok, but only if annotated (or level changed)
    static class PrivateBeanAnnotated {
        String a;

        @JsonCreator
        private PrivateBeanAnnotated(String a) { this.a = a; }
    }

    static class PrivateBeanNonAnnotated {
        String a;
        private PrivateBeanNonAnnotated(String a) { this.a = a; }
    }
    
    // test for [databind#1347], config overrides for visibility
    @JsonPropertyOrder(alphabetic=true)
    static class Feature1347SerBean {
        public int field = 2;

        public int getValue() { return 3; }
    }

    // let's promote use of fields; but not block setters yet
    @JsonAutoDetect(fieldVisibility=Visibility.NON_PRIVATE)
    static class Feature1347DeserBean {
        int value;

        public void setValue(int x) {
            throw new IllegalArgumentException("Should NOT get called");
        }
    }

    /*
    /********************************************************
    /* Unit tests
    /********************************************************
     */

    private final ObjectMapper MAPPER = new ObjectMapper();

    public void testProtectedDelegatingCtor() throws Exception
    {
        // first, default settings, with which construction works ok
        ObjectMapper m = new ObjectMapper();
        ProtectedBean bean = m.readValue(quote("abc"), ProtectedBean.class);
        assertEquals("abc", bean.a);

        // then by increasing visibility requirement:
        m = new ObjectMapper();
        VisibilityChecker<?> vc = m.getVisibilityChecker();
        vc = vc.withScalarConstructorVisibility(JsonAutoDetect.Visibility.PUBLIC_ONLY);
        m.setVisibility(vc);
        try {
            m.readValue("\"abc\"", ProtectedBean.class);
            fail("Expected exception for missing constructor");
        } catch (JsonProcessingException e) {
            verifyException(e, InvalidDefinitionException.class, "no String-argument constructor/factory");
        }
    }

    public void testPrivateDelegatingCtor() throws Exception
    {
        // first, default settings, with which construction works ok
        ObjectMapper m = new ObjectMapper();
        PrivateBeanAnnotated bean = m.readValue(quote("abc"), PrivateBeanAnnotated.class);
        assertEquals("abc", bean.a);

        // but not so much without
        try {
            m.readValue("\"abc\"", PrivateBeanNonAnnotated.class);
            fail("Expected exception for missing constructor");
        } catch (JsonProcessingException e) {
            verifyException(e, InvalidDefinitionException.class, "no String-argument constructor/factory");
        }

        // except if we lower requirement
        m = new ObjectMapper();
        VisibilityChecker<?> vc = m.getVisibilityChecker();
        vc = vc.withScalarConstructorVisibility(JsonAutoDetect.Visibility.ANY);
        m.setVisibility(vc);
        bean = m.readValue(quote("xyz"), PrivateBeanAnnotated.class);
        assertEquals("xyz", bean.a);
    }

    // [databind#1347]
    public void testVisibilityConfigOverridesForSer() throws Exception
    {
        // first, by default, both field/method should be visible
        final Feature1347SerBean input = new Feature1347SerBean();
        assertEquals(aposToQuotes("{'field':2,'value':3}"),
                MAPPER.writeValueAsString(input));

        ObjectMapper mapper = new ObjectMapper();
        mapper.configOverride(Feature1347SerBean.class)
            .setVisibility(JsonAutoDetect.Value.construct(PropertyAccessor.GETTER,
                            Visibility.NONE));
        assertEquals(aposToQuotes("{'field':2}"),
                mapper.writeValueAsString(input));
    }

    // [databind#1347]
    public void testVisibilityConfigOverridesForDeser() throws Exception
    {
        final String JSON = aposToQuotes("{'value':3}");

        // by default, should throw exception
        try {
            /*Feature1347DeserBean bean =*/
            MAPPER.readValue(JSON, Feature1347DeserBean.class);
            fail("Should not pass");
        } catch (JsonMappingException e) {
            verifyException(e, "Should NOT get called");
        }

        // but when instructed to ignore setter, should work
        ObjectMapper mapper = new ObjectMapper();
        mapper.configOverride(Feature1347DeserBean.class)
            .setVisibility(JsonAutoDetect.Value.construct(PropertyAccessor.SETTER,
                        Visibility.NONE));
        Feature1347DeserBean result = mapper.readValue(JSON, Feature1347DeserBean.class);
        assertEquals(3, result.value);
    }
}
