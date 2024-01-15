package com.fasterxml.jackson.databind.deser.filter;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [databind#4309] : Use @JsonSetter(nulls=...) handling of null values during deserialization with
public class NullSkipForCollections4309Test
{
    static class Data1 {
        public List<Type> types;
    }

    static enum Type {
        ONE, TWO
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXISTING_PROPERTY, visible = true)
    @JsonSubTypes(value = { @JsonSubTypes.Type(value = DataType1.class, names = { "TYPE1" }) })
    static abstract class Data2 {
        public String type;
    }

    static class DataType1 extends Data2 { }

    @Test
    void shouldSkipUnknownEnumDeserializationWithSetter() throws Exception
    {
        // Given
        String json = "{ \"types\" : [\"TWO\", \"THREE\"] }";

        // When
        Data1 data = JsonMapper.builder()
            .defaultSetterInfo(JsonSetter.Value.forContentNulls(Nulls.SKIP))
            .enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL)
            .build()
            .readValue(json, Data1.class); // will be [TWO, null]

        // Then
        assertEquals(1, data.types.size());
        assertEquals(Type.TWO, data.types.get(0));
    }

    @Test
    void shouldSkipUnknownSubTypeDeserializationWithSetter() throws Exception
    {
        // Given
        String json = "[ {\"type\":\"TYPE1\" }, { \"type\" : \"TYPE2\"  } ]";

        // When
        List<Data2> actual = JsonMapper.builder()
            .defaultSetterInfo(JsonSetter.Value.forContentNulls(Nulls.SKIP))
            .disable(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE)
            .build()
            .readValue(json, new TypeReference<List<Data2>>() {});

        // Then
        assertEquals(1, actual.size());
        assertEquals(DataType1.class, actual.get(0).getClass());
    }
}
