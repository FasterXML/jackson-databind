package com.fasterxml.jackson.databind.jdk21;

import java.util.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Java21CollectionsTest extends DatabindTestUtil
{
    // [databind#4089]
    record SequencedCollections(
            SequencedCollection<String> sequencedCollection,
            SequencedSet<String> sequencedSet,
            SequencedMap<String, Integer> sequencedMap) {
    }

    public void testSequencedCollectionTypesDeserialize() throws Exception {
        String json = """
                {
                    "sequencedCollection": ["A", "B"],
                    "sequencedSet": ["C", "D"],
                    "sequencedMap": {"A": 1, "B": 2}
                }
                """;

        ObjectMapper objectMapper = JsonMapper.builder().build();
        SequencedCollections value = objectMapper.readValue(json, SequencedCollections.class);
        assertEquals(ArrayList.class, value.sequencedCollection.getClass());
        assertEquals(LinkedHashSet.class, value.sequencedSet.getClass());
        assertEquals(LinkedHashMap.class, value.sequencedMap.getClass());
    }

    public void testSequencedCollectionTypesRoundTrip() throws Exception {
        ArrayList<String> arrayList = new ArrayList<>();
        arrayList.add("A");
        arrayList.add("B");
        LinkedHashSet<String> linkedHashSet = new LinkedHashSet<>();
        linkedHashSet.add("C");
        linkedHashSet.add("D");
        LinkedHashMap<String, Integer> linkedHashMap = new LinkedHashMap<>();
        linkedHashMap.put("A", 1);
        linkedHashMap.put("B", 2);
        SequencedCollections input = new SequencedCollections(arrayList, linkedHashSet, linkedHashMap);

        ObjectMapper objectMapper = JsonMapper.builder().build();
        String json = objectMapper.writeValueAsString(input);
        SequencedCollections value = objectMapper.readValue(json, SequencedCollections.class);
        assertEquals(input, value);
    }
}
