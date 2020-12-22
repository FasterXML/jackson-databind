package com.fasterxml.jackson.databind.introspect;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.*;

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

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
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
}
