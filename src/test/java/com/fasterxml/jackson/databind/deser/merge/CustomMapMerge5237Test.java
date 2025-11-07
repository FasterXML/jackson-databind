package com.fasterxml.jackson.databind.deser.merge;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonMerge;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SuppressWarnings("serial")
public class CustomMapMerge5237Test
    extends DatabindTestUtil
{
    // [databind#5237]
    interface MyMap<K, V> extends Map<K, V> {}

    static class MapImpl<K, V> extends HashMap<K, V> implements MyMap<K, V> {}

    static class MergeMap {
        int inter;
        String s;

        @JsonMerge
        public MyMap<Integer, String> map = new MapImpl<>();

        @JsonCreator
        MergeMap(@JsonProperty("inter") int inter, @JsonProperty("s") String s) {
             this.inter = inter;
             this.s = s;
        }

        public int getInter() {
             return inter;
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();
    
    // [databind#5237]: Merge for custom maps fails
    @Test
    void customMapMerging5237() throws Exception
    {
        String json = "{\n"
                + "  \"inter\" : 5,\n"
                + "  \"map\" : {\n"
                + "    \"3\" : \"ADS\"\n"
                + "  },\n"
                + "  \"s\" : \"abc\"\n"
                + "}";
        MergeMap merge2 = MAPPER.readValue(json, MergeMap.class);
        assertNotNull(merge2);
        assertEquals(Collections.singletonMap(3, "ADS"), merge2.map);
        assertEquals(5, merge2.getInter());
        assertEquals("abc", merge2.s);
    }
}
