package com.fasterxml.jackson.databind.introspect;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonSetter;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;

// mostly for [databind#1033]
public class SetterConflictTest extends BaseMapTest
{
    // Should prefer primitives over Strings, more complex types, by default
    static class Issue1033Bean {
        public int value;

        public void setValue(int v) { value = v; }
        public void setValue(Issue1033Bean foo) {
            throw new Error("Should not get called");
        }
    }

    // [databind#2979]
    static class DuplicateSetterBean2979 {
        Object value;

        public void setBloop(Boolean bloop) {
            throw new Error("Wrong setter!");
        }

        @JsonSetter
        public void setBloop(Object bloop) {
            value = bloop;
        }
    }

    // [databind#3125]: As per existing (2.7+) logic we SHOULD tie-break
    // in favor of `String` but code up until 2.12 short-circuited early fail
    static class DupSetter3125Bean {
        String str;

        public void setValue(Integer value) { throw new RuntimeException("Integer: wrong!"); }
        public void setValue(Boolean value) { throw new RuntimeException("Boolean: wrong!"); }
        public void setValue(String value) { str = value; }
    }

    static class DupSetter3125BeanFail {
        public void setValue(Integer value) { throw new RuntimeException("Integer: wrong!"); }
        public void setValue(Boolean value) { throw new RuntimeException("Boolean: wrong!"); }
        public void setValue(List<String> value) { throw new RuntimeException("List: wrong!"); }
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    // [databind#1033]
    public void testSetterPriority() throws Exception
    {
        Issue1033Bean bean = MAPPER.readValue(a2q("{'value':42}"),
                Issue1033Bean.class);
        assertEquals(42, bean.value);
    }

    // [databind#2979]
    public void testConflictingSetters() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .propertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE)
                .build();
        DuplicateSetterBean2979 result = mapper.readValue(a2q("{'bloop':true}"),
                DuplicateSetterBean2979.class);
        assertEquals(Boolean.TRUE, result.value);
    }

    // [databind#3125]
    public void testDuplicateSetterResolutionOk() throws Exception
    {
        POJOPropertiesCollector coll = collector(MAPPER, DupSetter3125Bean.class,
                false);
        final List<BeanPropertyDefinition> props = coll.getProperties();
        assertEquals(1, props.size());
        POJOPropertyBuilder prop = (POJOPropertyBuilder) props.get(0);
        assertEquals("value", prop.getName());
        // but this failed
        AnnotatedMethod m = prop.getSetter();
        assertNotNull(m);
        assertEquals(String.class, m.getRawParameterType(0));

        // and then actual usage too
        DupSetter3125Bean value = MAPPER.readValue(a2q("{'value':'foo'}"),
                DupSetter3125Bean.class);
        assertEquals("foo", value.str);
    }

    // [databind#3125]: caught case
    public void testDuplicateSetterResolutionFail() throws Exception
    {
        try {
            MAPPER.readValue(a2q("{'value':'foo'}"),
                    DupSetter3125BeanFail.class);
            fail("Should not pass");
        } catch (InvalidDefinitionException e) {
            verifyException(e, "Conflicting setter definitions for property \"value\"");
        }
    }

    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */

    protected POJOPropertiesCollector collector(ObjectMapper m0,
            Class<?> cls, boolean forSerialization)
    {
        BasicClassIntrospector bci = new BasicClassIntrospector();
        // no real difference between serialization, deserialization, at least here
        if (forSerialization) {
            return bci.collectProperties(m0.getSerializationConfig(),
                    m0.constructType(cls), null, true);
        }
        return bci.collectProperties(m0.getDeserializationConfig(),
                m0.constructType(cls), null, false);
    }
}
