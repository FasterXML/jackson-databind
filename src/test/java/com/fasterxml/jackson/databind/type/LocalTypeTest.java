package com.fasterxml.jackson.databind.type;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LocalTypeTest
{
    // [databind#609]
    static class EntityContainer {
        RuleForm entity;

        @SuppressWarnings("unchecked")
        public <T extends RuleForm> T getEntity() { return (T) entity; }
        public <T extends RuleForm> void setEntity(T e) { entity = e; }
    }

    static class RuleForm {
        public int value;

        public RuleForm() { }
        public RuleForm(int v) { value = v; }
    }

    // [databind#609]
    @Test
    void localPartialType609() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        EntityContainer input = new EntityContainer();
        input.entity = new RuleForm(12);
        String json = mapper.writeValueAsString(input);

        EntityContainer output = mapper.readValue(json, EntityContainer.class);
        assertEquals(12, output.getEntity().value);
    }
}

