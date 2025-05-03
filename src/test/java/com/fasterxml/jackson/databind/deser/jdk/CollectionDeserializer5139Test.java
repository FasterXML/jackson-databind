package com.fasterxml.jackson.databind.deser.jdk;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidNullException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CollectionDeserializer5139Test {
    static class Dst {
        private List<Integer> list;

        public List<Integer> getList() {
            return list;
        }

        public void setList(List<Integer> list) {
            this.list = list;
        }
    }

    @Test
    public void nullsFailTest() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configOverride(List.class).setSetterInfo(JsonSetter.Value.forContentNulls(Nulls.FAIL));

        assertThrows(
                InvalidNullException.class,
                () -> mapper.readValue("{\"list\":[\"\"]}", new TypeReference<Dst>(){})
        );
    }

    @Test
    public void nullsSkipTest() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configOverride(List.class).setSetterInfo(JsonSetter.Value.forContentNulls(Nulls.SKIP));

        Dst dst = mapper.readValue("{\"list\":[\"\"]}", new TypeReference<Dst>() {});

        assertTrue(dst.getList().isEmpty());
    }
}
