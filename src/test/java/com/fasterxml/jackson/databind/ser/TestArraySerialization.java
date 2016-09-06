package com.fasterxml.jackson.databind.ser;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;

public class TestArraySerialization
    extends BaseMapTest
{
    private final ObjectMapper MAPPER = objectMapper();
    
    public void testLongStringArray() throws Exception
    {
        final int SIZE = 40000;

        StringBuilder sb = new StringBuilder(SIZE*2);
        for (int i = 0; i < SIZE; ++i) {
            sb.append((char) i);
        }
        String str = sb.toString();
        byte[] data = MAPPER.writeValueAsBytes(new String[] { "abc", str, null, str });
        JsonParser jp = MAPPER.getFactory().createParser(data);
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        assertToken(JsonToken.VALUE_STRING, jp.nextToken());
        assertEquals("abc", jp.getText());
        assertToken(JsonToken.VALUE_STRING, jp.nextToken());
        String actual = jp.getText();
        assertEquals(str.length(), actual.length());
        assertEquals(str, actual);
        assertToken(JsonToken.VALUE_NULL, jp.nextToken());
        assertToken(JsonToken.VALUE_STRING, jp.nextToken());
        assertEquals(str, jp.getText());
        assertToken(JsonToken.END_ARRAY, jp.nextToken());
        assertNull(jp.nextToken());
        jp.close();
    }
    
    public void testIntArray() throws Exception
    {
        String json = MAPPER.writeValueAsString(new int[] { 1, 2, 3, -7 });
        assertEquals("[1,2,3,-7]", json);
    }

    public void testBigIntArray() throws Exception
    {
        final int SIZE = 99999;
        int[] ints = new int[SIZE];
        for (int i = 0; i < ints.length; ++i) {
            ints[i] = i;
        }

        // Let's try couple of times, to ensure that state is handled
        // correctly by ObjectMapper (wrt buffer recycling used
        // with 'writeAsBytes()')
        JsonFactory f = MAPPER.getFactory();
        for (int round = 0; round < 3; ++round) {
            byte[] data = MAPPER.writeValueAsBytes(ints);
            JsonParser jp = f.createParser(data);
            assertToken(JsonToken.START_ARRAY, jp.nextToken());
            for (int i = 0; i < SIZE; ++i) {
                assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
                assertEquals(i, jp.getIntValue());
            }
            assertToken(JsonToken.END_ARRAY, jp.nextToken());
            jp.close();
        }
    }
    
    public void testLongArray() throws Exception
    {
        String json = MAPPER.writeValueAsString(new long[] { Long.MIN_VALUE, 0, Long.MAX_VALUE });
        assertEquals("["+Long.MIN_VALUE+",0,"+Long.MAX_VALUE+"]", json);
    }

    public void testStringArray() throws Exception
    {
        String json = MAPPER.writeValueAsString(new String[] { "a", "\"foo\"", null });
        assertEquals("[\"a\",\"\\\"foo\\\"\",null]", json);
    }

    public void testDoubleArray() throws Exception
    {
        String json = MAPPER.writeValueAsString(new double[] { 1.01, 2.0, -7, Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY });
        assertEquals("[1.01,2.0,-7.0,\"NaN\",\"-Infinity\",\"Infinity\"]", json);
    }

    public void testFloatArray() throws Exception
    {
        String json = MAPPER.writeValueAsString(new float[] { 1.01f, 2.0f, -7f, Float.NaN, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY });
        assertEquals("[1.01,2.0,-7.0,\"NaN\",\"-Infinity\",\"Infinity\"]", json);
    }
    
    public void testShortArrayAsDelimitedList() throws Exception		
    {
    	ShortArr arr = new ShortArr(new short[]{1, 2, 3}, new short[]{4, 5, 6});
        String json = MAPPER.writeValueAsString(arr);
        assertEquals("{\"shortArr1\":\"1,2,3\",\"shortArr2\":\"4,5,6\"}", json);
    }
    
    public void testIntArrayAsDelimitedList() throws Exception
    {
    	IntArr arr = new IntArr(new int[]{1, 2, 3}, new int[]{4, 5, 6});
        String json = MAPPER.writeValueAsString(arr);
        assertEquals("{\"intArr1\":\"1,2,3\",\"intArr2\":\"4,5,6\"}", json);
    }
    
    public void testStringArrayAsDelimitedList() throws Exception
    {
    	StringArr arr = new StringArr(new String[]{"item1", "item2", "item3"}, new String[]{"item4", "item5", "item6"});
        String json = MAPPER.writeValueAsString(arr);
        assertEquals("{\"stringArr1\":\"item1,item2,item3\",\"stringArr2\":\"item4,item5,item6\"}", json);
    }
    
    public void testNullValueArrayAsDelimitedList() throws Exception 
    {
    	ShortArr arr = new ShortArr(null, new short[]{4, 5, 6});
        String json = MAPPER.writeValueAsString(arr);
        assertEquals("{\"shortArr1\":null,\"shortArr2\":\"4,5,6\"}", json);
       
        StringArr strArr = new StringArr(new String[]{"item1", null, "item3"}, new String[]{null, "item5", "item6"});
        json = MAPPER.writeValueAsString(strArr);
        assertEquals("{\"stringArr1\":\"item1,null,item3\",\"stringArr2\":\"null,item5,item6\"}", json);
    }

    static class ShortArr {
    	@JsonFormat(shape = Shape.STRING, pattern = "\\s*,\\s*")
    	short[] shortArr1;
    	
    	@JsonFormat(shape = Shape.STRING, pattern = "")
    	short[] shortArr2;
    	
    	public ShortArr(short[] shortArr1, short[] shortArr2) {
    		this.shortArr1 = shortArr1;
    		this.shortArr2 = shortArr2;
    	}
    }
    
    static class IntArr {
    	@JsonFormat(shape = Shape.STRING, pattern = "\\s*,\\s*")
    	int[] intArr1;
    	
    	@JsonFormat(shape = Shape.STRING, pattern = "")
    	int[] intArr2;
    	
    	public IntArr(int[] intArr1, int[] intArr2) {
    		this.intArr1 = intArr1;
    		this.intArr2 = intArr2;
    	}
    }
    
    static class StringArr {
    	@JsonFormat(shape = Shape.STRING, pattern = "\\s*,\\s*")
    	String[] stringArr1;
    	
    	@JsonFormat(shape = Shape.STRING, pattern = "")
    	String[] stringArr2;
    	
    	public StringArr(String[] stringArr1, String[] stringArr2) {
    		this.stringArr1 = stringArr1;
    		this.stringArr2 = stringArr2;
    	}
    }
}
