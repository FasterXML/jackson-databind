package com.fasterxml.jackson.databind.seq;

import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;

/**
 * Test(s) for [databind#823], adding support for reading root-level
 * sequences.
 */
public class MappingIteratorReadTest extends BaseMapTest
{
    public void testSimple() throws Exception
    {
        final ObjectMapper MAPPER = new ObjectMapper();
        final String JSON = aposToQuotes("{'str':'a'}{'str':'b'}\n  {'str':'c'}");
        MappingIterator<StringWrapper> it = MAPPER
                .readValue(JSON, new TypeReference<MappingIterator<StringWrapper>>() { });
        List<StringWrapper> all = it.readAll();
        assertEquals(3, all.size());
        assertEquals("c", all.get(2).str);
        it.close();
    }
}
