package com.fasterxml.jackson.failing;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

public class ObjectIdSubTypes4607Test extends DatabindTestUtil
{
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

    @Test
    public void shouldHandleTypeDefinitionJson() throws Exception {
        final ObjectMapper mapper = newJsonMapper();
        TypeDefinition model = mapper.readValue("{ \"@type\": \"number\" }", TypeDefinition.class);
        Assertions.assertInstanceOf(NumberTypeDefinition.class, model);
    }
}

