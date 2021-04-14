package com.fasterxml.jackson.databind.deser.filter;

import java.io.*;
import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.testutil.NoCheckSubTypeValidator;

// for [databind#2221]
public class ProblemHandlerUnknownTypeId2221Test extends BaseMapTest
{
    @SuppressWarnings("rawtypes")
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "_class")
    @JsonInclude(Include.NON_EMPTY)
    static class GenericContent {

        private Collection innerObjects;

        public Collection getInnerObjects() {
            return innerObjects;
        }

        public void setInnerObjects(Collection innerObjects) {
            this.innerObjects = innerObjects;
        }
    }

    static class DummyContent {
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

    private final static String CLASS_GENERIC_CONTENT = GenericContent.class.getName();
    private final static String CLASS_DUMMY_CONTENT = DummyContent.class.getName();
    private final static String JSON = a2q(
"{\n" +
"          \"_class\":\""+CLASS_GENERIC_CONTENT+"\",\n" +
"          \"innerObjects\":\n" +
"               [\n" +
"                    \"java.util.ArrayList\",\n" +
"                    [\n" +
"                         [\n" +
"                              \""+CLASS_DUMMY_CONTENT+"\",\n" +
"                              {\n" +
"                                   \"aField\":\"some value\"\n" +
"                              }\n" +
"                         ],\n" +
"                         [\n" +
"                              \"com.fasterxml.jackson.databind.deser.NoSuchClass$AnInventedClassBeingNotOnTheClasspath\",\n" +
"                              {\n" +
"                                   \"aField\":\"some value\"\n" +
"                              }\n" +
"                         ]\n" +
"                    ]\n" +
"               ]\n" +
"     }"
);

    public void testWithDeserializationProblemHandler() throws Exception {
        final ObjectMapper mapper = new ObjectMapper()
                .activateDefaultTyping(NoCheckSubTypeValidator.instance);
        mapper.addHandler(new DeserializationProblemHandler() {
            @Override
            public JavaType handleUnknownTypeId(DeserializationContext ctxt, JavaType baseType, String subTypeId, TypeIdResolver idResolver, String failureMsg) throws IOException {
//                System.out.println("Print out a warning here");
                return ctxt.constructType(Void.class);
            }
        });
        GenericContent processableContent = mapper.readValue(JSON, GenericContent.class);
        assertNotNull(processableContent.getInnerObjects());
        assertEquals(2, processableContent.getInnerObjects().size());
    }

    public void testWithDisabledFAIL_ON_INVALID_SUBTYPE() throws Exception {
        final ObjectMapper mapper = new ObjectMapper()
                .disable(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE)
                .activateDefaultTyping(NoCheckSubTypeValidator.instance)
        ;
        GenericContent processableContent = mapper.readValue(JSON, GenericContent.class);
        assertNotNull(processableContent.getInnerObjects());
        assertEquals(2, processableContent.getInnerObjects().size());
    }
}
