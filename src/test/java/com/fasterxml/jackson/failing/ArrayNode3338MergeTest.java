package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

// [databind#3338]
public class ArrayNode3338MergeTest extends BaseMapTest
{
    public void testEnabledArrayNodeMerge() throws Exception {
        final ObjectMapper mapperWithMerge = sharedMapper();

        JsonNode merged = _updateTree(mapperWithMerge);

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
        ObjectMapper mapperNoMerge = jsonMapperBuilder()
                .withConfigOverride(ArrayNode.class,
                        cfg -> cfg.setMergeable(false))
                .build();

        JsonNode merged = _updateTree(mapperNoMerge);

        ObjectNode expected = mapperNoMerge.createObjectNode();
        ArrayNode array = expected.putArray("array");
        array.add("Mister");
        array.add("Miss");
        expected.put("number", 888);

        assertEquals(expected, merged);
    }

    private JsonNode _updateTree(ObjectMapper mapper) throws Exception
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
}
