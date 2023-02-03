package com.fasterxml.jackson.databind.deser.merge;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonMerge;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.databind.*;

public class MapMergeTest extends BaseMapTest
{
    static class MergedMap
    {
        @JsonMerge
        public Map<String,Object> values;

        protected MergedMap() {
            values = new LinkedHashMap<>();
            values.put("a", "x");
        }

        public MergedMap(String a, String b) {
            values = new LinkedHashMap<>();
            values.put(a, b);
        }

        public MergedMap(Map<String,Object> src) {
            values = src;
        }
    }

    static class MergedIntMap
    {
        @JsonMerge
        public Map<Integer,Object> values;

        protected MergedIntMap() {
            values = new LinkedHashMap<>();
            values.put(Integer.valueOf(13), "a");
        }
    }

    /*
    /********************************************************
    /* Test methods, Map merging
    /********************************************************
     */

    private final ObjectMapper MAPPER = jsonMapperBuilder()
            // 26-Oct-2016, tatu: Make sure we'll report merge problems by default
            .disable(MapperFeature.IGNORE_MERGE_FOR_UNMERGEABLE)
            .build();

    private final ObjectMapper MAPPER_SKIP_NULLS = newJsonMapper()
            .setDefaultSetterInfo(JsonSetter.Value.forContentNulls(Nulls.SKIP));
    ;

    public void testShallowMapMerging() throws Exception
    {
        final String JSON = a2q("{'values':{'c':'y','d':null}}");
        MergedMap v = MAPPER.readValue(JSON, MergedMap.class);
        assertEquals(3, v.values.size());
        assertEquals("y", v.values.get("c"));
        assertEquals("x", v.values.get("a"));
        assertNull(v.values.get("d"));

        // but also, skip nulls
        v = MAPPER_SKIP_NULLS.readValue(JSON, MergedMap.class);
        assertEquals(2, v.values.size());
        assertEquals("y", v.values.get("c"));
        assertEquals("x", v.values.get("a"));
    }

    public void testShallowNonStringMerging() throws Exception
    {
        final String JSON = a2q("{'values':{'72':'b','666':null}}");
        MergedIntMap v = MAPPER.readValue(JSON , MergedIntMap.class);
        assertEquals(3, v.values.size());
        assertEquals("a", v.values.get(Integer.valueOf(13)));
        assertEquals("b", v.values.get(Integer.valueOf(72)));
        assertNull(v.values.get(Integer.valueOf(666)));

        v = MAPPER_SKIP_NULLS.readValue(JSON , MergedIntMap.class);
        assertEquals(2, v.values.size());
        assertEquals("a", v.values.get(Integer.valueOf(13)));
        assertEquals("b", v.values.get(Integer.valueOf(72)));
    }

    @SuppressWarnings("unchecked")
    public void testDeeperMapMerging() throws Exception
    {
        // first, create base Map
        MergedMap base = new MergedMap("name", "foobar");
        Map<String,Object> props = new LinkedHashMap<>();
        props.put("default", "yes");
        props.put("x", "abc");
        Map<String,Object> innerProps = new LinkedHashMap<>();
        innerProps.put("z", Integer.valueOf(13));
        props.put("extra", innerProps);
        base.values.put("props", props);

        // to be update
        MergedMap v = MAPPER.readerForUpdating(base)
                .readValue(a2q("{'values':{'props':{'x':'xyz','y' : '...','extra':{ 'ab' : true}}}}"));
        assertEquals(2, v.values.size());
        assertEquals("foobar", v.values.get("name"));
        assertNotNull(v.values.get("props"));
        props = (Map<String,Object>) v.values.get("props");
        assertEquals(4, props.size());
        assertEquals("yes", props.get("default"));
        assertEquals("xyz", props.get("x"));
        assertEquals("...", props.get("y"));
        assertNotNull(props.get("extra"));
        innerProps = (Map<String,Object>) props.get("extra");
        assertEquals(2, innerProps.size());
        assertEquals(Integer.valueOf(13), innerProps.get("z"));
        assertEquals(Boolean.TRUE, innerProps.get("ab"));
    }

    @SuppressWarnings("unchecked")
    public void testMapMergingWithArray() throws Exception
    {
        // first, create base Map
        MergedMap base = new MergedMap("name", "foobar");
        Map<String,Object> props = new LinkedHashMap<>();
        List<String> names = new ArrayList<>();
        names.add("foo");
        props.put("names", names);
        base.values.put("props", props);
        props.put("extra", "misc");

        // to be update
        MergedMap v = MAPPER.readerForUpdating(base)
                .readValue(a2q("{'values':{'props':{'names': [ 'bar' ] }}}"));
        assertEquals(2, v.values.size());
        assertEquals("foobar", v.values.get("name"));
        assertNotNull(v.values.get("props"));
        props = (Map<String,Object>) v.values.get("props");
        assertEquals(2, props.size());
        assertEquals("misc", props.get("extra"));
        assertNotNull(props.get("names"));
        names = (List<String>) props.get("names");
        assertEquals(2, names.size());
        assertEquals("foo", names.get(0));
        assertEquals("bar", names.get(1));
    }

    /*
    /********************************************************
    /* Forcing shallow merge of root Maps:
    /********************************************************
     */

    public void testDefaultDeepMapMerge() throws Exception
    {
        // First: deep merge should be enabled by default
        HashMap<String,Object> input = new HashMap<>();
        input.put("list", new ArrayList<>(Arrays.asList("a")));

        Map<?,?> resultMap = MAPPER.readerForUpdating(input)
                .readValue(a2q("{'list':['b']}"));

        List<?> resultList = (List<?>) resultMap.get("list");
        assertEquals(Arrays.asList("a", "b"), resultList);
    }

    public void testDisabledMergeViaGlobal() throws Exception
    {
        ObjectMapper mapper = newJsonMapper();
        // disable merging, globally; does not affect main level
        mapper.setDefaultMergeable(false);

        HashMap<String,Object> input = new HashMap<>();
        input.put("list", new ArrayList<>(Arrays.asList("a")));

        Map<?,?> resultMap = mapper.readerForUpdating(input)
                .readValue(a2q("{'list':['b']}"));

        List<?> resultList = (List<?>) resultMap.get("list");

        assertEquals(Arrays.asList("b"), resultList);
    }

    public void testDisabledMergeByType() throws Exception
    {
        ObjectMapper mapper = newJsonMapper();
        // disable merging for "untyped", that is, `Object.class`
        mapper.configOverride(Object.class)
            .setMergeable(false);

        HashMap<String,Object> input = new HashMap<>();
        input.put("list", new ArrayList<>(Arrays.asList("a")));

        Map<?,?> resultMap = mapper.readerForUpdating(input)
                .readValue(a2q("{'list':['b']}"));
        List<?> resultList = (List<?>) resultMap.get("list");
        assertEquals(Arrays.asList("b"), resultList);

        // and for extra points, disable by default but ENABLE for type,
        // which should once again allow merging

        mapper = newJsonMapper();
        mapper.setDefaultMergeable(false);
        mapper.configOverride(Object.class)
            .setMergeable(true);

        input = new HashMap<>();
        input.put("list", new ArrayList<>(Arrays.asList("x")));

        resultMap = mapper.readerForUpdating(input)
                .readValue(a2q("{'list':['y']}"));
        resultList = (List<?>) resultMap.get("list");
        assertEquals(Arrays.asList("x", "y"), resultList);
    }
}
