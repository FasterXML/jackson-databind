package com.fasterxml.jackson.databind.jsonschema;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Trivial test to ensure <code>JsonSchema</code> can be also deserialized
 */
public class TestReadJsonSchema
    extends DatabindTestUtil
{
    enum SchemaEnum { YES, NO; }

    static class Schemable {
        public String name;
        public char[] nameBuffer;

        // We'll include tons of stuff, just to force generation of schema
        public boolean[] states;
        public byte[] binaryData;
        public short[] shorts;
        public int[] ints;
        public long[] longs;

        public float[] floats;
        public double[] doubles;

        public Object[] objects;
        public JsonSerializable someSerializable;

        public Iterable<Object> iterableOhYeahBaby;

        public List<String> extra;
        public ArrayList<String> extra2;
        public Iterator<String[]> extra3;

        public Map<String,Double> sizes;
        public EnumMap<SchemaEnum,List<String>> whatever;

        SchemaEnum testEnum;
        public EnumSet<SchemaEnum> testEnums;
    }

    /**
     * Verifies that a simple schema that is serialized can be
     * deserialized back to equal schema instance
     */
    @SuppressWarnings("deprecation")
    @Test
    public void testDeserializeSimple() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        JsonSchema schema = mapper.generateJsonSchema(Schemable.class);
        assertNotNull(schema);

        String schemaStr = mapper.writeValueAsString(schema);
        assertNotNull(schemaStr);
        JsonSchema result = mapper.readValue(schemaStr, JsonSchema.class);
        assertEquals(schema, result, "Trying to read from '"+schemaStr+"'");
    }
}
