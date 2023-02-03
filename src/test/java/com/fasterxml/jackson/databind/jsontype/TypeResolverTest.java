package com.fasterxml.jackson.databind.jsontype;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleAbstractTypeResolver;
import com.fasterxml.jackson.databind.module.SimpleModule;

@SuppressWarnings("rawtypes")
public class TypeResolverTest extends BaseMapTest
{
    static class A {
        private Map map;

        @JsonCreator
        public A(@JsonProperty("z") Map<String, B> map) {
            this.map = map;
        }

        public Map getMap() {
            return map;
        }
    }

    static class B {
        int a;

        public B(@JsonProperty("a") int a) {
            this.a = a;
        }
    }

    @SuppressWarnings("serial")
    static class MyMap<K,V> extends HashMap<K,V> { }

    public static void testSubtypeResolution() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        SimpleAbstractTypeResolver resolver = new SimpleAbstractTypeResolver();
        resolver.addMapping(Map.class, MyMap.class);

        SimpleModule basicModule = new SimpleModule();
        basicModule.setAbstractTypes(resolver);
        mapper.registerModule(basicModule);
        String value = "{\"z\": {\"zz\": {\"a\": 42}}}";
        A a = mapper.readValue(value, A.class);

        Map map = a.getMap();
        assertEquals(MyMap.class, map.getClass());

        Object ob = map.get("zz");
        assertEquals(B.class, ob.getClass());
    }
}
