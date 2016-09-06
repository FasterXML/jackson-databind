package com.fasterxml.jackson.databind.deser;

import java.io.*;
import java.util.*;

import static org.junit.Assert.*;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * This unit test suite tries to verify that the "Native" java type
 * mapper can properly re-construct Java array objects from Json arrays.
 */
public class TestArrayDeserialization
    extends BaseMapTest
{
    public final static class Bean1
    {
        int _x, _y;
        List<Bean2> _beans;

        // Just for deserialization:
        @SuppressWarnings("unused")
        private Bean1() { }

        public Bean1(int x, int y, List<Bean2> beans)
        {
            _x = x;
            _y = y;
            _beans = beans;
        }

        public int getX() { return _x; }
        public int getY() { return _y; }
        public List<Bean2> getBeans() { return _beans; }

        public void setX(int x) { _x = x; }
        public void setY(int y) { _y = y; }
        public void setBeans(List<Bean2> b) { _beans = b; }

        @Override public boolean equals(Object o) {
            if (!(o instanceof Bean1)) return false;
            Bean1 other = (Bean1) o;
            return (_x == other._x)
                && (_y == other._y)
                && _beans.equals(other._beans)
                ;
        }
    }

    /**
     * Simple bean that just gets serialized as a String value.
     * Deserialization from String value will be done via single-arg
     * constructor.
     */
    public final static class Bean2
        implements JsonSerializable // so we can output as simple String
    {
        final String _desc;

        public Bean2(String d)
        {
            _desc = d;
        }

        @Override
        public void serialize(JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeString(_desc);
        }

        @Override public String toString() { return _desc; }

        @Override public boolean equals(Object o) {
            if (!(o instanceof Bean2)) return false;
            Bean2 other = (Bean2) o;
            return _desc.equals(other._desc);
        }

        @Override
        public void serializeWithType(JsonGenerator jgen,
                SerializerProvider provider, TypeSerializer typeSer)
                throws IOException, JsonProcessingException {
        }
    }	

    static class ObjectWrapper {
        public Object wrapped;
    }

    static class ObjectArrayWrapper {
        public Object[] wrapped;
    }

    static class CustomNonDeserArrayDeserializer extends JsonDeserializer<NonDeserializable[]>
    {
        @Override
        public NonDeserializable[] deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException
        {
            List<NonDeserializable> list = new ArrayList<NonDeserializable>();
            while (jp.nextToken() != JsonToken.END_ARRAY) {
                list.add(new NonDeserializable(jp.getText(), false));
            }
            return list.toArray(new NonDeserializable[list.size()]);
        }
    }

    static class NonDeserializable {
        protected String value;
        
        public NonDeserializable(String v, boolean bogus) {
            value = v;
        }
    }

    static class Product { 
        public String name; 
        public List<Things> thelist; 
    }

    static class Things {
        public String height;
        public String width;
    }

    static class HiddenBinaryBean890 {
        @JsonDeserialize(as=byte[].class)
        public Object someBytes;
    }

    /*
    /**********************************************************
    /* Tests for "untyped" arrays, Object[]
    /**********************************************************
     */

    private final ObjectMapper MAPPER = new ObjectMapper();
    
    public void testUntypedArray() throws Exception
    {

        // to get "untyped" default map-to-map, pass Object[].class
        String JSON = "[ 1, null, \"x\", true, 2.0 ]";

        Object[] result = MAPPER.readValue(JSON, Object[].class);
        assertNotNull(result);

        assertEquals(5, result.length);

        assertEquals(Integer.valueOf(1), result[0]);
        assertNull(result[1]);
        assertEquals("x", result[2]);
        assertEquals(Boolean.TRUE, result[3]);
        assertEquals(Double.valueOf(2.0), result[4]);
    }

    public void testIntegerArray() throws Exception
    {
        final int LEN = 90000;

        // Let's construct array to get it big enough

        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < LEN; ++i) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(i);
        }
        sb.append(']');

        Integer[] result = MAPPER.readValue(sb.toString(), Integer[].class);
        assertNotNull(result);

        assertEquals(LEN, result.length);
        for (int i = 0; i < LEN; ++i) {
            assertEquals(i, result[i].intValue());
        }
    }

    // [JACKSON-620]: allow "" to mean 'null' for Arrays, List and Maps
    public void testFromEmptyString() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        m.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
        assertNull(m.readValue(quote(""), Object[].class));
        assertNull( m.readValue(quote(""), String[].class));
        assertNull( m.readValue(quote(""), int[].class));
    }

    // [JACKSON-620]: allow "" to mean 'null' for Arrays, List and Maps
    public void testFromEmptyString2() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        m.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
        m.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        Product p = m.readValue("{\"thelist\":\"\"}", Product.class);
        assertNotNull(p);
        assertNull(p.thelist);
    }
    
    /*
    /**********************************************************
    /* Arrays of arrays...
    /**********************************************************
     */

    public void testUntypedArrayOfArrays() throws Exception
    {
        // to get "untyped" default map-to-map, pass Object[].class
        final String JSON = "[[[-0.027512,51.503221],[-0.008497,51.503221],[-0.008497,51.509744],[-0.027512,51.509744]]]";

        Object result = MAPPER.readValue(JSON, Object.class);
        assertEquals(ArrayList.class, result.getClass());
        assertNotNull(result);

        // Should be able to get it as an Object array as well

        Object[] array = MAPPER.readValue(JSON, Object[].class);
        assertNotNull(array);
        assertEquals(Object[].class, array.getClass());

        // and as wrapped variants too
        ObjectWrapper w = MAPPER.readValue("{\"wrapped\":"+JSON+"}", ObjectWrapper.class);
        assertNotNull(w);
        assertNotNull(w.wrapped);
        assertEquals(ArrayList.class, w.wrapped.getClass());

        ObjectArrayWrapper aw = MAPPER.readValue("{\"wrapped\":"+JSON+"}", ObjectArrayWrapper.class);
        assertNotNull(aw);
        assertNotNull(aw.wrapped);
    }    
    
    /*
    /**********************************************************
    /* Tests for String arrays, char[]
    /**********************************************************
     */

    public void testStringArray() throws Exception
    {
        final String[] STRS = new String[] {
            "a", "b", "abcd", "", "???", "\"quoted\"", "lf: \n",
        };
        StringWriter sw = new StringWriter();
        JsonGenerator jg = MAPPER.getFactory().createGenerator(sw);
        jg.writeStartArray();
        for (String str : STRS) {
            jg.writeString(str);
        }
        jg.writeEndArray();
        jg.close();

        String[] result = MAPPER.readValue(sw.toString(), String[].class);
        assertNotNull(result);

        assertEquals(STRS.length, result.length);
        for (int i = 0; i < STRS.length; ++i) {
            assertEquals(STRS[i], result[i]);
        }

        // [#479]: null handling was busted in 2.4.0
        result = MAPPER.readValue(" [ null ]", String[].class);
        assertNotNull(result);
        assertEquals(1, result.length);
        assertNull(result[0]);
    }

    public void testCharArray() throws Exception
    {
        final String TEST_STR = "Let's just test it? Ok!";
        char[] result = MAPPER.readValue("\""+TEST_STR+"\"", char[].class);
        assertEquals(TEST_STR, new String(result));

        // And just for [JACKSON-289], let's verify that fluffy arrays work too
        result = MAPPER.readValue("[\"a\",\"b\",\"c\"]", char[].class);
        assertEquals("abc", new String(result));
    }

    /*
    /**********************************************************
    /* Tests for primitive arrays
    /**********************************************************
     */

    public void testBooleanArray() throws Exception
    {
        boolean[] result = MAPPER.readValue("[ true, false, false ]", boolean[].class);
        assertNotNull(result);
        assertEquals(3, result.length);
        assertTrue(result[0]);
        assertFalse(result[1]);
        assertFalse(result[2]);
    }

    public void testByteArrayAsNumbers() throws Exception
    {
        final int LEN = 37000;
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < LEN; ++i) {
            int value = i - 128;
            sb.append((value < 256) ? value : (value & 0x7F));
            sb.append(',');
        }
        sb.append("0]");
        byte[] result = MAPPER.readValue(sb.toString(), byte[].class);
        assertNotNull(result);
        assertEquals(LEN+1, result.length);
        for (int i = 0; i < LEN; ++i) {
            int value = i - 128;
            byte exp = (byte) ((value < 256) ? value : (value & 0x7F));
            if (exp != result[i]) {
                fail("At offset #"+i+" ("+result.length+"), expected "+exp+", got "+result[i]);
            }
            assertEquals(exp, result[i]);
        }
        assertEquals(0, result[LEN]);
    }

    public void testByteArrayAsBase64() throws Exception
    {
        /* Hmmh... let's use JsonGenerator here, to hopefully ensure we
         * get proper base64 encoding. Plus, not always using that
         * silly sample from Wikipedia.
         */
        JsonFactory jf = new JsonFactory();
        StringWriter sw = new StringWriter();

        int LEN = 9000;
        byte[] TEST = new byte[LEN];
        for (int i = 0; i < LEN; ++i) {
            TEST[i] = (byte) i;
        }

        JsonGenerator jg = jf.createGenerator(sw);
        jg.writeBinary(TEST);
        jg.close();
        String inputData = sw.toString();

        byte[] result = MAPPER.readValue(inputData, byte[].class);
        assertNotNull(result);
        assertArrayEquals(TEST, result);
    }

    /**
     * And then bit more challenging case; let's try decoding
     * multiple byte arrays from an array...
     */
    public void testByteArraysAsBase64() throws Exception
    {
        JsonFactory jf = new JsonFactory();
        StringWriter sw = new StringWriter(1000);

        final int entryCount = 15;

        JsonGenerator jg = jf.createGenerator(sw);
        jg.writeStartArray();

        byte[][] entries = new byte[entryCount][];
        for (int i = 0; i < entryCount; ++i) {
            byte[] b = new byte[1000 - i * 20];
            for (int x = 0; x < b.length; ++x) {
                b[x] = (byte) (i + x);
            }
            entries[i] = b;
            jg.writeBinary(b);
        }
        jg.writeEndArray();
        jg.close();

        String inputData = sw.toString();

        byte[][] result = MAPPER.readValue(inputData, byte[][].class);
        assertNotNull(result);

        assertEquals(entryCount, result.length);
        for (int i = 0; i < entryCount; ++i) {
            byte[] b = result[i];
            assertArrayEquals("Comparing entry #"+i+"/"+entryCount,entries[i], b);
        }
    }

    // [JACKSON-763]
    public void testByteArraysWith763() throws Exception
    {
        String[] input = new String[] { "YQ==", "Yg==", "Yw==" };
        byte[][] data = MAPPER.convertValue(input, byte[][].class);
        assertEquals("a", new String(data[0], "US-ASCII"));
        assertEquals("b", new String(data[1], "US-ASCII"));
        assertEquals("c", new String(data[2], "US-ASCII"));
    }
    
    public void testShortArray() throws Exception
    {
        final int LEN = 31001; // fits in signed 16-bit
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < LEN; ++i) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(i);
        }
        sb.append(']');

        short[] result = MAPPER.readValue(sb.toString(), short[].class);
        assertNotNull(result);

        assertEquals(LEN, result.length);
        for (int i = 0; i < LEN; ++i) {
            short exp = (short) i;
            assertEquals(exp, result[i]);
        }
    }

    public void testIntArray() throws Exception
    {
        final int LEN = 70000;

        // Let's construct array to get it big enough

        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < LEN; ++i) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(-i);
        }
        sb.append(']');

        int[] result = MAPPER.readValue(sb.toString(), int[].class);
        assertNotNull(result);

        assertEquals(LEN, result.length);
        for (int i = 0; i < LEN; ++i) {
            assertEquals(-i, result[i]);
        }
    }

    public void testLongArray() throws Exception
    {
        final int LEN = 12300;
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < LEN; ++i) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(i);
        }
        sb.append(']');

        long[] result = MAPPER.readValue(sb.toString(), long[].class);
        assertNotNull(result);

        assertEquals(LEN, result.length);
        for (int i = 0; i < LEN; ++i) {
            long exp = (long) i;
            assertEquals(exp, result[i]);
        }
    }

    public void testDoubleArray() throws Exception
    {
        final int LEN = 7000;
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < LEN; ++i) {
            // not ideal, but has to do...
            if (i > 0) {
                sb.append(',');
            }
            sb.append(i).append('.').append(i % 10);
        }
        sb.append(']');

        double[] result = MAPPER.readValue(sb.toString(), double[].class);
        assertNotNull(result);

        assertEquals(LEN, result.length);
        for (int i = 0; i < LEN; ++i) {
            String expStr = String.valueOf(i) + "." + String.valueOf(i % 10);
            String actStr = String.valueOf(result[i]);
            if (!expStr.equals(actStr)) {
                fail("Entry #"+i+"/"+LEN+"; exp '"+expStr+"', got '"+actStr+"'");
            }
        }
    }

    public void testFloatArray() throws Exception
    {
        final int LEN = 7000;
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < LEN; ++i) {
            if (i > 0) {
                sb.append(',');
            }
            // not ideal, but has to do...
            sb.append(i).append('.').append(i % 10);
        }
        sb.append(']');

        float[] result = MAPPER.readValue(sb.toString(), float[].class);
        assertNotNull(result);

        assertEquals(LEN, result.length);
        for (int i = 0; i < LEN; ++i) {
            String expStr = String.valueOf(i) + "." + String.valueOf(i % 10);
            assertEquals(expStr, String.valueOf(result[i]));
        }
    }

    /*
    /**********************************************************
    /* Tests for Bean arrays
    /**********************************************************
     */

    public void testBeanArray()
        throws Exception
    {
        List<Bean1> src = new ArrayList<Bean1>();

        List<Bean2> b2 = new ArrayList<Bean2>();
        b2.add(new Bean2("a"));
        b2.add(new Bean2("foobar"));
        src.add(new Bean1(1, 2, b2));

        b2 = new ArrayList<Bean2>();
        b2.add(null);
        src.add(new Bean1(4, 5, b2));

        // Ok: let's assume bean serializer works ok....
        StringWriter sw = new StringWriter();

        MAPPER.writeValue(sw, src);

        // And then test de-serializer
        List<Bean1> result = MAPPER.readValue(sw.toString(), new TypeReference<List<Bean1>>() { });
        assertNotNull(result);
        assertEquals(src, result);
    }

    /*
    /**********************************************************
    /* And special cases for byte array (base64 encoded)
    /**********************************************************
     */
    
    // for [databind#890]
    public void testByteArrayTypeOverride890() throws Exception
    {
        HiddenBinaryBean890 result = MAPPER.readValue(
                aposToQuotes("{'someBytes':'AQIDBA=='}"), HiddenBinaryBean890.class);
        assertNotNull(result);
        assertNotNull(result.someBytes);
        assertEquals(byte[].class, result.someBytes.getClass());
    }
    
    /*
    /**********************************************************
    /* And custom deserializers too
    /**********************************************************
     */

    public void testCustomDeserializers() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule testModule = new SimpleModule("test", Version.unknownVersion());
        testModule.addDeserializer(NonDeserializable[].class, new CustomNonDeserArrayDeserializer());
        mapper.registerModule(testModule);
        
        NonDeserializable[] result = mapper.readValue("[\"a\"]", NonDeserializable[].class);
        assertNotNull(result);
        assertEquals(1, result.length);
        assertEquals("a", result[0].value);
    }
    
    public void testShortArrayDeserAsDelimitedList() throws Exception		
    {
    	StringWriter sw = new StringWriter();
    	
    	ShortArr arr = new ShortArr(new short[]{1, 2, 3}, new short[]{4, 5, 6});
        MAPPER.writeValue(sw, arr);
        ShortArr arrDeser = MAPPER.readValue(sw.toString(), ShortArr.class);

        assertEquals("[1, 2, 3]", Arrays.toString(arrDeser.shortArr1));
        assertEquals("[4, 5, 6]", Arrays.toString(arrDeser.shortArr2));
    }
    
    public void testIntArrayDeserAsDelimitedList() throws Exception		
    {
    	StringWriter sw = new StringWriter();
    	
    	IntArr arr = new IntArr(new int[]{1, 2, 3}, new int[]{4, 5, 6});
        MAPPER.writeValue(sw, arr);
        IntArr arrDeser = MAPPER.readValue(sw.toString(), IntArr.class);

        assertEquals("[1, 2, 3]", Arrays.toString(arrDeser.intArr1));
        assertEquals("[4, 5, 6]", Arrays.toString(arrDeser.intArr2));
    }
    
    public void testStringArrayDeserAsDelimitedList() throws Exception		
    {
    	StringWriter sw = new StringWriter();
    	
    	StringArr arr = new StringArr(new String[]{"item1", "item2", "item3"}, new String[]{"item4", "item5", "item6"});
        MAPPER.writeValue(sw, arr);
        StringArr arrDeser = MAPPER.readValue(sw.toString(), StringArr.class);

        assertEquals("[item1, item2, item3]", Arrays.toString(arrDeser.stringArr1));
        assertEquals("[item4, item5, item6]", Arrays.toString(arrDeser.stringArr2));
    }
    
    public void testNullValueArrayDeserAsDelimitedList() throws Exception		
    {
    	StringWriter sw = new StringWriter();
    	
    	StringArr arr = new StringArr(new String[]{null, "item2", "item3"}, new String[]{"item4", null, "item6"});
        MAPPER.writeValue(sw, arr);
        StringArr arrDeser = MAPPER.readValue(sw.toString(), StringArr.class);

        assertEquals("[null, item2, item3]", Arrays.toString(arrDeser.stringArr1));
        assertEquals("[item4, null, item6]", Arrays.toString(arrDeser.stringArr2));
        
        IntArr intArr = new IntArr(null, new int[]{4, 5, 6});
        sw = new StringWriter();
        MAPPER.writeValue(sw, intArr);
        IntArr intArrDeser = MAPPER.readValue(sw.toString(), IntArr.class);

        assertEquals(null, intArrDeser.intArr1);
        assertEquals("[4, 5, 6]", Arrays.toString(intArrDeser.intArr2));
    }
    
    static class ShortArr {
    	@JsonFormat(shape = Shape.STRING, pattern = "\\s*,\\s*")
    	public short[] shortArr1;
    	
    	@JsonFormat(shape = Shape.STRING, pattern = "")
    	public short[] shortArr2;
    	
    	public ShortArr() {
    		
    	}
    	
    	public ShortArr(short[] shortArr1, short[] shortArr2) {
    		this.shortArr1 = shortArr1;
    		this.shortArr2 = shortArr2;
    	}
    }
    
    static class IntArr {
    	@JsonFormat(shape = Shape.STRING, pattern = "\\s*,\\s*")
    	public int[] intArr1;
    	
    	@JsonFormat(shape = Shape.STRING, pattern = "")
    	public int[] intArr2;
    	
    	public IntArr() {
    		
    	}
    	
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
    	
    	public StringArr() {
    		
    	}
    	
    	public StringArr(String[] stringArr1, String[] stringArr2) {
    		this.stringArr1 = stringArr1;
    		this.stringArr2 = stringArr2;
    	}
    }
    
}
