package com.fasterxml.jackson.databind.deser.filter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import com.fasterxml.jackson.databind.deser.ValueInstantiator;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;

/**
 * Tests to exercise handler methods of {@link DeserializationProblemHandler}.
 *
 * @since 2.8
 */
public class ProblemHandlerTest extends BaseMapTest
{
    /*
    /**********************************************************
    /* Test handler types
    /**********************************************************
     */

    static class WeirdKeyHandler
        extends DeserializationProblemHandler
    {
        protected final Object key;

        public WeirdKeyHandler(Object key0) {
            key = key0;
        }

        @Override
        public Object handleWeirdKey(DeserializationContext ctxt,
                Class<?> rawKeyType, String keyValue,
                String failureMsg)
            throws IOException
        {
            return key;
        }
    }

    static class WeirdNumberHandler
        extends DeserializationProblemHandler
    {
        protected final Object value;

        public WeirdNumberHandler(Object v0) {
            value = v0;
        }

        @Override
        public Object handleWeirdNumberValue(DeserializationContext ctxt,
                Class<?> targetType, Number n,
                String failureMsg)
            throws IOException
        {
            return value;
        }
    }

    static class WeirdStringHandler
        extends DeserializationProblemHandler
    {
        protected final Object value;

        public WeirdStringHandler(Object v0) {
            value = v0;
        }

        @Override
        public Object handleWeirdStringValue(DeserializationContext ctxt,
                Class<?> targetType, String v,
                String failureMsg)
            throws IOException
        {
            return value;
        }
    }

    static class InstantiationProblemHandler
        extends DeserializationProblemHandler
    {
        protected final Object value;

        public InstantiationProblemHandler(Object v0) {
            value = v0;
        }

        @Override
        public Object handleInstantiationProblem(DeserializationContext ctxt,
                Class<?> instClass, Object argument, Throwable t)
            throws IOException
        {
            if (!(t instanceof ValueInstantiationException)) {
                throw new IllegalArgumentException("Should have gotten `ValueInstantiationException`, instead got: "+t);
            }
            return value;
        }
    }

    static class MissingInstantiationHandler
        extends DeserializationProblemHandler
    {
        protected final Object value;

        public MissingInstantiationHandler(Object v0) {
            value = v0;
        }

        @Override
        public Object handleMissingInstantiator(DeserializationContext ctxt,
                Class<?> instClass, ValueInstantiator inst, JsonParser p, String msg)
            throws IOException
        {
            p.skipChildren();
            return value;
        }
    }

    static class WeirdTokenHandler
        extends DeserializationProblemHandler
    {
        protected final Object value;

        public WeirdTokenHandler(Object v) {
            value = v;
        }

        @Override
        public Object handleUnexpectedToken(DeserializationContext ctxt,
                JavaType targetType, JsonToken t, JsonParser p,
                String failureMsg)
            throws IOException
        {
            return value;
        }
    }

    static class UnknownTypeIdHandler
        extends DeserializationProblemHandler
    {
        protected final Class<?> raw;

        public UnknownTypeIdHandler(Class<?> r) { raw = r; }

        @Override
        public JavaType handleUnknownTypeId(DeserializationContext ctxt,
                JavaType baseType, String subTypeId, TypeIdResolver idResolver,
                String failureMsg)
            throws IOException
        {
            return ctxt.constructType(raw);
        }
    }

    static class MissingTypeIdHandler
        extends DeserializationProblemHandler
    {
        protected final Class<?> raw;

        public MissingTypeIdHandler(Class<?> r) { raw = r; }

        @Override
        public JavaType handleMissingTypeId(DeserializationContext ctxt,
                JavaType baseType, TypeIdResolver idResolver,
                String failureMsg)
            throws IOException
        {
            return ctxt.constructType(raw);
        }
    }

    /*
    /**********************************************************
    /* Other helper types
    /**********************************************************
     */

    static class IntKeyMapWrapper {
        public Map<Integer,String> stuff;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    static class Base { }
    static class BaseImpl extends Base {
        public int a;
    }

    static class BaseWrapper {
        public Base value;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "clazz")
    static class Base2 { }
    static class Base2Impl extends Base2 {
        public int a;
    }

    static class Base2Wrapper {
        public Base2 value;
    }

    enum SingleValuedEnum {
        A;
    }

    static class BustedCtor {
        public final static BustedCtor INST = new BustedCtor(true);

        public BustedCtor() {
            throw new RuntimeException("Fail! (to be caught by handler)");
        }
        private BustedCtor(boolean b) { }
    }

    static class NoDefaultCtor {
        public int value;

        public NoDefaultCtor(int v) { value = v; }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    public void testWeirdKeyHandling() throws Exception
    {
        ObjectMapper mapper = newJsonMapper()
            .addHandler(new WeirdKeyHandler(7));
        IntKeyMapWrapper w = mapper.readValue("{\"stuff\":{\"foo\":\"abc\"}}",
                IntKeyMapWrapper.class);
        Map<Integer,String> map = w.stuff;
        assertEquals(1, map.size());
        assertEquals("abc", map.values().iterator().next());
        assertEquals(Integer.valueOf(7), map.keySet().iterator().next());
    }

    public void testWeirdNumberHandling() throws Exception
    {
        ObjectMapper mapper = newJsonMapper()
            .addHandler(new WeirdNumberHandler(SingleValuedEnum.A))
            ;
        SingleValuedEnum result = mapper.readValue("3", SingleValuedEnum.class);
        assertEquals(SingleValuedEnum.A, result);
    }

    public void testWeirdStringHandling() throws Exception
    {
        ObjectMapper mapper = newJsonMapper()
            .addHandler(new WeirdStringHandler(SingleValuedEnum.A))
            ;
        SingleValuedEnum result = mapper.readValue("\"B\"", SingleValuedEnum.class);
        assertEquals(SingleValuedEnum.A, result);

        // also, write [databind#1629] try this
        mapper = new ObjectMapper()
                .addHandler(new WeirdStringHandler(null));
        UUID result2 = mapper.readValue(q("not a uuid!"), UUID.class);
        assertNull(result2);
    }

    public void testInvalidTypeId() throws Exception
    {
        ObjectMapper mapper = newJsonMapper()
            .addHandler(new UnknownTypeIdHandler(BaseImpl.class));
        BaseWrapper w = mapper.readValue("{\"value\":{\"type\":\"foo\",\"a\":4}}",
                BaseWrapper.class);
        assertNotNull(w);
        assertEquals(BaseImpl.class, w.value.getClass());
    }

    public void testInvalidClassAsId() throws Exception
    {
        ObjectMapper mapper = newJsonMapper()
            .addHandler(new UnknownTypeIdHandler(Base2Impl.class));
        Base2Wrapper w = mapper.readValue("{\"value\":{\"clazz\":\"com.fizz\",\"a\":4}}",
                Base2Wrapper.class);
        assertNotNull(w);
        assertEquals(Base2Impl.class, w.value.getClass());
    }

    // 2.9: missing type id, distinct from unknown

    public void testMissingTypeId() throws Exception
    {
        ObjectMapper mapper = newJsonMapper()
            .addHandler(new MissingTypeIdHandler(BaseImpl.class));
        BaseWrapper w = mapper.readValue("{\"value\":{\"a\":4}}",
                BaseWrapper.class);
        assertNotNull(w);
        assertEquals(BaseImpl.class, w.value.getClass());
    }

    public void testMissingClassAsId() throws Exception
    {
        ObjectMapper mapper = newJsonMapper()
            .addHandler(new MissingTypeIdHandler(Base2Impl.class));
        Base2Wrapper w = mapper.readValue("{\"value\":{\"a\":4}}",
                Base2Wrapper.class);
        assertNotNull(w);
        assertEquals(Base2Impl.class, w.value.getClass());
    }

    // verify that by default we get special exception type
    public void testInvalidTypeIdFail() throws Exception
    {
        try {
            MAPPER.readValue("{\"value\":{\"type\":\"foo\",\"a\":4}}",
                BaseWrapper.class);
            fail("Should not pass");
        } catch (InvalidTypeIdException e) {
            verifyException(e, "Could not resolve type id 'foo'");
            assertEquals(Base.class, e.getBaseType().getRawClass());
            assertEquals("foo", e.getTypeId());
        }
    }

    public void testInstantiationExceptionHandling() throws Exception
    {
        ObjectMapper mapper = newJsonMapper()
            .addHandler(new InstantiationProblemHandler(BustedCtor.INST));
        BustedCtor w = mapper.readValue("{ }",
                BustedCtor.class);
        assertNotNull(w);
    }

    public void testMissingInstantiatorHandling() throws Exception
    {
        ObjectMapper mapper = newJsonMapper()
            .addHandler(new MissingInstantiationHandler(new NoDefaultCtor(13)))
            ;
        NoDefaultCtor w = mapper.readValue("{ \"x\" : true }", NoDefaultCtor.class);
        assertNotNull(w);
        assertEquals(13, w.value);
    }

    public void testUnexpectedTokenHandling() throws Exception
    {
        ObjectMapper mapper = newJsonMapper()
            .addHandler(new WeirdTokenHandler(Integer.valueOf(13)))
        ;
        Integer v = mapper.readValue("true", Integer.class);
        assertEquals(Integer.valueOf(13), v);

        // Just for code coverage really...
        mapper = newJsonMapper();
        mapper.addHandler(new WeirdTokenHandler(Integer.valueOf(13)));
        mapper.clearProblemHandlers();
        try {
            mapper.readValue("true", Integer.class);
            fail("Should not pass");
        } catch (MismatchedInputException e) {
            verifyException(e, "from Boolean value (token `JsonToken.VALUE_TRUE`)");
        }
    }
}
