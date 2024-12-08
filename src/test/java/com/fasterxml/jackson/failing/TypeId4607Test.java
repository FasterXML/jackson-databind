package com.fasterxml.jackson.failing;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static com.fasterxml.jackson.databind.BaseMapTest.newJsonMapper;

public class TypeId4607Test {

    @JsonIdentityInfo(generator = ObjectIdGenerators.StringIdGenerator.class)
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
    @JsonSubTypes({
            @JsonSubTypes.Type(value = EnumTypeDefinition.class, name = "enum"),
            @JsonSubTypes.Type(value = NumberTypeDefinition.class, name = "number")
    })
    interface TypeDefinition {
    }

    static class EnumTypeDefinition implements TypeDefinition {
        public List<String> values;
    }

    static class NumberTypeDefinition implements TypeDefinition {
    }

    private static final ObjectMapper mapper = newJsonMapper();

    @Test
    public void shouldHandleTypeDefinitionJson() throws Exception {
        String input = "{" +
                "        \"@type\": \"number\"   " +
                "      }";

        TypeDefinition model = mapper.readValue(input, TypeDefinition.class);

        Assertions.assertInstanceOf(NumberTypeDefinition.class, model);
    }
}

