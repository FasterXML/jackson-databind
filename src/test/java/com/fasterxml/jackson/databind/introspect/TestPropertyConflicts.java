package com.fasterxml.jackson.databind.introspect;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;

/**
 * Unit tests verifying handling of potential and actual
 * conflicts, regarding property handling.
 */
public class TestPropertyConflicts extends BaseMapTest
{
    // error message for conflicting getters sub-optimal
    static class BeanWithConflict
    {
        public int getX() { return 3; }
        public boolean getx() { return false; }
    }

    // [databind#238]
    protected static class Getters1A
    {
        @JsonProperty
        protected int value = 3;

        public int getValue() { return value+1; }
        public boolean isValue() { return false; }
    }

    // variant where order of declarations is reversed; to try to
    // ensure declaration order won't break things
    protected static class Getters1B
    {
        public boolean isValue() { return false; }

        @JsonProperty
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

    // For [databind#541]
    static class Bean541 {
        protected String str;

        @JsonCreator
        public Bean541(@JsonProperty("str") String str) {
            this.str = str;
        }

        @JsonProperty("s")
        public String getStr() {
            return str;
        }
     }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    public void testFailWithDupProps() throws Exception
    {
        BeanWithConflict bean = new BeanWithConflict();
        try {
            String json = objectWriter().writeValueAsString(bean);
            fail("Should have failed due to conflicting accessor definitions; got JSON = "+json);
        } catch (InvalidDefinitionException e) {
            verifyException(e, "Conflicting getter definitions");
        }
    }

    // [databind#238]: ok to have getter, "isGetter"
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
        ObjectMapper mapper = jsonMapperBuilder()
                .annotationIntrospector(new InferingIntrospector())
                .build();
        String json = mapper.writeValueAsString(new Infernal());
        assertEquals(a2q("{'name':'Bob'}"), json);
    }

    public void testInferredNameConflictsWithSetters() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setAnnotationIntrospector(new InferingIntrospector());
        Infernal inf = mapper.readValue(a2q("{'stuff':'Bob'}"), Infernal.class);
        assertNotNull(inf);
    }

    public void testIssue541() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder()
                .disable(
                MapperFeature.AUTO_DETECT_CREATORS,
                MapperFeature.AUTO_DETECT_FIELDS,
                MapperFeature.AUTO_DETECT_GETTERS,
                MapperFeature.AUTO_DETECT_IS_GETTERS,
                MapperFeature.AUTO_DETECT_SETTERS,
                MapperFeature.USE_GETTERS_AS_SETTERS
                        ).build();
        Bean541 data = mapper.readValue("{\"str\":\"the string\"}", Bean541.class);
        if (data == null) {
            throw new IllegalStateException("data is null");
        }
        if (!"the string".equals(data.getStr())) {
            throw new IllegalStateException("bad value for data.str");
        }
    }
}
