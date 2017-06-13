package com.fasterxml.jackson.failing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

public class MapMerge1625Test extends BaseMapTest
{

    // for [databind#1625]: should be possible to prevent deep merge
    public void testMapMergeForcedShallow() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        // prevent merging of "untyped" values
        mapper.configOverride(Object.class)
            .setMergeable(Boolean.FALSE);
        Map<String,Object> variables = new HashMap<>();
        List<String> list = new ArrayList<>();
        list.add("a");
        variables.put("list", list);
        mapper.readerForUpdating(variables).readValue(aposToQuotes("{'list':['b']}"));
        // should overwrite, not append
        List<?> l = (List<?>) variables.get("list");
        if (l.size() != 1 || !"b".equals(list.get(0))) {
            fail("Should overwrite contents, end up with entry for 'b', got: "+l);
        }
    }
}
