package com.fasterxml.jackson.databind.introspect;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;

/**
 * Unit tests verifying handling of potential and actual
 * conflicts, regarding property handling.
 */
public class TestPropertyConflicts extends BaseMapTest
{
    // For [JACKSON-694]: error message for conflicting getters sub-optimal
    static class BeanWithConflict
    {
        public int getX() { return 3; }
        public boolean getx() { return false; }
    }

    // [Issue#238]
    protected static class Getters1A
    {
        protected int value = 3;
        
        public int getValue() { return value+1; }
        public boolean isValue() { return false; }
    }

    // variant where order of declarations is reversed; to try to
    // ensure declaration order won't break things
    protected static class Getters1B
    {
        public boolean isValue() { return false; }

        protected int value = 3;
        
        public int getValue() { return value+1; }
    }

    protected static class InferingIntrospector extends JacksonAnnotationIntrospector
    {
        private static final long serialVersionUID = 1L;

        @Override
        public String findImplicitPropertyName(AnnotatedMember member) {
            String name = member.getName();
            if (name.startsWith("_")) {
                return name.substring(1);
            }
            return null;
        }
    }

    static class Infernal {
        public String _name() { return "foo"; }
        public String getName() { return "Bob"; }

        public void setStuff(String value) { ; // ok
        }

        public void _stuff(String value) {
            throw new UnsupportedOperationException();
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */
    
    // for [JACKSON-694]
    public void testFailWithDupProps() throws Exception
    {
        BeanWithConflict bean = new BeanWithConflict();
        try {
            String json = objectWriter().writeValueAsString(bean);
            fail("Should have failed due to conflicting accessor definitions; got JSON = "+json);
        } catch (JsonProcessingException e) {
            verifyException(e, "Conflicting getter definitions");
        }
    }        

    // [Issue#238]: ok to have getter, "isGetter"
    public void testRegularAndIsGetter() throws Exception
    {
        final ObjectWriter writer = objectWriter();
        
        // first, serialize without probs:
        assertEquals("{\"value\":4}", writer.writeValueAsString(new Getters1A()));
        assertEquals("{\"value\":4}", writer.writeValueAsString(new Getters1B()));

        // and similarly, deserialize
        ObjectMapper mapper = objectMapper();
        assertEquals(1, mapper.readValue("{\"value\":1}", Getters1A.class).value);
        assertEquals(2, mapper.readValue("{\"value\":2}", Getters1B.class).value);
    }

    public void testInferredNameConflictsWithGetters() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setAnnotationIntrospector(new InferingIntrospector());
        String json = mapper.writeValueAsString(new Infernal());
        assertEquals(aposToQuotes("{'name':'Bob'}"), json);
    }
    
    public void testInferredNameConflictsWithSetters() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setAnnotationIntrospector(new InferingIntrospector());
        Infernal inf = mapper.readValue(aposToQuotes("{'stuff':'Bob'}"), Infernal.class);
        assertNotNull(inf);
    }
}
