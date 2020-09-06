package com.fasterxml.jackson.failing;

import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;

import com.fasterxml.jackson.databind.*;

public class GenericTypeDeserialization2846Test extends BaseMapTest
{
    @SuppressWarnings("rawtypes")
    static class GenericEntity<T> {
        T field;

        Map map;

        public void setField(T field) {
            this.field = field;
        }

        public T getField() {
            return field;
        }

        public Map getMap() {
            return map;
        }

        public void setMap(Map map) {
            this.map = map;
        }
    }

    static class SimpleEntity {
        Integer number;

        public void setNumber(Integer number) {
            this.number = number;
        }

        public Integer getNumber() {
            return number;
        }
    }

    public void testIssue2821Part2() throws Exception {
        ObjectMapper m = new ObjectMapper();
        final String JSON = "{ \"field\": { \"number\": 1 }, \"map\": { \"key\": \"value\" } }";
        GenericEntity<SimpleEntity> genericEntity = m.readValue(JSON,
                new TypeReference<GenericEntity<SimpleEntity>>() {});
        assertNotNull(genericEntity);
    }
}
