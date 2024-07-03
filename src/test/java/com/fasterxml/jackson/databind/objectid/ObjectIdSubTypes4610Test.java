package com.fasterxml.jackson.databind.objectid;

import java.util.List;

import com.fasterxml.jackson.databind.DeserializationFeature;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ObjectIdSubTypes4610Test extends DatabindTestUtil
{
    // Unused @JsonIdentityInfo
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
        String input = "{\"@type\": \"number\"}";

        TypeDefinition model = jsonMapperBuilder()
                .disable(DeserializationFeature.FAIL_ON_UNRESOLVED_OBJECT_IDS)
                .build()
                .readValue(input, TypeDefinition.class);

        Assertions.assertInstanceOf(NumberTypeDefinition.class, model);
    }

    @Test
    public void testRoundTrip() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder()
                .enable(DeserializationFeature.FAIL_ON_UNRESOLVED_OBJECT_IDS)
                .build();

        // Ser
        String JSON = mapper.writeValueAsString(new NumberTypeDefinition());
        assertTrue(JSON.contains("@id"));

        // Deser
        TypeDefinition model = mapper.readValue(JSON, TypeDefinition.class);
        Assertions.assertInstanceOf(NumberTypeDefinition.class, model);
    }

    @Test
    public void shouldHandleTypeDefinitionJsonFail() throws Exception {
        String input = "{\"@type\": \"number\"}";

        try {
            TypeDefinition model = jsonMapperBuilder()
                    .enable(DeserializationFeature.FAIL_ON_UNRESOLVED_OBJECT_IDS)
                    .build()
                    .readValue(input, TypeDefinition.class);
            fail("Should not pass");
        } catch (Exception e) {
            assertTrue(e.getMessage().startsWith("No Object Id found for an instance of"));
        }
    }

}

