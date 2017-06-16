package com.fasterxml.jackson.failing;

import java.util.HashMap;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;

@SuppressWarnings("serial")
public class TypeResolution1658Test extends BaseMapTest
{
    static class Tree<T> extends HashMap<T, Tree<T>> {
        public T value;
    }

    static class StringTree extends Tree<String> { }
    
    public void testTypeEquality1658() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        BeanDescription desc1 = mapper.getSerializationConfig()
                .introspect(mapper.constructType(StringTree.class));
        BeanDescription desc2 = mapper.getSerializationConfig()
                .introspect(mapper.constructType(StringTree.class));

        BeanPropertyDefinition prop1 = desc1.findProperties().get(0);
        BeanPropertyDefinition prop2 = desc2.findProperties().get(0);

        JavaType t1 = prop1.getPrimaryMember().getType();
        JavaType t2 = prop2.getPrimaryMember().getType();

        assertSame(t1, t2);
        assertTrue(t1.equals(t2));
    }
}
