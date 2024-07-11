package com.fasterxml.jackson.databind.jsontype;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

// for [databind#4407]
public class CustomTypeIdResolver4407Test extends DatabindTestUtil
{
    static class Wrapper4407Prop {
        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
                include = JsonTypeInfo.As.PROPERTY,
                defaultImpl = Default4407.class,
                property = "type")
        @JsonTypeIdResolver(Resolver4407_typex.class)
        public Base4407 wrapped;

        Wrapper4407Prop() { }
        public Wrapper4407Prop(String v) {
            wrapped = new Impl4407(v);
        }
    }

    static class Wrapper4407PropNull {
        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
                include = JsonTypeInfo.As.PROPERTY,
                defaultImpl = Default4407.class,
                property = "type")
        @JsonTypeIdResolver(Resolver4407_null.class)
        public Base4407 wrapped;

        Wrapper4407PropNull() { }
        public Wrapper4407PropNull(String v) {
            wrapped = new Impl4407(v);
        }
    }

    static class Wrapper4407WrapperArray {
        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
                include = JsonTypeInfo.As.WRAPPER_ARRAY,
                defaultImpl = Default4407.class)
        @JsonTypeIdResolver(Resolver4407_typex.class)
        public Base4407 wrapped;

        Wrapper4407WrapperArray() { }
        public Wrapper4407WrapperArray(String v) {
            wrapped = new Impl4407(v);
        }
    }

    static class Wrapper4407WrapperArrayNull {
        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
                include = JsonTypeInfo.As.WRAPPER_ARRAY,
                defaultImpl = Default4407.class)
        @JsonTypeIdResolver(Resolver4407_null.class)
        public Base4407 wrapped;

        Wrapper4407WrapperArrayNull() { }
        public Wrapper4407WrapperArrayNull(String v) {
            wrapped = new Impl4407(v);
        }
    }

    static class Wrapper4407WrapperObject {
        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
                include = JsonTypeInfo.As.WRAPPER_OBJECT,
                defaultImpl = Default4407.class)
        @JsonTypeIdResolver(Resolver4407_typex.class)
        public Base4407 wrapped;

        Wrapper4407WrapperObject() { }
        public Wrapper4407WrapperObject(String v) {
            wrapped = new Impl4407(v);
        }
    }

    static class Wrapper4407WrapperObjectNull {
        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
                include = JsonTypeInfo.As.WRAPPER_OBJECT,
                defaultImpl = Default4407.class)
        @JsonTypeIdResolver(Resolver4407_null.class)
        public Base4407 wrapped;

        Wrapper4407WrapperObjectNull() { }
        public Wrapper4407WrapperObjectNull(String v) {
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

    static class Default4407 extends Base4407 {
        public String value;

        Default4407() { }
        public Default4407(String v) { value = v; }
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
            // NOTE: needed for trying to deserialize without type id
            return "default";
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

    // [databind#4407]: with "as-property" type id
    @Test
    public void testTypeIdProp4407NonNull() throws Exception
    {
        // First, check out "normal" case of non-null type id
        final String EXP = a2q("{'wrapped':{'type':'typeX','value':'xyz'}}");
        assertEquals(EXP,
                MAPPER.writeValueAsString(new Wrapper4407Prop("xyz")));
        Wrapper4407Prop result = MAPPER.readValue(EXP, Wrapper4407Prop.class);
        assertNotNull(result);
        assertNotNull(result.wrapped);
        assertEquals(Impl4407.class, result.wrapped.getClass());
    }

    @Test
    public void testTypeIdProp4407Null() throws Exception
    {
        // And then null one
        final String EXP = a2q("{'wrapped':{'value':'xyz'}}");
        assertEquals(EXP,
                MAPPER.writeValueAsString(new Wrapper4407PropNull("xyz")));
        assertNotNull(MAPPER.readValue(EXP, Wrapper4407PropNull.class));
        Wrapper4407Prop result = MAPPER.readValue(EXP, Wrapper4407Prop.class);
        assertNotNull(result);
        assertNotNull(result.wrapped);
        assertEquals(Default4407.class, result.wrapped.getClass());
    }

    // [databind#4407]: with "as-wrapper-array" type id
    @Test
    public void testTypeIdWrapperArray4407NonNull() throws Exception
    {
        // First, check out "normal" case of non-null type id
        final String EXP = a2q("{'wrapped':['typeX',{'value':'xyz'}]}");
        assertEquals(EXP,
                MAPPER.writeValueAsString(new Wrapper4407WrapperArray("xyz")));
        Wrapper4407WrapperArray result = MAPPER.readValue(EXP, Wrapper4407WrapperArray.class);
        assertNotNull(result);
        assertNotNull(result.wrapped);
        assertEquals(Impl4407.class, result.wrapped.getClass());
    }

    @Test
    public void testTypeIdWrapperArray4407Null() throws Exception
    {
        // And then null one
        final String EXP = a2q("{'wrapped':{'value':'xyz'}}");
        assertEquals(EXP,
                MAPPER.writeValueAsString(new Wrapper4407WrapperArrayNull("xyz")));
        Wrapper4407WrapperArray result = MAPPER.readValue(EXP, Wrapper4407WrapperArray.class);
        assertNotNull(result);
        assertNotNull(result.wrapped);
        assertEquals(Default4407.class, result.wrapped.getClass());
    }

    // [databind#4407]: with "as-wrapper-object" type id
    @Test
    public void testTypeIdWrapperObject4407NonNull() throws Exception
    {
        // First, check out "normal" case of non-null type id
        final String EXP = a2q("{'wrapped':{'typeX':{'value':'xyz'}}}");
        assertEquals(EXP,
                MAPPER.writeValueAsString(new Wrapper4407WrapperObject("xyz")));
        Wrapper4407WrapperObject result = MAPPER.readValue(EXP, Wrapper4407WrapperObject.class);
        assertNotNull(result);
        assertNotNull(result.wrapped);
        assertEquals(Impl4407.class, result.wrapped.getClass());
    }

    @Test
    public void testTypeIdWrapperObject4407Null() throws Exception
    {
        // And then null one
        final String EXP = a2q("{'wrapped':{'value':'xyz'}}");
        assertEquals(EXP,
                MAPPER.writeValueAsString(new Wrapper4407WrapperObjectNull("xyz")));
        Wrapper4407WrapperObject result = MAPPER.readValue(EXP, Wrapper4407WrapperObject.class);
        assertNotNull(result);
        assertNotNull(result.wrapped);
        assertEquals(Default4407.class, result.wrapped.getClass());
    }
}
