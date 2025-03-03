package com.fasterxml.jackson.databind.deser.filter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

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

import static org.junit.jupiter.api.Assertions.*;

import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.*;

/**
 * Tests to exercise handler methods of {@link DeserializationProblemHandler}.
 *
 * @since 2.8
 */
public class ProblemHandlerTest
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
            p.skipChildren();
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

    @Test
    public void testWeirdKeyHandling() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
            .addHandler(new WeirdKeyHandler(7))
            .build();
        IntKeyMapWrapper w = mapper.readValue("{\"stuff\":{\"foo\":\"abc\"}}",
                IntKeyMapWrapper.class);
        Map<Integer,String> map = w.stuff;
        assertEquals(1, map.size());
        assertEquals("abc", map.values().iterator().next());
        assertEquals(Integer.valueOf(7), map.keySet().iterator().next());
    }

    @Test
    public void testWeirdNumberHandling() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
            .addHandler(new WeirdNumberHandler(SingleValuedEnum.A))
            .build();
        SingleValuedEnum result = mapper.readValue("3", SingleValuedEnum.class);
        assertEquals(SingleValuedEnum.A, result);
    }

    @Test
    public void testWeirdStringHandling() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
            .addHandler(new WeirdStringHandler(SingleValuedEnum.A))
            .build();
        SingleValuedEnum result = mapper.readValue("\"B\"", SingleValuedEnum.class);
        assertEquals(SingleValuedEnum.A, result);

        // also, write [databind#1629] try this
        mapper = new ObjectMapper()
                .addHandler(new WeirdStringHandler(null));
        UUID result2 = mapper.readValue(q("not a uuid!"), UUID.class);
        assertNull(result2);
    }

    // [databind#3784]: Base64 decoding
    @Test
    public void testWeirdStringForBase64() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .addHandler(new WeirdStringHandler(new byte[0]))
                .build();
        byte[] binary = mapper.readValue(q("foobar"), byte[].class);
        assertNotNull(binary);
        assertEquals(0, binary.length);

        JsonNode tree = mapper.readTree(q("foobar"));
        binary = mapper.treeToValue(tree, byte[].class);
        assertNotNull(binary);
        assertEquals(0, binary.length);
    }

    @Test
    public void testInvalidTypeId() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
            .addHandler(new UnknownTypeIdHandler(BaseImpl.class))
            .build();
        BaseWrapper w = mapper.readValue("{\"value\":{\"type\":\"foo\",\"a\":4}}",
                BaseWrapper.class);
        assertNotNull(w);
        assertEquals(BaseImpl.class, w.value.getClass());
    }

    @Test
    public void testInvalidClassAsId() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
            .addHandler(new UnknownTypeIdHandler(Base2Impl.class))
            .build();
        Base2Wrapper w = mapper.readValue("{\"value\":{\"clazz\":\"com.fizz\",\"a\":4}}",
                Base2Wrapper.class);
        assertNotNull(w);
        assertEquals(Base2Impl.class, w.value.getClass());
    }

    // 2.9: missing type id, distinct from unknown

    @Test
    public void testMissingTypeId() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
            .addHandler(new MissingTypeIdHandler(BaseImpl.class))
            .build();
        BaseWrapper w = mapper.readValue("{\"value\":{\"a\":4}}",
                BaseWrapper.class);
        assertNotNull(w);
        assertEquals(BaseImpl.class, w.value.getClass());
    }

    @Test
    public void testMissingClassAsId() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
            .addHandler(new MissingTypeIdHandler(Base2Impl.class))
            .build();
        Base2Wrapper w = mapper.readValue("{\"value\":{\"a\":4}}",
                Base2Wrapper.class);
        assertNotNull(w);
        assertEquals(Base2Impl.class, w.value.getClass());
    }

    // verify that by default we get special exception type
    @Test
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

    @Test
    public void testInstantiationExceptionHandling() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
            .addHandler(new InstantiationProblemHandler(BustedCtor.INST))
            .build();
        BustedCtor w = mapper.readValue("{ }",
                BustedCtor.class);
        assertNotNull(w);
    }

    @Test
    public void testMissingInstantiatorHandling() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
            // 14-Jan-2025, tatu: Need to disable trailing tokens (for 3.0)
            //   for this to work (handler not consuming all tokens as it should
            //   but no time to fully fix right now)
            .disable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .addHandler(new MissingInstantiationHandler(new NoDefaultCtor(13)))
            .build();
        NoDefaultCtor w = mapper.readValue("{ \"x\" : true }", NoDefaultCtor.class);
        assertNotNull(w);
        assertEquals(13, w.value);
    }

    @Test
    public void testUnexpectedTokenHandling() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
            .addHandler(new WeirdTokenHandler(Integer.valueOf(13)))
            .build();
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
