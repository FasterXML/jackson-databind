package com.fasterxml.jackson.databind.struct;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public class FormatFeatureUnwrapSingleTest extends BaseMapTest
{
    static class StringArrayNotAnnoted {
        public String[] values;

        protected StringArrayNotAnnoted() { }
        public StringArrayNotAnnoted(String ... v) { values = v; }
    }

    @JsonPropertyOrder( { "strings", "ints", "bools" })
    static class WrapWriteWithArrays
    {
        @JsonProperty("strings")
        @JsonFormat(with={ JsonFormat.Feature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED })
        public String[] _strings = new String[] {
            "a"
        };

        @JsonFormat(without={ JsonFormat.Feature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED })
        public int[] ints = new int[] { 1 };

        public boolean[] bools = new boolean[] { true };
    }

    static class UnwrapShortArray {
        @JsonFormat(with={ JsonFormat.Feature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED })
        public short[] v = { (short) 7 };
    }

    static class UnwrapIntArray {
        @JsonFormat(with={ JsonFormat.Feature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED })
        public int[] v = { 3 };
    }

    static class UnwrapLongArray {
        @JsonFormat(with={ JsonFormat.Feature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED })
        public long[] v = { 1L };
    }

    static class UnwrapBooleanArray {
        @JsonFormat(with={ JsonFormat.Feature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED })
        public boolean[] v = { true };
    }

    static class UnwrapFloatArray {
        @JsonFormat(with={ JsonFormat.Feature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED })
        public float[] v = { 0.5f };
    }

    static class UnwrapDoubleArray {
        @JsonFormat(with={ JsonFormat.Feature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED })
        public double[] v = { 0.25 };
    }

    static class UnwrapIterable {
        @JsonFormat(with={ JsonFormat.Feature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED })
        @JsonSerialize(as=Iterable.class)
        public Iterable<String> v;

        public UnwrapIterable() {
            v = Collections.singletonList("foo");
        }

        public UnwrapIterable(String... values) {
            v = Arrays.asList(values);
        }
    }

    static class UnwrapCollection {
        @JsonFormat(with={ JsonFormat.Feature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED })
        @JsonSerialize(as=Collection.class)
        public Collection<String> v;

        public UnwrapCollection() {
            v = Collections.singletonList("foo");
        }

        public UnwrapCollection(String... values) {
            v = new LinkedHashSet<String>(Arrays.asList(values));
        }
    }

    static class UnwrapStringLike {
        @JsonFormat(with={ JsonFormat.Feature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED })
        public URI[] v = { URI.create("http://foo") };
    }

    @JsonPropertyOrder( { "strings", "ints", "bools", "enums" })
    static class WrapWriteWithCollections
    {
        @JsonFormat(with={ JsonFormat.Feature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED })
        public List<String> strings = Arrays.asList("a");

        @JsonFormat(without={ JsonFormat.Feature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED })
        public Collection<Integer> ints = Arrays.asList(Integer.valueOf(1));

        public Set<Boolean> bools = Collections.singleton(true);

        @JsonFormat(with={ JsonFormat.Feature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED })
        public EnumSet<ABC> enums = EnumSet.of(ABC.B);
    }

    /*
    /**********************************************************
    /* Test methods, writing with single-element unwrapping
    /**********************************************************
     */

    private final ObjectMapper MAPPER = new ObjectMapper();

    public void testWithArrayTypes() throws Exception
    {
        // default: strings unwrapped, ints wrapped
        assertEquals(a2q("{'strings':'a','ints':[1],'bools':[true]}"),
                MAPPER.writeValueAsString(new WrapWriteWithArrays()));

        // change global default to "yes, unwrap"; changes 'bools' only
        assertEquals(a2q("{'strings':'a','ints':[1],'bools':true}"),
                MAPPER.writer().with(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED)
                .writeValueAsString(new WrapWriteWithArrays()));

        // change global default to "no, don't, unwrap", same as first case
        assertEquals(a2q("{'strings':'a','ints':[1],'bools':[true]}"),
                MAPPER.writer().without(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED)
                .writeValueAsString(new WrapWriteWithArrays()));

        // And then without SerializationFeature but with config override:
        ObjectMapper mapper = jsonMapperBuilder()
                .withConfigOverride(String[].class,
                        v -> v.setFormat(JsonFormat.Value.empty()
                                .withFeature(JsonFormat.Feature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED)))
                .build();
        assertEquals(a2q("{'values':'a'}"),
                mapper.writeValueAsString(new StringArrayNotAnnoted("a")));
    }

    public void testWithCollectionTypes() throws Exception
    {
        // default: strings unwrapped, ints wrapped
        assertEquals(a2q("{'strings':'a','ints':[1],'bools':[true],'enums':'B'}"),
                MAPPER.writeValueAsString(new WrapWriteWithCollections()));

        // change global default to "yes, unwrap"; changes 'bools' only
        assertEquals(a2q("{'strings':'a','ints':[1],'bools':true,'enums':'B'}"),
                MAPPER.writer().with(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED)
                .writeValueAsString(new WrapWriteWithCollections()));

        // change global default to "no, don't, unwrap", same as first case
        assertEquals(a2q("{'strings':'a','ints':[1],'bools':[true],'enums':'B'}"),
                MAPPER.writer().without(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED)
                .writeValueAsString(new WrapWriteWithCollections()));
    }

    public void testUnwrapWithPrimitiveArraysEtc() throws Exception {
        assertEquals("{\"v\":7}", MAPPER.writeValueAsString(new UnwrapShortArray()));
        assertEquals("{\"v\":3}", MAPPER.writeValueAsString(new UnwrapIntArray()));
        assertEquals("{\"v\":1}", MAPPER.writeValueAsString(new UnwrapLongArray()));
        assertEquals("{\"v\":true}", MAPPER.writeValueAsString(new UnwrapBooleanArray()));

        assertEquals("{\"v\":0.5}", MAPPER.writeValueAsString(new UnwrapFloatArray()));
        assertEquals("{\"v\":0.25}", MAPPER.writeValueAsString(new UnwrapDoubleArray()));
        assertEquals("0.5",
                MAPPER.writer().with(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED)
                .writeValueAsString(new double[] { 0.5 }));

        assertEquals("{\"v\":\"foo\"}", MAPPER.writeValueAsString(new UnwrapIterable()));
        assertEquals("{\"v\":\"x\"}", MAPPER.writeValueAsString(new UnwrapIterable("x")));
        assertEquals("{\"v\":[\"x\",null]}", MAPPER.writeValueAsString(new UnwrapIterable("x", null)));

        assertEquals("{\"v\":\"foo\"}", MAPPER.writeValueAsString(new UnwrapCollection()));
        assertEquals("{\"v\":\"x\"}", MAPPER.writeValueAsString(new UnwrapCollection("x")));
        assertEquals("{\"v\":[\"x\",null]}", MAPPER.writeValueAsString(new UnwrapCollection("x", null)));

        assertEquals("{\"v\":\"http://foo\"}", MAPPER.writeValueAsString(new UnwrapStringLike()));
    }
}
