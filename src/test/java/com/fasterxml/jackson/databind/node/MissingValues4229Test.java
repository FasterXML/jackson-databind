package com.fasterxml.jackson.databind.node;


import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MissingValues4229Test {
    private static final String jsonString =
            "{\n"
                    + "                      \"target\": \"target1\"," // Found in <= 2.15.3 and 2.16.0\n
                    + "                      \"object1\": {\n"
                    + "                        \"target\": \"target2\"" // Found in <= 2.15.3, but not in 2.16.0\n
                    + "                      },\n"
                    + "                      \"object2\": {\n"
                    + "                        \"target\": {" // Found in <= 2.15.3, but not in 2.16.0
                    + "                          \"target\": \"ignoredAsParentIsTarget\""
                    // Expect not to be found (as sub-tree search ends when parent is found)\n
                    + "                        }\n"
                    + "                      }\n"
                    + "                    }";

    private JsonNode rootNode;

    @BeforeEach
    public void init() throws JsonProcessingException {
        ObjectMapper objectMapper =
                new ObjectMapper().configure(JsonParser.Feature.ALLOW_COMMENTS, true);
        rootNode = objectMapper.readTree(jsonString);
    }

    @Test
    public void testFindValues() {
        List<JsonNode> foundNodes = rootNode.findValues("target");

        List<String> expectedNodePaths = new ArrayList<>();
        expectedNodePaths.add("/target");
        expectedNodePaths.add("/object1/target");
        expectedNodePaths.add("/object2/target");

        List<JsonNode> expectedNodes = expectedNodePaths.stream().map(rootNode::at).collect(Collectors.toList());

        Assertions.assertEquals(expectedNodes, foundNodes);
    }

    @Test
    public void testFindParents() {
        List<JsonNode> foundNodes = rootNode.findParents("target");

        List<JsonNode> expectedNodes = new ArrayList<>();
        expectedNodes.add(rootNode.at(""));
        expectedNodes.add(rootNode.at("/object1"));
        expectedNodes.add(rootNode.at("/object2"));

        Assertions.assertEquals(expectedNodes, foundNodes);
    }
}
