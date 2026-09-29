package com.fasterxml.jackson.databind.jsontype.vld;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class LegacyDenylistGenericArgumentTest extends DatabindTestUtil
{
    private ObjectMapper newDefaultTypingMapper() {
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Object.class)
                .allowIfSubType(Object.class)
                .build();
        return jsonMapperBuilder()
                .activateDefaultTyping(ptv, DefaultTyping.NON_FINAL)
                .build();
    }

    @Test
    public void directDenylistedTypeRejected() throws Exception
    {
        ObjectMapper mapper = newDefaultTypingMapper();
        String json = "[\"javax.swing.JEditorPane\",{}]";

        assertThrows(JsonMappingException.class,
                () -> mapper.readValue(json, Object.class));
    }

    @Test
    public void denylistedGenericArgumentRejected() throws Exception
    {
        ObjectMapper mapper = newDefaultTypingMapper();
        String json = "[\"java.util.HashMap<javax.swing.JEditorPane,java.lang.String>\",{}]";

        assertThrows(JsonMappingException.class,
                () -> mapper.readValue(json, Object.class));
    }
}
