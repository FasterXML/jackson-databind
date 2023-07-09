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
        // Keep within suitable limits; above default max (1000) but below serialization-side
        // StackOverflow limits
        int depth = 4000;
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
