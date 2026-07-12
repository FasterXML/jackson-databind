package tools.jackson.databind.node;

import org.junit.jupiter.api.Test;

import tools.jackson.core.*;
import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

// [databind#1803]: TreeTraversingParser should allow passing of parent context
public class TreeTraversingParserParentContext1803Test
    extends DatabindTestUtil
{
    private final ObjectMapper MAPPER = newJsonMapper();

    // Without a parent context, traversal of a subtree reports paths relative
    // to that subtree (existing behavior).
    @Test
    void noParentContextByDefault() throws Exception
    {
        JsonNode root = MAPPER.readTree("{\"wrapped\":{\"x\":1}}");
        JsonNode sub = root.get("wrapped");

        try (TreeTraversingParser p = new TreeTraversingParser(sub)) {
            assertNull(p.streamReadContext().getParent());
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertToken(JsonToken.PROPERTY_NAME, p.nextToken()); // "x"
            assertEquals("/x", p.streamReadContext().pathAsPointer().toString());
        }
    }

    // Attaching a parent context (here: the context of an outer parser positioned
    // at the "wrapped" property) makes the subtree traversal report the full path.
    @Test
    void parentContextViaConstructor() throws Exception
    {
        JsonNode root = MAPPER.readTree("{\"wrapped\":{\"x\":1}}");
        JsonNode sub = root.get("wrapped");

        try (TreeTraversingParser outer = new TreeTraversingParser(root)) {
            assertToken(JsonToken.START_OBJECT, outer.nextToken());
            assertToken(JsonToken.PROPERTY_NAME, outer.nextToken()); // "wrapped"
            assertEquals("wrapped", outer.currentName());
            TokenStreamContext parent = outer.streamReadContext();
            assertEquals("/wrapped", parent.pathAsPointer().toString());

            try (TreeTraversingParser inner = new TreeTraversingParser(sub,
                    ObjectReadContext.empty(), parent)) {
                assertSame(parent, inner.streamReadContext().getParent());
                assertToken(JsonToken.START_OBJECT, inner.nextToken());
                assertToken(JsonToken.PROPERTY_NAME, inner.nextToken()); // "x"
                assertEquals("/wrapped/x",
                        inner.streamReadContext().pathAsPointer().toString());
            }
        }
    }

    // Same effect via the post-construction override setter.
    @Test
    void parentContextViaOverride() throws Exception
    {
        JsonNode root = MAPPER.readTree("{\"wrapped\":{\"x\":1}}");
        JsonNode sub = root.get("wrapped");

        try (TreeTraversingParser outer = new TreeTraversingParser(root)) {
            assertToken(JsonToken.START_OBJECT, outer.nextToken());
            assertToken(JsonToken.PROPERTY_NAME, outer.nextToken()); // "wrapped"
            TokenStreamContext parent = outer.streamReadContext();

            try (TreeTraversingParser inner = new TreeTraversingParser(sub)
                    .overrideParentContext(parent)) {
                assertSame(parent, inner.streamReadContext().getParent());
                assertToken(JsonToken.START_OBJECT, inner.nextToken());
                assertToken(JsonToken.PROPERTY_NAME, inner.nextToken()); // "x"
                assertEquals("/wrapped/x",
                        inner.streamReadContext().pathAsPointer().toString());
            }
        }
    }

    // Attaching a parent context also exposes the parent's "current value" via
    // the parent-context chain, which is the other half of what #1803 asks for.
    @Test
    void parentCurrentValueVisibleThroughChain() throws Exception
    {
        JsonNode root = MAPPER.readTree("{\"wrapped\":{\"x\":1}}");
        JsonNode sub = root.get("wrapped");
        Object marker = new Object();

        try (TreeTraversingParser outer = new TreeTraversingParser(root)) {
            assertToken(JsonToken.START_OBJECT, outer.nextToken());
            assertToken(JsonToken.PROPERTY_NAME, outer.nextToken()); // "wrapped"
            outer.assignCurrentValue(marker);
            TokenStreamContext parent = outer.streamReadContext();

            try (TreeTraversingParser inner = new TreeTraversingParser(sub)
                    .overrideParentContext(parent)) {
                assertToken(JsonToken.START_OBJECT, inner.nextToken());
                assertToken(JsonToken.PROPERTY_NAME, inner.nextToken()); // "x"
                // Inner root has no value of its own...
                assertNull(inner.streamReadContext().getParent().currentValue());
                // ...but walking up to the attached parent reaches the marker.
                assertSame(marker,
                        inner.streamReadContext().getParent().getParent().currentValue());
            }
        }
    }

    // Attaching a parent context must not disturb normal traversal of nested
    // structure within the subtree itself.
    @Test
    void nestedTraversalUnaffectedByParentContext() throws Exception
    {
        JsonNode root = MAPPER.readTree("{\"wrapped\":{\"a\":[1,2],\"b\":3}}");
        JsonNode sub = root.get("wrapped");

        try (TreeTraversingParser outer = new TreeTraversingParser(root)) {
            outer.nextToken(); // START_OBJECT
            outer.nextToken(); // PROPERTY_NAME "wrapped"
            TokenStreamContext parent = outer.streamReadContext();

            try (TreeTraversingParser inner = new TreeTraversingParser(sub)
                    .overrideParentContext(parent)) {
                assertToken(JsonToken.START_OBJECT, inner.nextToken());
                assertToken(JsonToken.PROPERTY_NAME, inner.nextToken()); // "a"
                assertToken(JsonToken.START_ARRAY, inner.nextToken());
                assertToken(JsonToken.VALUE_NUMBER_INT, inner.nextToken()); // 1
                assertEquals("/wrapped/a/0",
                        inner.streamReadContext().pathAsPointer().toString());
                assertToken(JsonToken.VALUE_NUMBER_INT, inner.nextToken()); // 2
                assertToken(JsonToken.END_ARRAY, inner.nextToken());
                assertToken(JsonToken.PROPERTY_NAME, inner.nextToken()); // "b"
                assertEquals("/wrapped/b",
                        inner.streamReadContext().pathAsPointer().toString());
                assertToken(JsonToken.VALUE_NUMBER_INT, inner.nextToken()); // 3
                assertToken(JsonToken.END_OBJECT, inner.nextToken());
                assertNull(inner.nextToken());
            }
        }
    }
}
