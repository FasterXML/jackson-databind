package com.fasterxml.jackson.databind.type;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.jsontype.impl.StdTypeResolverBuilder;

public class RecursiveType1658Test extends BaseMapTest
{
    @SuppressWarnings("serial")
    static class Tree<T> extends HashMap<T, Tree<T>> // implements Serializable
    {
        public Tree() { }

        public Tree(List<T> children) {
            this();
            for (final T t : children) {
                this.put(t, new Tree<T>());
            }
        }

        public List<Tree<T>> getLeafTrees() {
            return null;
        }
    }

    public void testRecursive1658() throws Exception
    {
        Tree<String> t = new Tree<String>(Arrays.asList("hello", "world"));
        ObjectMapper mapper = new ObjectMapper();

        final TypeResolverBuilder<?> typer = new StdTypeResolverBuilder(JsonTypeInfo.Id.CLASS,
                JsonTypeInfo.As.PROPERTY, null);
        mapper.setDefaultTyping(typer);
        String res = mapper.writeValueAsString(t);
        Tree<?> tRead = mapper.readValue(res, Tree.class);
        assertNotNull(tRead);
    }
}
