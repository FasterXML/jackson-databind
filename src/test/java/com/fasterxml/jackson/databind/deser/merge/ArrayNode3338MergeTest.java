package com.fasterxml.jackson.databind.deser.merge;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

// [databind#3338]
public class ArrayNode3338MergeTest extends BaseMapTest
{
    public void testEnabledArrayNodeMerge() throws Exception {
        final ObjectMapper mapperWithMerge = sharedMapper();

        JsonNode merged = _updateTreeWithArray(mapperWithMerge);

        ObjectNode expected = mapperWithMerge.createObjectNode();
        expected.put("number", 888);

        // default behavior is to enable merge, so we get all elements
        ArrayNode array = expected.putArray("array");
        array.add("Mr.");
        array.add("Ms.");
        array.add("Mister");
        array.add("Miss");

        assertEquals(expected, merged);
    }

    public void testDisabledArrayNodeMerge() throws Exception {
        ObjectMapper mapperNoArrayMerge = jsonMapperBuilder()
                .withConfigOverride(ArrayNode.class,
                        cfg -> cfg.setMergeable(false))
                .build();

        JsonNode merged = _updateTreeWithArray(mapperNoArrayMerge);

        ObjectNode expected = mapperNoArrayMerge.createObjectNode();
        ArrayNode array = expected.putArray("array");
        array.add("Mister");
        array.add("Miss");
        expected.put("number", 888);

        assertEquals(expected, merged);

        // Also should work via JsonNode
        ObjectMapper mapperNoJsonNodeMerge = jsonMapperBuilder()
                .withConfigOverride(JsonNode.class,
                        cfg -> cfg.setMergeable(false))
                .build();

        JsonNode merged2 = _updateTreeWithArray(mapperNoJsonNodeMerge);
        assertEquals(expected, merged2);
    }

    private JsonNode _updateTreeWithArray(ObjectMapper mapper) throws Exception
    {
        JsonNode mergeTarget = mapper.readTree(a2q("{"
                + "'array': ['Mr.', 'Ms.' ],"
                + "'number': 888"
                + "}"));
        JsonNode updateNode = mapper.readTree(a2q("{"
                + "'array': ['Mister', 'Miss' ]"
                + "}"));
       return mapper.readerForUpdating(mergeTarget).readValue(updateNode);
    }

    public void testEnabledObjectNodeMerge() throws Exception {
        final ObjectMapper mapperWithMerge = sharedMapper();

        JsonNode merged = _updateTreeWithObject(mapperWithMerge);

        // default behavior is to enable merge:
        ObjectNode expected = mapperWithMerge.createObjectNode();
        ObjectNode obj = expected.putObject("object");
        obj.put("a", "1");
        obj.put("b", "xyz");

        expected.put("number", 42);
        ArrayNode array = expected.putArray("array");
        array.add(1);
        array.add(2);
        array.add(3);

        assertEquals(expected, merged);
    }

    public void testDisabledObjectNodeMerge() throws Exception {
        ObjectMapper mapperNoObjectMerge = jsonMapperBuilder()
                .withConfigOverride(ObjectNode.class,
                        cfg -> cfg.setMergeable(false))
                .build();

        JsonNode merged = _updateTreeWithObject(mapperNoObjectMerge);

        // but that can be disabled
        ObjectNode expected = mapperNoObjectMerge.createObjectNode();
        ObjectNode obj = expected.putObject("object");
        obj.put("b", "xyz");

        expected.put("number", 42);
        ArrayNode array = expected.putArray("array");
        array.add(1);
        array.add(2);
        array.add(3);

        assertEquals(expected, merged);

        // Also: verify that `JsonNode` target also works:
        ObjectMapper mapperNoJsonNodeMerge = jsonMapperBuilder()
                .withConfigOverride(JsonNode.class,
                        cfg -> cfg.setMergeable(false))
                .build();

        JsonNode merged2 = _updateTreeWithObject(mapperNoJsonNodeMerge);
        assertEquals(expected, merged2);
    }

    private JsonNode _updateTreeWithObject(ObjectMapper mapper) throws Exception
    {
        JsonNode mergeTarget = mapper.readTree(a2q("{"
                + "'object': {'a':'1', 'b':'2' },"
                + "'array': [1, 2, 3],"
                + "'number': 42"
                + "}"));
        JsonNode updateNode = mapper.readTree(a2q("{"
                + "'object': {'b':'xyz'}"
                + "}"));
       return mapper.readerForUpdating(mergeTarget).readValue(updateNode);
    }
}
