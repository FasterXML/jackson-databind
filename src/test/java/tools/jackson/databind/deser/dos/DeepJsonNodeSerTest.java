package tools.jackson.databind.deser.dos;

import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.StreamWriteConstraints;
import tools.jackson.core.json.JsonFactory;

import tools.jackson.databind.BaseMapTest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

public class DeepJsonNodeSerTest extends BaseMapTest
{
    private final JsonFactory jsonFactory = JsonFactory.builder()
            .streamReadConstraints(StreamReadConstraints.builder().maxNestingDepth(Integer.MAX_VALUE).build())
            .streamWriteConstraints(StreamWriteConstraints.builder().maxNestingDepth(Integer.MAX_VALUE).build())
            .build();
    private final ObjectMapper MAPPER = JsonMapper.builder(jsonFactory).build();

    public void testVeryDeepNodeSer() throws Exception
    {
        // 10-Jul-2023, tatu: With recursive implementation there will be practical upper
        //   limit; but we are not testing specific size here but rather that users
        //   may relax/remove limits. For that all we need is level bit higher than
        //   the default there is
        int depth = StreamWriteConstraints.DEFAULT_MAX_DEPTH + 100;
        StringBuilder jsonString = new StringBuilder();
        jsonString.append("{");

        for (int i=0; i < depth; i++) {
          jsonString.append(String.format("\"abc%s\": {", i));
        }

        for (int i=0; i < depth; i++) {
          jsonString.append("}");
        }

        jsonString.append("}");

        JsonNode jsonNode = MAPPER.readTree(jsonString.toString());
        String json = MAPPER.writeValueAsString(jsonNode);
        assertNotNull(json);
    }
}
