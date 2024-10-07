package tools.jackson.databind.deser.dos;

import tools.jackson.core.*;
import tools.jackson.core.exc.StreamConstraintsException;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.databind.*;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

public class DeepJsonTreeTraversingTest extends DatabindTestUtil
{
    private final static int DEFAULT_MAX_DEPTH = StreamReadConstraints.DEFAULT_MAX_DEPTH;
    private final static int TOO_DEEP_NESTING = DEFAULT_MAX_DEPTH + 1;

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
            fail("expected StreamConstraintsException");
        } catch (StreamConstraintsException e) {
            assertThat(e.getMessage())
                .startsWith("Document nesting depth ("+(DEFAULT_MAX_DEPTH+1)
                        +") exceeds the maximum allowed ("+DEFAULT_MAX_DEPTH);
        }
    }

    public void testTreeWithObjectWithDefaultConfig() throws Exception
    {
        final String doc = "{"+_nestedDoc(TOO_DEEP_NESTING, "\"x\":{", "} ") + "}";
        try {
            defaultMapper.readTree(doc);
            fail("expected StreamConstraintsException");
        } catch (StreamConstraintsException e) {
            assertThat(e.getMessage())
                .startsWith("Document nesting depth ("+(DEFAULT_MAX_DEPTH+1)
                        +") exceeds the maximum allowed ("+DEFAULT_MAX_DEPTH);
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
        try (JsonParser p = tree.traverse(ObjectReadContext.empty())) {
            while (p.nextToken() != null) { }
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
