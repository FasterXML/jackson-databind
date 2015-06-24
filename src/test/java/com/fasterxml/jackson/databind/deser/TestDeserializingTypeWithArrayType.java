package com.fasterxml.jackson.databind.deser;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeId;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.jsontype.impl.TypeIdResolverBase;
import org.junit.Assert;

import java.util.Arrays;


/**
 * Created by zoliszel on 23/06/2015.
 */
public class TestDeserializingTypeWithArrayType {

    public static class TestIdResolver extends TypeIdResolverBase {
        public TestIdResolver() {
        }

        @Override
        public String idFromValue(Object value) {
            if (value instanceof Test) {
                Test test = ((Test) value);
                return Arrays.toString(test.getType());
            }
            return null;
        }

        @Override
        public String idFromValueAndType(Object value, Class<?> suggestedType) {
            return idFromValue(value);
        }

        @Override
        public JavaType typeFromId(DatabindContext ctxt, String id) {
            if(id.matches("\\[.+\\]")){
                return ctxt.constructType(Test.class);
            }
           throw new IllegalArgumentException("Can't resolve type");
        }

        @Override
        public JsonTypeInfo.Id getMechanism() {
            return JsonTypeInfo.Id.CUSTOM;
        }

        @Override
        public void init(JavaType baseType) {
        }

        @Override
        public String idFromBaseType() {
            return null;
        }

        @Override
        public JavaType typeFromId(String s) {
            throw new UnsupportedOperationException("Getting JavaType from Id is not supported without a Databinding Context");
        }
    }

    private static final String testMessage_NPE = "{\"someOtherTest\":{\"type\":[\"string\",\"object\"]},\"type\":[\"string\",\"object\",\"array\"],\"name\":\"someName\"}";


    private static final String testMessage = "{\"type\":[\"string\",\"object\",\"array\"]}";
    @JsonTypeIdResolver(TestIdResolver.class)
    @JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type",visible = true)
    public static class Test {

        @JsonProperty
        private Test someOtherTest;

        public void setSomeOtherTest(Test someOtherTest) {
            this.someOtherTest = someOtherTest;
        }

        public Test getSomeOtherTest() {
            return someOtherTest;
        }

        @JsonProperty
        private String name;

        public void setName(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        @JsonTypeId
        private String[] type;

        public void setType(String[] type) {
            this.type = type;
        }

        public String[] getType() {
            return type;
        }
    }

    @org.junit.Test
    public void testParse() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        TypeResolverBuilder<?> typer = new ObjectMapper.DefaultTypeResolverBuilder(ObjectMapper.DefaultTyping.NON_FINAL);
        typer = typer.init(JsonTypeInfo.Id.NAME, null);
        typer = typer.inclusion(JsonTypeInfo.As.PROPERTY);
        typer = typer.typeProperty("type");
        typer = typer.typeIdVisibility(true);
        mapper.setDefaultTyping(typer);

        Test test = mapper.readValue(testMessage, Test.class);
        Assert.assertArrayEquals(new String[]{"string","object","array"},test.getType());
    }



    @org.junit.Test
    public void testTokenBufferNPE() throws Exception {

        /*
         * The TokenBuffer parser implementation failed to reach out to the parentcontext when currentItem is a start_array
         * cause NPE in parsing the below message.
         */
        ObjectMapper mapper = new ObjectMapper();
        TypeResolverBuilder<?> typer = new ObjectMapper.DefaultTypeResolverBuilder(ObjectMapper.DefaultTyping.NON_FINAL);
        typer = typer.init(JsonTypeInfo.Id.NAME, null);
        typer = typer.inclusion(JsonTypeInfo.As.PROPERTY);
        typer = typer.typeProperty("type");
        typer = typer.typeIdVisibility(true);
        mapper.setDefaultTyping(typer);

        Test test = mapper.readValue(testMessage_NPE, Test.class);
        Assert.assertArrayEquals(new String[]{"string","object","array"},test.getType());
    }


}
