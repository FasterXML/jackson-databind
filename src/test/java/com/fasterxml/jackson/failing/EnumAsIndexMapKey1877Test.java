package com.fasterxml.jackson.failing;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

public class EnumAsIndexMapKey1877Test extends BaseMapTest
{
    public enum Type {
        ANY,
        OTHER
    }

    static class Container {
        private Type simpleType;
        private Map<Type, String> map;

        public Type getSimpleType() {
            return simpleType;
        }

        public void setSimpleType(Type simpleType) {
            this.simpleType= simpleType;
        }

        public Map<Type, String> getMap() {
            return map;
        }

        public void setMap(Map<Type, String> map) {
            this.map = map;
        }
    }
    // [databind#1877]
    public void testEnumAsIndexMapKey() throws Exception
    {
        ObjectMapper mapper = newObjectMapper();

        Map<Type, String> map = new HashMap<>();
        map.put(Type.OTHER, "hello world");
        Container container = new Container();
        container.setSimpleType(Type.ANY);
        container.setMap(map);

        String json = mapper
                .writer().with(SerializationFeature.WRITE_ENUMS_USING_INDEX)
                .writeValueAsString(container);

        Container cont;
        try {
            cont = mapper
                .readerFor(Container.class)
                .readValue(json);
        } catch (JsonMappingException e) {
            throw e;
        }
        assertNotNull(cont);
        assertEquals(1, container.getMap().size());
        InvalidFormatException foo = null;

        assertSame(Type.OTHER, container.getMap().keySet().iterator().next());
    }
}
