package com.fasterxml.jackson.failing;

import java.util.HashMap;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.type.TypeFactory;

@SuppressWarnings("serial")
public class JavaTypeDesc1301Test extends BaseMapTest
{
    static class DataDefinition extends HashMap<String, DataDefinition> {
        public DataDefinition definition;
        public DataDefinition elements;
        public String regex;
        public boolean required;
        public String type;
    }

    // for [databind#1301]
    public void testJavaTypeToString() throws Exception
    {
        TypeFactory tf = objectMapper().getTypeFactory();
        String desc = tf.constructType(DataDefinition.class).toString();
        assertNotNull(desc);
        // could try comparing exact message, but since it's informational try looser:
        if (!desc.contains("map type")) {
            fail("Description should contain 'map type', did not: "+desc);
        }
        if (!desc.contains("recursive type")) {
            fail("Description should contain 'recursive type', did not: "+desc);
        }
    }
}
