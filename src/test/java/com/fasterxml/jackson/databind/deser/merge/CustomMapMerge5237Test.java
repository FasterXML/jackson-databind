package com.fasterxml.jackson.databind.deser.merge;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonMerge;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

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
             System.out.println("creator for " + map.getClass().getSimpleName());
             this.inter = inter;
             this.s = s;
        }

        public int getInter() {
             return inter;
        }

        @Override
        public String toString() {
             return map.toString() + " " + inter + " " + s;
        }

    }

    private final ObjectMapper MAPPER = newJsonMapper();
    
    // [databind#5237]: Merge for custom maps fails
    @Test
    void customMapMerging5237() throws Exception
    {
        MergeMap merge = new MergeMap(5, "f");
        merge.map.put(3, "ADS");
        System.out.println(merge);

        System.out.println(" == serializing --");

        String json = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(merge);
        System.out.println(json);

        System.out.println(" == deserializing --");

        MergeMap merge2 = MAPPER.readValue(json, MergeMap.class);

        System.out.println(" == checking --");

        System.out.println(merge2);
    }
}
