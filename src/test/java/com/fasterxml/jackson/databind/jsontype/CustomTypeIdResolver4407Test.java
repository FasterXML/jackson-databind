package com.fasterxml.jackson.databind.jsontype;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class CustomTypeIdResolver4407Test extends DatabindTestUtil
{
    // for [databind#4407]
    static class Wrapper4407Prop {
        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
                include = JsonTypeInfo.As.PROPERTY,
                property = "type")
        @JsonTypeIdResolver(Resolver4407_typex.class)
        public Base4407 wrapped;

        Wrapper4407Prop() { }
        public Wrapper4407Prop(String v) {
            wrapped = new Impl4407(v);
        }
    }

    @JsonSubTypes({ @JsonSubTypes.Type(value = Impl4407.class) })
    static class Base4407 { }

    static class Impl4407 extends Base4407 {
        public String value;

        Impl4407() { }
        public Impl4407(String v) { value = v; }
    }

    static class Resolver4407_typex extends Resolver4407Base {
        public Resolver4407_typex() { super("typeX"); }
    }

    static class Resolver4407_null extends Resolver4407Base {
        public Resolver4407_null() { super(null); }
    }

    static abstract class Resolver4407Base implements TypeIdResolver {
        private final String _typeId;

        Resolver4407Base(String typeId) {
            _typeId = typeId;
        }

        @Override
        public void init(JavaType baseType) { }

        @Override
        public String idFromValue(Object value) {
            return _typeId;
        }

        @Override
        public String idFromValueAndType(Object value, Class<?> suggestedType) {
            return idFromValue(value);
        }

        @Override
        public String idFromBaseType() {
            return null;
        }

        @Override
        public JavaType typeFromId(DatabindContext ctxt, String id) {
            if (id.equals(_typeId)) {
                return ctxt.constructType(Impl4407.class);
            }
            return null;
        }

        @Override
        public String getDescForKnownTypeIds() {
            return null;
        }

        @Override
        public JsonTypeInfo.Id getMechanism() {
            return JsonTypeInfo.Id.CUSTOM;
        }
    }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    // [databind#4407]
    @Test
    public void testTypeIdProp4407() throws Exception
    {
        final String EXP = a2q("{'wrapped':{'type':'typeX','value':'xyz'}}");
        assertEquals(EXP,
                MAPPER.writeValueAsString(new Wrapper4407Prop("xyz")));
        assertNotNull(MAPPER.readValue(EXP, Wrapper4407Prop.class));
    }
}
