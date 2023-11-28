package com.fasterxml.jackson.databind.node;

import static com.fasterxml.jackson.databind.BaseMapTest.jsonMapperBuilder;
import static com.fasterxml.jackson.databind.BaseTest.a2q;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

// [databind#4229] JsonNode findValues and findParents missing expected values
public class MissingValues4229Test
{

    private final String JSON = a2q("{"
            + "    'target': 'target1'," // Found in <= 2.15.3 and 2.16.0
            + "    'object1': {"
            + "        'target': 'target2' " // Found in <= 2.15.3, but not in 2.16.0
            + "    },"
            + "    'object2': {"
            + "        'target': { " // Found in <= 2.15.3, but not in 2.16.0
            + "            'target': 'ignoredAsParentIsTarget'" // Expect not to be found (as sub-tree search ends when parent is found)
            + "        }"
            + "    }"
            + "}");

    private final ObjectMapper objectMapper = jsonMapperBuilder()
            .configure(JsonParser.Feature.ALLOW_COMMENTS, true)
            .build();

    @Test
    public void testFindValues() throws Exception
    {
        JsonNode rootNode = objectMapper.readTree(JSON);

        List<JsonNode> expectedNodes = new ArrayList<>();
        expectedNodes.add(rootNode.at("/target"));
        expectedNodes.add(rootNode.at("/object1/target"));
        expectedNodes.add(rootNode.at("/object2/target"));

        List<JsonNode> actualNodes = rootNode.findValues("target");

        Assertions.assertEquals(expectedNodes, actualNodes);
    }

    @Test
    public void testFindParents() throws Exception
    {
        JsonNode rootNode = objectMapper.readTree(JSON);

        List<JsonNode> expectedNodes = new ArrayList<>();
        expectedNodes.add(rootNode.at(""));
        expectedNodes.add(rootNode.at("/object1"));
        expectedNodes.add(rootNode.at("/object2"));

        List<JsonNode> foundNodes = rootNode.findParents("target");

        Assertions.assertEquals(expectedNodes, foundNodes);
    }
}
