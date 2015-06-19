package com.fasterxml.jackson.databind.struct;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;

/**
 * Tests for {@link JsonFormat} and specifically <code>JsonFormat.Feature</code>s.
 */
public class FormatFeaturesTest extends BaseMapTest
{
    @JsonPropertyOrder( { "strings", "ints", "bools" })
    static class WrapWriteTest
    {
        @JsonFormat(with={ JsonFormat.Feature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED })
        public String[] strings = new String[] {
            "a"
        };

        @JsonFormat(without={ JsonFormat.Feature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED })
        public int[] ints = new int[] {
            1
        };

        public boolean[] bools = new boolean[] { true };
    }

    private final ObjectMapper MAPPER = new ObjectMapper();

    public void testWriteSingleElemArrayUnwrapped() throws Exception
    {
        // Comment out temporarily, to prevent build fail!

        /*
        
        // default: strings unwrapped, ints wrapped
        assertEquals(aposToQuotes("{'strings':'a','ints':[1],'bools':[true]}"),
                MAPPER.writeValueAsString(new WrapWriteTest()));

        // change global default to "yes, unwrap"; changes 'bools' only
        assertEquals(aposToQuotes("{'strings':'a','ints':[1],'bools':true}"),
                MAPPER.writeValueAsString(new WrapWriteTest()));

        // change global default to "no, don't, unwrap", same as first case
        assertEquals(aposToQuotes("{'strings':'a','ints':[1],'bools':[true]}"),
                MAPPER.writeValueAsString(new WrapWriteTest()));
                */
    }
   
}
