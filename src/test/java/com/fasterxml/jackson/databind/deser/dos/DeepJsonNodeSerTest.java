package com.fasterxml.jackson.databind.deser.dos;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DeepJsonNodeSerTest extends BaseMapTest
{
    private final ObjectMapper MAPPER = newJsonMapper();

    public void testVeryDeepNodeSer() throws Exception
    {
        int depth = 9000;
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
        String json = jsonNode.toString();
        assertNotNull(json);
    }
}
