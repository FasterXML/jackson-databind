package tools.jackson.databind.deser.filter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

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
 * should be invoked for array-like types when given an incompatible token.
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
            // Return empty collection to allow deserialization to proceed
            return new ArrayList<>();
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

    private final ObjectMapper MAPPER = newJsonMapper();

    // Baseline: verify that handleUnexpectedToken is called for String type
    // when given an array token (this should work fine)
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

        assertTrue(handler.handleUnexpectedTokenCalled,
            "handleUnexpectedToken should be called when deserializing String from START_ARRAY");
        assertFalse(handler.handleInstantiationProblemCalled,
            "handleInstantiationProblem should NOT be called");
        assertFalse(handler.handleMissingInstantiatorCalled,
            "handleMissingInstantiator should NOT be called");
    }

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

        assertTrue(handler.handleUnexpectedTokenCalled,
            "handleUnexpectedToken should be called when deserializing Collection from STRING token");
        assertFalse(handler.handleInstantiationProblemCalled,
            "handleInstantiationProblem should NOT be called");
        assertFalse(handler.handleMissingInstantiatorCalled,
            "handleMissingInstantiator should NOT be called");
    }

    // Also test direct deserialization of Collection from string
    @Test
    public void testHandleUnexpectedTokenForDirectCollection() throws Exception
    {
        TrackingProblemHandler handler = new TrackingProblemHandler();
        ObjectMapper mapper = jsonMapperBuilder()
            .addHandler(handler)
            .build();

        mapper.readValue("\"someString\"",
            mapper.getTypeFactory().constructCollectionType(ArrayList.class, String.class));

        assertTrue(handler.handleUnexpectedTokenCalled,
            "handleUnexpectedToken should be called when deserializing Collection from STRING");
        assertFalse(handler.handleInstantiationProblemCalled,
            "handleInstantiationProblem should NOT be called");
        assertFalse(handler.handleMissingInstantiatorCalled,
            "handleMissingInstantiator should NOT be called");
    }
}
