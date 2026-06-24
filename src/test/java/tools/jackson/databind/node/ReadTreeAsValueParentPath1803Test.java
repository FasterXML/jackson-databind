package tools.jackson.databind.node;

import org.junit.jupiter.api.Test;

import tools.jackson.core.*;
import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [databind#1803]: when a custom deserializer converts a buffered subtree via
// `ctxt.readTreeAsValue(...)`, the resulting parser should report the FULL document
// path (parent context linked) rather than one that restarts at the subtree.
public class ReadTreeAsValueParentPath1803Test
    extends DatabindTestUtil
{
    static class Outer {
        @JsonDeserialize(using = InnerDeserializer.class)
        public Inner inner;
    }

    static class Inner {
        public String pathSeenByDeserializer;
        public int x;
    }

    // Two-phase deserializer: read the value as a tree, then convert it back via
    // `readTreeAsValue`, capturing the path the nested deserialization observes.
    static class InnerDeserializer extends ValueDeserializer<Inner> {
        @Override
        public Inner deserialize(JsonParser p, DeserializationContext ctxt) {
            JsonNode node = ctxt.readTree(p);
            Inner result = ctxt.readTreeAsValue(node, InnerImpl.class);
            return result;
        }
    }

    // Distinct type to avoid recursive use of InnerDeserializer; its own deserializer
    // records the path it sees while being read from the tree-traversing parser.
    @JsonDeserialize(using = InnerImplDeserializer.class)
    static class InnerImpl extends Inner { }

    static class InnerImplDeserializer extends ValueDeserializer<InnerImpl> {
        @Override
        public InnerImpl deserialize(JsonParser p, DeserializationContext ctxt) {
            InnerImpl result = new InnerImpl();
            // Record path at the point the Object value starts being read
            result.pathSeenByDeserializer = p.streamReadContext().pathAsPointer().toString();
            // consume the object so parsing completes cleanly
            for (JsonToken t = p.currentToken(); t != null && t != JsonToken.END_OBJECT; t = p.nextToken()) {
                if (t == JsonToken.PROPERTY_NAME && "x".equals(p.currentName())) {
                    p.nextToken();
                    result.x = p.getIntValue();
                }
            }
            return result;
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    void readTreeAsValueSeesFullParentPath() throws Exception {
        Outer outer = MAPPER.readValue("{\"inner\":{\"x\":42}}", Outer.class);
        assertEquals(42, outer.inner.x);
        // With parent context linked, the nested deserializer sees "/inner" prefix.
        assertEquals("/inner", outer.inner.pathSeenByDeserializer);
    }
}
