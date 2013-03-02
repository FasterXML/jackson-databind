package com.fasterxml.jackson.failing;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TestCollectionSerialization extends BaseMapTest
{
    // [JACKSON-822]
    static interface Issue822Interface {
        public int getA();
    }

    // If this annotation is added, things will work:
    //@com.fasterxml.jackson.databind.annotation.JsonSerialize(as=Issue822Interface.class)
    // but it should not be necessary when root type is passed
    static class Issue822Impl implements Issue822Interface {
        @Override
        public int getA() { return 3; }
        public int getB() { return 9; }
    }

    // [JACKSON-822]: ensure that type can be coerced
    public void testTypedArrays() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
// Work-around when real solution not yet implemented:        
//        mapper.enable(MapperFeature.USE_STATIC_TYPING);
        assertEquals("[{\"a\":3}]", mapper.writerWithType(Issue822Interface[].class).writeValueAsString(
                new Issue822Interface[] { new Issue822Impl() }));
    }
    
    // [JACKSON-822]: ensure that type can be coerced
    public void testTypedLists() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
     // Work-around when real solution not yet implemented:        
//        mapper.enable(MapperFeature.USE_STATIC_TYPING);

        String singleJson = mapper.writerWithType(Issue822Interface.class).writeValueAsString(new Issue822Impl());
        // start with specific value case:
        assertEquals("{\"a\":3}", singleJson);

        // then lists
        List<Issue822Interface> list = new ArrayList<Issue822Interface>();
        list.add(new Issue822Impl());
        String listJson = mapper.writerWithType(new TypeReference<List<Issue822Interface>>(){})
                .writeValueAsString(list);
        assertEquals("[{\"a\":3}]", listJson);
    }

}
