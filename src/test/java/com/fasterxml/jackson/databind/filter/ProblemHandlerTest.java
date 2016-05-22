package com.fasterxml.jackson.databind.filter;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;

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
                Class<?> instClass, JsonParser p, String msg)
            throws IOException
        {
            p.skipChildren();
            return value;
        }
    }
    
    static class IntKeyMapWrapper {
        public Map<Integer,String> stuff;
    }

    static class TypeIdHandler
        extends DeserializationProblemHandler
    {
        @Override
        public JavaType handleUnknownTypeId(DeserializationContext ctxt,
                JavaType baseType, String subTypeId,
                String failureMsg)
            throws IOException
        {
            return ctxt.constructType(Impl.class);
        }
    }

    /*
    /**********************************************************
    /* Other helper types
    /**********************************************************
     */
    
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    static class Base { }
    static class Impl extends Base {
        public int a;
    }

    static class BaseWrapper {
        public Base value;
    }

    enum SingleValuedEnum {
        A;
    }

    static class BustedCtor {
        public final static BustedCtor INST = new BustedCtor(true);

        public BustedCtor() {
            throw new RuntimeException("Fail!");
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

    private final ObjectMapper MAPPER = new ObjectMapper();

    public void testWeirdKeyHandling() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper()
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
        ObjectMapper mapper = new ObjectMapper()
            .addHandler(new WeirdNumberHandler(SingleValuedEnum.A))
            ;
        SingleValuedEnum result = mapper.readValue("3", SingleValuedEnum.class);
        assertEquals(SingleValuedEnum.A, result);
    }

    public void testWeirdStringHandling() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper()
            .addHandler(new WeirdStringHandler(SingleValuedEnum.A))
            ;
        SingleValuedEnum result = mapper.readValue("\"B\"", SingleValuedEnum.class);
        assertEquals(SingleValuedEnum.A, result);
    }

    public void testInvalidTypeId() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper()
            .addHandler(new TypeIdHandler());
        BaseWrapper w = mapper.readValue("{\"value\":{\"type\":\"foo\",\"a\":4}}",
                BaseWrapper.class);
        assertNotNull(w);
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
        ObjectMapper mapper = new ObjectMapper()
            .addHandler(new InstantiationProblemHandler(BustedCtor.INST));
        BustedCtor w = mapper.readValue("{ }",
                BustedCtor.class);
        assertNotNull(w);
    }

    public void testMissingInstantiatorHandling() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper()
            .addHandler(new MissingInstantiationHandler(new NoDefaultCtor(13)))
            ;
        NoDefaultCtor w = mapper.readValue("{ \"x\" : true }", NoDefaultCtor.class);
        assertNotNull(w);
        assertEquals(13, w.value);
    }
}
