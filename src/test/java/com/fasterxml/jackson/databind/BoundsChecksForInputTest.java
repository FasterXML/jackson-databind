package com.fasterxml.jackson.databind;

// Tests for verifying [databind#3572]
public class BoundsChecksForInputTest extends BaseMapTest
{
    interface ByteBackedCreation {
        void call(byte[] data, int offset, int len) throws Exception;
    }

    interface CharBackedCreation {
        void call(char[] data, int offset, int len) throws Exception;
    }

    private final ObjectMapper MAPPER = newJsonMapper();
    private final ObjectReader OBJ_READER = MAPPER.reader();

    /*
    /**********************************************************************
    /* Test methods, byte[] backed
    /**********************************************************************
     */

    public void testBoundsWithByteArrayInput() throws Exception {
        _testBoundsWithByteArrayInput(
                (data,offset,len)->MAPPER.createParser(data, offset, len));
        _testBoundsWithByteArrayInput(
                (data,offset,len)->OBJ_READER.createParser(data, offset, len));

        _testBoundsWithByteArrayInput(
                (data,offset,len)->MAPPER.readTree(data, offset, len));

        _testBoundsWithByteArrayInput(
                (data,offset,len)->MAPPER.readValue(data, offset, len, Object.class));
        _testBoundsWithByteArrayInput(
                (data,offset,len)->OBJ_READER.readValue(data, offset, len));

        final JavaType TYPE = MAPPER.constructType(String.class);
        _testBoundsWithByteArrayInput(
                (data,offset,len)->MAPPER.readValue(data, offset, len, TYPE));
    }

    private void _testBoundsWithByteArrayInput(ByteBackedCreation creator) throws Exception
    {
        final byte[] DATA = new byte[10];
        _testBoundsWithByteArrayInput(creator, DATA, -1, 1);
        _testBoundsWithByteArrayInput(creator, DATA, 4, -1);
        _testBoundsWithByteArrayInput(creator, DATA, 4, -6);
        _testBoundsWithByteArrayInput(creator, DATA, 9, 5);
        // and the integer overflow, too
        _testBoundsWithByteArrayInput(creator, DATA, Integer.MAX_VALUE, 4);
        _testBoundsWithByteArrayInput(creator, DATA, Integer.MAX_VALUE, Integer.MAX_VALUE);
        // and null checks too
        _testBoundsWithByteArrayInput(creator, null, 0, 3);
    }

    private void _testBoundsWithByteArrayInput(ByteBackedCreation creator,
            byte[] data, int offset, int len) throws Exception
    {
        try {
            creator.call(data, offset, len);
            fail("Should not pass");
        } catch (IllegalArgumentException e) {
            if (data == null) {
                // If it gets to TokenStreamFactory we'll have:
                // verifyException(e, "Invalid `byte[]` argument: `null`");
                // But ObjectMapper/ObjectReader use different exception
                verifyException(e, "argument \"");
                verifyException(e, "is null");
            } else {
                verifyException(e, "Invalid 'offset'");
                verifyException(e, "'len'");
                verifyException(e, "arguments for `byte[]` of length "+data.length);
            }
        }
    }

    /*
    /**********************************************************************
    /* Test methods, char[] backed
    /**********************************************************************
     */

    public void testBoundsWithCharArrayInput() throws Exception {
        testBoundsWithCharArrayInput(
                (data,offset,len)->MAPPER.createParser(data, offset, len));
        testBoundsWithCharArrayInput(
                (data,offset,len)->OBJ_READER.createParser(data, offset, len));
    }

    private void testBoundsWithCharArrayInput(CharBackedCreation creator) throws Exception
    {
        final char[] DATA = new char[10];
        testBoundsWithCharArrayInput(creator, DATA, -1, 1);
        testBoundsWithCharArrayInput(creator, DATA, 4, -1);
        testBoundsWithCharArrayInput(creator, DATA, 4, -6);
        testBoundsWithCharArrayInput(creator, DATA, 9, 5);
        // and the integer overflow, too
        testBoundsWithCharArrayInput(creator, DATA, Integer.MAX_VALUE, 4);
        testBoundsWithCharArrayInput(creator, DATA, Integer.MAX_VALUE, Integer.MAX_VALUE);
        // and null checks too
        testBoundsWithCharArrayInput(creator, null, 0, 3);
    }

    private void testBoundsWithCharArrayInput(CharBackedCreation creator,
            char[] data, int offset, int len) throws Exception
    {
        try {
            creator.call(data, offset, len);
            fail("Should not pass");
        } catch (IllegalArgumentException e) {
            if (data == null) {
                // If it gets to TokenStreamFactory we'll have:
                // verifyException(e, "Invalid `char[]` argument: `null`");
                // But ObjectMapper/ObjectReader use different exception
                verifyException(e, "argument \"");
                verifyException(e, "is null");
            } else {
                verifyException(e, "Invalid 'offset'");
                verifyException(e, "'len'");
                verifyException(e, "arguments for `char[]` of length "+data.length);
            }
        }
    }
}
