package com.fasterxml.jackson.databind.deser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;

public class HandleUnknowTypeIdTest {

    public static class DummyContent {
        private String aField;

        public DummyContent() {
            super();
        }

        public DummyContent(String aField) {
            super();
            this.aField = aField;
        }

        public String getaField() {
            return aField;
        }

        public void setaField(String aField) {
            this.aField = aField;
        }

        @Override
        public String toString() {
            return "DummyContent [aField=" + aField + "]";
        }
    }

    private ObjectMapper objectMapper;

    @Before
    public void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.enableDefaultTyping();
    }

    @Test
    public void testDeserializationWithDeserializationProblemHandler() throws JsonParseException, JsonMappingException, IOException {
        String dummyJson = IOUtils.toString(HandleUnknowTypeIdTest.class.getResourceAsStream("/com/fasterxml/jackson/databind/deser/DummyProcessableContent.json"),
                StandardCharsets.UTF_8);
        objectMapper.addHandler(new DeserializationProblemHandler() {
            @Override
            public JavaType handleUnknownTypeId(DeserializationContext ctxt, JavaType baseType, String subTypeId, TypeIdResolver idResolver, String failureMsg) throws IOException {
                System.out.println("Print out a warning here");
                return ctxt.constructType(Void.class);
            }
        });
        GenericContent processableContent = objectMapper.readValue(dummyJson, GenericContent.class);
        Assertions.assertThat(processableContent.getInnerObjects()).hasSize(2).usingFieldByFieldElementComparator().contains(new DummyContent("some value"));
    }

    @Test
    public void testDeserializationWithFAIL_ON_INVALID_SUBTYPE_false() throws JsonParseException, JsonMappingException, IOException {
        String dummyJson = IOUtils.toString(HandleUnknowTypeIdTest.class.getResourceAsStream("/com/fasterxml/jackson/databind/deser/DummyProcessableContent.json"),
                StandardCharsets.UTF_8);
        objectMapper.disable(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE);
        GenericContent processableContent = objectMapper.readValue(dummyJson, GenericContent.class);
        Assertions.assertThat(processableContent.getInnerObjects()).hasSize(2).usingFieldByFieldElementComparator().contains(new DummyContent("some value"));
    }
}
