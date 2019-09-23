package com.fasterxml.jackson.databind.deser.filter;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

import com.fasterxml.jackson.databind.*;

public class NullConversionsViaCreator2458Test extends BaseMapTest
{
    // [databind#2458]
    static class Pojo {
        List<String> _value;

        @JsonCreator
        public Pojo(@JsonProperty("value") List<String> v) {
            this._value = Objects.requireNonNull(v, "value");
        }

        protected Pojo() { }

        public List<String> value() {
            return _value;
        }

        public void setOther(List<String> v) { }
    }

    private final ObjectMapper MAPPER_WITH_AS_EMPTY = jsonMapperBuilder()
            .defaultSetterInfo(JsonSetter.Value.construct(Nulls.AS_EMPTY,
                    Nulls.AS_EMPTY))
            .build();

    // [databind#2458]
    public void testMissingToEmptyViaCreator() throws Exception {
        Pojo pojo = MAPPER_WITH_AS_EMPTY.readValue("{}", Pojo.class);
        assertNotNull(pojo);
        assertNotNull(pojo.value());
        assertEquals(0, pojo.value().size());
    }

    // [databind#2458]
    public void testNullToEmptyViaCreator() throws Exception {
        Pojo pojo = MAPPER_WITH_AS_EMPTY.readValue("{\"value\":null}", Pojo.class);
        assertNotNull(pojo);
        assertNotNull(pojo.value());
        assertEquals(0, pojo.value().size());
    }
}
