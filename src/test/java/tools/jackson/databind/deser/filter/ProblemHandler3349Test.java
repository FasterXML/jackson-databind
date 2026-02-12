package tools.jackson.databind.deser.filter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.*;
import tools.jackson.databind.deser.DeserializationProblemHandler;
import tools.jackson.databind.node.ObjectNode;

import static org.junit.jupiter.api.Assertions.*;

import static tools.jackson.databind.testutil.DatabindTestUtil.*;

/**
 * Test for [databind#3349]: DeserializationProblemHandler::handleUnexpectedToken
 * should be invoked for container types (Collections, Maps, arrays) when given
 * an incompatible String token.
 */
public class ProblemHandler3349Test
{
    // Problem handler that tracks which handler method was called
    static class TrackingProblemHandler extends DeserializationProblemHandler {
        boolean handleUnexpectedTokenCalled = false;
        boolean handleInstantiationProblemCalled = false;
        boolean handleMissingInstantiatorCalled = false;

        @Override
        public Object handleUnexpectedToken(DeserializationContext ctxt,
                JavaType targetType, JsonToken t, JsonParser p,
                String failureMsg)
        {
            handleUnexpectedTokenCalled = true;
            if (targetType.isMapLikeType()) {
                return new HashMap<>();
            }
            if (targetType.isCollectionLikeType()) {
                return new ArrayList<>();
            }
            if (targetType.isArrayType()) {
                // Return zero-length array of correct type
                return java.lang.reflect.Array.newInstance(
                        targetType.getContentType().getRawClass(), 0);
            }
            return NOT_HANDLED;
        }

        @Override
        public Object handleInstantiationProblem(DeserializationContext ctxt,
                Class<?> instClass, Object argument, Throwable t)
        {
            handleInstantiationProblemCalled = true;
            return NOT_HANDLED;
        }

        @Override
        public Object handleMissingInstantiator(DeserializationContext ctxt,
                Class<?> instClass, tools.jackson.databind.deser.ValueInstantiator inst,
                JsonParser p, String msg)
        {
            handleMissingInstantiatorCalled = true;
            return NOT_HANDLED;
        }

        void reset() {
            handleUnexpectedTokenCalled = false;
            handleInstantiationProblemCalled = false;
            handleMissingInstantiatorCalled = false;
        }
    }

    static class ArrayHolder {
        private final Collection<String> prop;

        private ArrayHolder(Collection<String> prop) {
            this.prop = prop;
        }

        @JsonCreator
        static ArrayHolder create(@JsonProperty("prop") Iterable<String> prop) {
            ArrayList<String> list = new ArrayList<>();
            prop.forEach(list::add);
            return new ArrayHolder(list);
        }

        @JsonProperty("prop")
        public Iterable<String> getProp() {
            return prop;
        }
    }

    static class StringHolder {
        private final String prop;

        @JsonCreator
        StringHolder(@JsonProperty("prop") String prop) {
            this.prop = prop;
        }

        @JsonProperty("prop")
        public String getProp() {
            return prop;
        }
    }

    /*
    /**********************************************************************
    /* Test methods: baseline
    /**********************************************************************
     */

    // Baseline: verify that handleUnexpectedToken is called for String type
    // when given an array token (this should work fine, not affected by #3349)
    @Test
    public void testHandleUnexpectedTokenForStringProp() throws Exception
    {
        TrackingProblemHandler handler = new TrackingProblemHandler();
        ObjectMapper mapper = jsonMapperBuilder()
            .addHandler(handler)
            .build();

        ObjectNode input = mapper.createObjectNode();
        input.set("prop", mapper.createArrayNode());

        try {
            mapper.treeToValue(input, StringHolder.class);
        } catch (Exception e) {
            // May fail, but we just want to check which handler was called
        }

        _verifyHandleUnexpectedTokenCalled(handler);
    }

    /*
    /**********************************************************************
    /* Test methods: Collection types
    /**********************************************************************
     */

    // [databind#3349]: handleUnexpectedToken should be called for Collection/Iterable types
    // when given a string token instead of START_ARRAY
    @Test
    public void testHandleUnexpectedTokenForCollectionProp() throws Exception
    {
        TrackingProblemHandler handler = new TrackingProblemHandler();
        ObjectMapper mapper = jsonMapperBuilder()
            .addHandler(handler)
            .build();

        ObjectNode input = mapper.createObjectNode();
        input.put("prop", "someString");

        mapper.treeToValue(input, ArrayHolder.class);

        _verifyHandleUnexpectedTokenCalled(handler);
    }

    // [databind#3349]: direct Collection<String> (StringCollectionDeserializer)
    @Test
    public void testHandleUnexpectedTokenForStringCollection() throws Exception
    {
        TrackingProblemHandler handler = new TrackingProblemHandler();
        ObjectMapper mapper = jsonMapperBuilder()
            .addHandler(handler)
            .build();

        mapper.readValue(q("someString"),
            mapper.getTypeFactory().constructCollectionType(ArrayList.class, String.class));

        _verifyHandleUnexpectedTokenCalled(handler);
    }

    // [databind#3349]: Collection<Integer> (CollectionDeserializer)
    @Test
    public void testHandleUnexpectedTokenForObjectCollection() throws Exception
    {
        TrackingProblemHandler handler = new TrackingProblemHandler();
        ObjectMapper mapper = jsonMapperBuilder()
            .addHandler(handler)
            .build();

        mapper.readValue(q("someString"),
            mapper.getTypeFactory().constructCollectionType(ArrayList.class, Integer.class));

        _verifyHandleUnexpectedTokenCalled(handler);
    }

    /*
    /**********************************************************************
    /* Test methods: Map types
    /**********************************************************************
     */

    // [databind#3349]: Map<String,String> (MapDeserializer)
    @Test
    public void testHandleUnexpectedTokenForMap() throws Exception
    {
        TrackingProblemHandler handler = new TrackingProblemHandler();
        ObjectMapper mapper = jsonMapperBuilder()
            .addHandler(handler)
            .build();

        mapper.readValue(q("someString"),
            mapper.getTypeFactory().constructMapType(HashMap.class, String.class, String.class));

        _verifyHandleUnexpectedTokenCalled(handler);
    }

    /*
    /**********************************************************************
    /* Test methods: Array types
    /**********************************************************************
     */

    // [databind#3349]: Object[] (ObjectArrayDeserializer)
    @Test
    public void testHandleUnexpectedTokenForObjectArray() throws Exception
    {
        TrackingProblemHandler handler = new TrackingProblemHandler();
        ObjectMapper mapper = jsonMapperBuilder()
            .addHandler(handler)
            .build();

        mapper.readValue(q("someString"), Object[].class);

        _verifyHandleUnexpectedTokenCalled(handler);
    }

    // [databind#3349]: String[] (StringArrayDeserializer)
    @Test
    public void testHandleUnexpectedTokenForStringArray() throws Exception
    {
        TrackingProblemHandler handler = new TrackingProblemHandler();
        ObjectMapper mapper = jsonMapperBuilder()
            .addHandler(handler)
            .build();

        mapper.readValue(q("someString"), String[].class);

        _verifyHandleUnexpectedTokenCalled(handler);
    }

    // [databind#3349]: int[] (PrimitiveArrayDeserializers)
    @Test
    public void testHandleUnexpectedTokenForIntArray() throws Exception
    {
        TrackingProblemHandler handler = new TrackingProblemHandler();
        ObjectMapper mapper = jsonMapperBuilder()
            .addHandler(handler)
            .build();

        mapper.readValue(q("someString"), int[].class);

        _verifyHandleUnexpectedTokenCalled(handler);
    }

    // [databind#3349]: long[] (PrimitiveArrayDeserializers)
    @Test
    public void testHandleUnexpectedTokenForLongArray() throws Exception
    {
        TrackingProblemHandler handler = new TrackingProblemHandler();
        ObjectMapper mapper = jsonMapperBuilder()
            .addHandler(handler)
            .build();

        mapper.readValue(q("someString"), long[].class);

        _verifyHandleUnexpectedTokenCalled(handler);
    }

    // NOTE: double[] and float[] not tested here: they have special "packed binary
    // vector" handling that intercepts STRING tokens (base64) before handleNonArray

    // [databind#3349]: boolean[] (PrimitiveArrayDeserializers)
    @Test
    public void testHandleUnexpectedTokenForBooleanArray() throws Exception
    {
        TrackingProblemHandler handler = new TrackingProblemHandler();
        ObjectMapper mapper = jsonMapperBuilder()
            .addHandler(handler)
            .build();

        mapper.readValue(q("someString"), boolean[].class);

        _verifyHandleUnexpectedTokenCalled(handler);
    }

    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */

    private void _verifyHandleUnexpectedTokenCalled(TrackingProblemHandler handler) {
        assertTrue(handler.handleUnexpectedTokenCalled,
            "handleUnexpectedToken should have been called");
        assertFalse(handler.handleInstantiationProblemCalled,
            "handleInstantiationProblem should NOT have been called");
        assertFalse(handler.handleMissingInstantiatorCalled,
            "handleMissingInstantiator should NOT have been called");
    }
}
