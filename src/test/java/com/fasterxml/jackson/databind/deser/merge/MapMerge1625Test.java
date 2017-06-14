package com.fasterxml.jackson.databind.deser.merge;

import java.util.*;

import com.fasterxml.jackson.databind.*;

public class MapMerge1625Test extends BaseMapTest
{
    final ObjectMapper MAPPER = newObjectMapper();

    public void testDefaultDeepMapMerge() throws Exception
    {
        // First: deep merge should be enabled by default
        HashMap<String,Object> input = new HashMap<>();
        input.put("list", new ArrayList<>(Arrays.asList("a")));

        Map<?,?> resultMap = MAPPER.readerForUpdating(input)
                .readValue(aposToQuotes("{'list':['b']}"));

        List<?> resultList = (List<?>) resultMap.get("list");

        assertEquals(Arrays.asList("a", "b"), resultList);
    }

    public void testGloballyDisabledDeepMapMerge() throws Exception
    {
        ObjectMapper mapper = newObjectMapper();
        // disable merging, globally; does not affect main level
        mapper.setDefaultMergeable(false);

        HashMap<String,Object> input = new HashMap<>();
        input.put("list", new ArrayList<>(Arrays.asList("a")));

        Map<?,?> resultMap = mapper.readerForUpdating(input)
                .readValue(aposToQuotes("{'list':['b']}"));

        List<?> resultList = (List<?>) resultMap.get("list");

        assertEquals(Arrays.asList("b"), resultList);
    }
}
