package com.fasterxml.jackson.databind.misc;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.core.JsonToken;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.util.TokenBuffer;

public class ParsingContext2525Test extends BaseMapTest
{
    private final ObjectMapper MAPPER = sharedMapper();

    private final String MINIMAL_ARRAY_DOC = "[ 42 ]";

    private final String MINIMAL_OBJECT_DOC = "{\"answer\" : 42 }";

    private final String FULL_DOC = a2q("{'a':123,'array':[1,2,[3],5,{'obInArray':4}],"
            +"'ob':{'first':[false,true],'second':{'sub':37}},'b':true}");

    /*
    /**********************************************************************
    /* Baseline sanity check first
    /**********************************************************************
     */

    public void testAllWithRegularParser() throws Exception
    {
        try (JsonParser p = MAPPER.createParser(MINIMAL_ARRAY_DOC)) {
            _testSimpleArrayUsingPathAsPointer(p);
        }
        try (JsonParser p = MAPPER.createParser(MINIMAL_OBJECT_DOC)) {
            _testSimpleObjectUsingPathAsPointer(p);
        }
        try (JsonParser p = MAPPER.createParser(FULL_DOC)) {
            _testFullDocUsingPathAsPointer(p);
        }
    }

    /*
    /**********************************************************************
    /* Then TokenBuffer-backed tests
    /**********************************************************************
     */

    public void testSimpleArrayWithBuffer() throws Exception
    {
        try (TokenBuffer buf = _readAsTokenBuffer(MINIMAL_ARRAY_DOC)) {
            _testSimpleArrayUsingPathAsPointer(buf.asParser());
        }
    }

    public void testSimpleObjectWithBuffer() throws Exception
    {
        try (TokenBuffer buf = _readAsTokenBuffer(MINIMAL_OBJECT_DOC)) {
            _testSimpleObjectUsingPathAsPointer(buf.asParser());
        }
    }

    public void testFullDocWithBuffer() throws Exception
    {
        try (TokenBuffer buf = _readAsTokenBuffer(FULL_DOC)) {
            _testFullDocUsingPathAsPointer(buf.asParser());
        }
    }

    private TokenBuffer _readAsTokenBuffer(String doc) throws IOException
    {
        try (JsonParser p = MAPPER.createParser(doc)) {
            p.nextToken();
            try (TokenBuffer tb = new TokenBuffer(p, null)) {
                tb.copyCurrentStructure(p);
                return tb.overrideParentContext(null);
            }
        }
    }

    /*
    /**********************************************************************
    /* And Tree-backed tests
    /**********************************************************************
     */

    public void testSimpleArrayWithTree() throws Exception
    {
        JsonNode root = MAPPER.readTree(MINIMAL_ARRAY_DOC);
        try (JsonParser p = root.traverse(null)) {
            _testSimpleArrayUsingPathAsPointer(p);
        }
    }

    public void testSimpleObjectWithTree() throws Exception
    {
        JsonNode root = MAPPER.readTree(MINIMAL_OBJECT_DOC);
        try (JsonParser p = root.traverse(null)) {
            _testSimpleObjectUsingPathAsPointer(p);
        }
    }

    public void testFullDocWithTree() throws Exception
    {
        JsonNode root = MAPPER.readTree(FULL_DOC);
        try (JsonParser p = root.traverse(null)) {
            _testFullDocUsingPathAsPointer(p);
        }
    }

    /*
    /**********************************************************************
    /* Shared helper methods
    /**********************************************************************
     */

    private void _testSimpleArrayUsingPathAsPointer(JsonParser p) throws Exception
    {
        assertSame(JsonPointer.empty(), p.getParsingContext().pathAsPointer());
        assertTrue(p.getParsingContext().inRoot());

        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertSame(JsonPointer.empty(), p.getParsingContext().pathAsPointer());
        assertTrue(p.getParsingContext().inArray());

        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals("/0", p.getParsingContext().pathAsPointer().toString());

        assertToken(JsonToken.END_ARRAY, p.nextToken());
        assertSame(JsonPointer.empty(), p.getParsingContext().pathAsPointer());
        assertTrue(p.getParsingContext().inRoot());

        assertNull(p.nextToken());
    }

    private void _testSimpleObjectUsingPathAsPointer(JsonParser p) throws Exception
    {
        assertSame(JsonPointer.empty(), p.getParsingContext().pathAsPointer());
        assertTrue(p.getParsingContext().inRoot());

        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertSame(JsonPointer.empty(), p.getParsingContext().pathAsPointer());
        assertTrue(p.getParsingContext().inObject());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("/answer", p.getParsingContext().pathAsPointer().toString());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(42, p.getIntValue());
        assertEquals("/answer", p.getParsingContext().pathAsPointer().toString());

        assertToken(JsonToken.END_OBJECT, p.nextToken());
        assertSame(JsonPointer.empty(), p.getParsingContext().pathAsPointer());
        assertTrue(p.getParsingContext().inRoot());

        assertNull(p.nextToken());
    }

    private void _testFullDocUsingPathAsPointer(JsonParser p) throws Exception
    {
        // by default should just get "empty"
        assertSame(JsonPointer.empty(), p.getParsingContext().pathAsPointer());
        assertTrue(p.getParsingContext().inRoot());

        // let's just traverse, then:
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertSame(JsonPointer.empty(), p.getParsingContext().pathAsPointer());
        assertTrue(p.getParsingContext().inObject());

        assertToken(JsonToken.FIELD_NAME, p.nextToken()); // a
        assertEquals("/a", p.getParsingContext().pathAsPointer().toString());

        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals("/a", p.getParsingContext().pathAsPointer().toString());

        assertToken(JsonToken.FIELD_NAME, p.nextToken()); // array
        assertEquals("/array", p.getParsingContext().pathAsPointer().toString());
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertEquals("/array", p.getParsingContext().pathAsPointer().toString());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken()); // 1
        assertEquals("/array/0", p.getParsingContext().pathAsPointer().toString());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken()); // 2
        assertEquals("/array/1", p.getParsingContext().pathAsPointer().toString());
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertEquals("/array/2", p.getParsingContext().pathAsPointer().toString());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken()); // 3
        assertEquals("/array/2/0", p.getParsingContext().pathAsPointer().toString());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        assertEquals("/array/2", p.getParsingContext().pathAsPointer().toString());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken()); // 5
        assertEquals("/array/3", p.getParsingContext().pathAsPointer().toString());
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertEquals("/array/4", p.getParsingContext().pathAsPointer().toString());
        assertToken(JsonToken.FIELD_NAME, p.nextToken()); // obInArray
        assertEquals("/array/4/obInArray", p.getParsingContext().pathAsPointer().toString());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken()); // 4
        assertEquals("/array/4/obInArray", p.getParsingContext().pathAsPointer().toString());
        assertToken(JsonToken.END_OBJECT, p.nextToken());
        assertEquals("/array/4", p.getParsingContext().pathAsPointer().toString());
        assertToken(JsonToken.END_ARRAY, p.nextToken()); // /array
        assertEquals("/array", p.getParsingContext().pathAsPointer().toString());

        assertToken(JsonToken.FIELD_NAME, p.nextToken()); // ob
        assertEquals("/ob", p.getParsingContext().pathAsPointer().toString());
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertEquals("/ob", p.getParsingContext().pathAsPointer().toString());
        assertToken(JsonToken.FIELD_NAME, p.nextToken()); // first
        assertEquals("/ob/first", p.getParsingContext().pathAsPointer().toString());
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertEquals("/ob/first", p.getParsingContext().pathAsPointer().toString());
        assertToken(JsonToken.VALUE_FALSE, p.nextToken());
        assertEquals("/ob/first/0", p.getParsingContext().pathAsPointer().toString());
        assertToken(JsonToken.VALUE_TRUE, p.nextToken());
        assertEquals("/ob/first/1", p.getParsingContext().pathAsPointer().toString());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        assertEquals("/ob/first", p.getParsingContext().pathAsPointer().toString());
        assertToken(JsonToken.FIELD_NAME, p.nextToken()); // second
        assertEquals("/ob/second", p.getParsingContext().pathAsPointer().toString());
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertEquals("/ob/second", p.getParsingContext().pathAsPointer().toString());
        assertToken(JsonToken.FIELD_NAME, p.nextToken()); // sub
        assertEquals("/ob/second/sub", p.getParsingContext().pathAsPointer().toString());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken()); // 37
        assertEquals("/ob/second/sub", p.getParsingContext().pathAsPointer().toString());
        assertToken(JsonToken.END_OBJECT, p.nextToken());
        assertEquals("/ob/second", p.getParsingContext().pathAsPointer().toString());
        assertToken(JsonToken.END_OBJECT, p.nextToken()); // /ob
        assertEquals("/ob", p.getParsingContext().pathAsPointer().toString());

        assertToken(JsonToken.FIELD_NAME, p.nextToken()); // b
        assertEquals("/b", p.getParsingContext().pathAsPointer().toString());
        assertToken(JsonToken.VALUE_TRUE, p.nextToken());
        assertEquals("/b", p.getParsingContext().pathAsPointer().toString());

        assertToken(JsonToken.END_OBJECT, p.nextToken());
        assertSame(JsonPointer.empty(), p.getParsingContext().pathAsPointer());
        assertTrue(p.getParsingContext().inRoot());

        assertNull(p.nextToken());
    }
}
