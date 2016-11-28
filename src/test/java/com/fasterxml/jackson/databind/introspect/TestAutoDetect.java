package com.fasterxml.jackson.databind.introspect;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;

public class TestAutoDetect
    extends BaseMapTest
{
    static class PrivateBean {
        String a;

        private PrivateBean() { }

        private PrivateBean(String a) { this.a = a; }
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
            throw new RuntimeException("Should NOT get called");
        }
    }

    /*
    /********************************************************
    /* Unit tests
    /********************************************************
     */

    private final ObjectMapper MAPPER = new ObjectMapper();

    public void testPrivateCtor() throws Exception
    {
        // first, default settings, with which construction works ok
        ObjectMapper m = new ObjectMapper();
        PrivateBean bean = m.readValue("\"abc\"", PrivateBean.class);
        assertEquals("abc", bean.a);

        // then by increasing visibility requirement:
        m = new ObjectMapper();
        VisibilityChecker<?> vc = m.getVisibilityChecker();
        vc = vc.withCreatorVisibility(JsonAutoDetect.Visibility.PUBLIC_ONLY);
        m.setVisibility(vc);
        try {
            m.readValue("\"abc\"", PrivateBean.class);
            fail("Expected exception for missing constructor");
        } catch (JsonProcessingException e) {
            verifyException(e, "no String-argument constructor/factory");
        }
    }

    // [databind#1347]
    public void testVisibilityConfigOverridesForSer() throws Exception
    {
        // first, by default, both field/method should be visible
        String json = MAPPER.writeValueAsString(new Feature1347SerBean());
        assertEquals(aposToQuotes("{'field':2,'value':3}"), json);

        ObjectMapper mapper = new ObjectMapper();
        mapper.configOverride(Feature1347SerBean.class)
        // !!! TODO
        ;
    }

    // [databind#1347]
    public void testVisibilityConfigOverridesForDeser() throws Exception
    {
    }
}
