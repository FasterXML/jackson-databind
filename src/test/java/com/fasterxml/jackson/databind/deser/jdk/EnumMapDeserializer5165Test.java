package com.fasterxml.jackson.databind.deser.jdk;

import java.util.EnumMap;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidNullException;
import com.fasterxml.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

// For [databind#5165]
public class EnumMapDeserializer5165Test
{
    public enum MyEnum {
        FOO
    }

    static class Dst {
        private EnumMap<MyEnum, Integer> map;

        public EnumMap<MyEnum, Integer> getMap() {
            return map;
        }

        public void setMap(EnumMap<MyEnum, Integer> map) {
            this.map = map;
        }
    }

    @Test
    public void nullsFailTest() {
        ObjectMapper mapper = JsonMapper.builder()
                .defaultSetterInfo(JsonSetter.Value.forContentNulls(Nulls.FAIL))
                .build();

        assertThrows(
                InvalidNullException.class,
                () -> mapper.readValue("{\"map\":{\"FOO\":\"\"}}", new TypeReference<Dst>(){})
        );
    }

    @Test
    public void nullsSkipTest() throws Exception {
        ObjectMapper mapper = JsonMapper.builder()
                .defaultSetterInfo(JsonSetter.Value.forContentNulls(Nulls.SKIP))
                .build();

        Dst dst = mapper.readValue("{\"map\":{\"FOO\":\"\"}}", new TypeReference<Dst>() {});

        assertTrue(dst.getMap().isEmpty());
    }
}
