package tools.jackson.databind.node;

import org.junit.jupiter.api.Test;

import tools.jackson.core.*;
import tools.jackson.core.exc.StreamConstraintsException;

import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link TreeTraversingParser} constructors that take explicit
 * {@link StreamReadConstraints}, for use by tree-backed format backends
 * that need their {@code TokenStreamFactory}'s constraints applied
 * regardless of {@link ObjectReadContext} given.
 */
public class TreeTraversingParserReadConstraintsTest extends DatabindTestUtil
{
    private final static int MAX_TOKENS = 4;

    private final static StreamReadConstraints CONSTRAINTS = StreamReadConstraints.builder()
            .maxTokenCount(MAX_TOKENS).build();

    private final ObjectMapper MAPPER = newJsonMapper();

    // 10 tokens: START_ARRAY, 8 x VALUE_NUMBER_INT, END_ARRAY
    private JsonNode _tree() throws Exception {
        return MAPPER.readTree("[0,1,2,3,4,5,6,7]");
    }

    @Test
    void constraintsExposed() throws Exception
    {
        try (JsonParser p = new TreeTraversingParser(_tree(), ObjectReadContext.empty(), CONSTRAINTS)) {
            assertSame(CONSTRAINTS, p.streamReadConstraints());
        }
        try (JsonParser p = new TreeTraversingParser(_tree(), ObjectReadContext.empty(), null, CONSTRAINTS)) {
            assertSame(CONSTRAINTS, p.streamReadConstraints());
        }
        // and without explicit constraints, those of read context (here: defaults)
        try (JsonParser p = new TreeTraversingParser(_tree(), ObjectReadContext.empty())) {
            assertSame(StreamReadConstraints.defaults(), p.streamReadConstraints());
            assertFalse(p.streamReadConstraints().hasMaxTokenCount());
        }
    }

    @Test
    void tokenCountEnforced() throws Exception
    {
        try (JsonParser p = new TreeTraversingParser(_tree(), ObjectReadContext.empty(), CONSTRAINTS)) {
            for (int i = 0; i < MAX_TOKENS; ++i) {
                assertNotNull(p.nextToken());
            }
            try {
                p.nextToken();
                fail("Should not pass");
            } catch (StreamConstraintsException e) {
                verifyException(e, "Token count (" + (MAX_TOKENS + 1) + ")");
                verifyException(e, "exceeds the maximum allowed (" + MAX_TOKENS);
            }
        }
    }

    @Test
    void tokenCountNotEnforcedWithoutConstraints() throws Exception
    {
        // Same tree, read context with default (unlimited) constraints: fine
        try (JsonParser p = new TreeTraversingParser(_tree(), ObjectReadContext.empty())) {
            int count = 0;
            while (p.nextToken() != null) {
                ++count;
            }
            assertEquals(10, count);
        }
    }

    // Parent context variant must retain both the parent and the constraints
    @Test
    void withParentContext() throws Exception
    {
        JsonNode root = MAPPER.readTree("{\"a\":[0,1,2,3,4,5,6,7]}");
        try (JsonParser outer = new TreeTraversingParser(root)) {
            assertToken(JsonToken.START_OBJECT, outer.nextToken());
            assertToken(JsonToken.PROPERTY_NAME, outer.nextToken());
            assertToken(JsonToken.START_ARRAY, outer.nextToken());
            JsonNode sub = root.get("a");
            try (JsonParser p = new TreeTraversingParser(sub, ObjectReadContext.empty(),
                    outer.streamReadContext().getParent(), CONSTRAINTS)) {
                assertToken(JsonToken.START_ARRAY, p.nextToken());
                assertEquals("/a", p.streamReadContext().pathAsPointer().toString());
                assertSame(CONSTRAINTS, p.streamReadConstraints());
                p.nextToken(); p.nextToken(); p.nextToken();
                assertThrows(StreamConstraintsException.class, p::nextToken);
            }
        }
    }
}
