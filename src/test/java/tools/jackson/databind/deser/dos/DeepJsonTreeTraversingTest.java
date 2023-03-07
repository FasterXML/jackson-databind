package tools.jackson.databind.deser.dos;

import tools.jackson.core.*;
import tools.jackson.core.exc.StreamConstraintsException;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.databind.*;
import tools.jackson.databind.json.JsonMapper;

public class DeepJsonTreeTraversingTest extends BaseMapTest
{
    private final static int TOO_DEEP_NESTING = 2_000;

    private final JsonFactory unconstrainedFactory = JsonFactory.builder()
            .streamReadConstraints(StreamReadConstraints.builder().maxNestingDepth(Integer.MAX_VALUE).build())
            .build();
    private final ObjectMapper unconstrainedMapper = JsonMapper.builder(unconstrainedFactory).build();
    private final ObjectMapper defaultMapper = JsonMapper.builder().build();

    public void testTreeWithArrayWithDefaultConfig() throws Exception
    {
        final String doc = _nestedDoc(TOO_DEEP_NESTING, "[ ", "] ");
        try {
            defaultMapper.readTree(doc);
        } catch (StreamConstraintsException e) {
            assertEquals("Depth (1001) exceeds the maximum allowed nesting depth (1000)", e.getMessage());
        }
    }

    public void testTreeWithObjectWithDefaultConfig() throws Exception
    {
        final String doc = "{"+_nestedDoc(TOO_DEEP_NESTING, "\"x\":{", "} ") + "}";
        try {
            defaultMapper.readTree(doc);
        } catch (StreamConstraintsException e) {
            assertEquals("Depth (1001) exceeds the maximum allowed nesting depth (1000)", e.getMessage());
        }
    }

    public void testTreeWithArrayWithUnconstrainedConfig() throws Exception
    {
        final String doc = _nestedDoc(TOO_DEEP_NESTING, "[ ", "] ");
        JsonNode tree = unconstrainedMapper.readTree(doc);
        try (JsonParser jp = tree.traverse(ObjectReadContext.empty())) {
            while (jp.nextToken() != null) { }
        }
    }

    public void testTreeWithObjectWithUnconstrainedConfig() throws Exception
    {
        final String doc = "{"+_nestedDoc(TOO_DEEP_NESTING, "\"x\":{", "} ") + "}";
        JsonNode tree = unconstrainedMapper.readTree(doc);
        try (JsonParser jp = tree.traverse(ObjectReadContext.empty())) {
            while (jp.nextToken() != null) { }
        }
    }

    private String _nestedDoc(int nesting, String open, String close) {
        StringBuilder sb = new StringBuilder(nesting * (open.length() + close.length()));
        for (int i = 0; i < nesting; ++i) {
            sb.append(open);
            if ((i & 31) == 0) {
                sb.append("\n");
            }
        }
        for (int i = 0; i < nesting; ++i) {
            sb.append(close);
            if ((i & 31) == 0) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }
}
