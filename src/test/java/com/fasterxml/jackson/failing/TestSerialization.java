package com.fasterxml.jackson.failing;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TestSerialization
{

    public static class Holder
    {
        int i;
        int i0 = 0;
        int i1 = 1;

        String sEmpty = "";
        String sEmpty2 = "";
        String sNull;
        String sNull2;
        String s = "st";
        String s2 = "st2";
        String s3 = "st3";

        public Holder() {
        }

        public int getI()
        {
            return i;
        }

        public int getI1()
        {
            return i1;
        }

        public int getI0()
        {
            return i0;
        }

        public String getsEmpty()
        {
            return sEmpty;
        }

        public String getsNull()
        {
            return sNull;
        }

        public String getsEmpty2()
        {
            return sEmpty2;
        }
        public String getsNull2()
        {
            return sNull2;
        }
        public String getS()
        {
            return s;
        }

        public String getS2()
        {
            return s2;
        }

        public String getS3()
        {
            return s3;
        }

        @Override
        public String toString()
        {
            return "Holder [i=" + i + ", i0=" + i0 + ", i1=" + i1 + ", sEmpty=" + sEmpty + ", sEmpty2=" + sEmpty2
                    + ", sNull=" + sNull + ", sNull2=" + sNull2 + ", s=" + s + ", s2=" + s2 + ", s3=" + s3 + "]";
        }

    }

    @Test
    public void testInts() throws Exception
    {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(Include.NON_DEFAULT);

        Holder holder = new Holder();
        // Changing the default value of Holder.i1 to the default value of int
        holder.i1 = 0;

        String asString = objectMapper.writeValueAsString(holder);

        System.out.println(asString);

        Holder readValue = objectMapper.readValue(asString, Holder.class);
        System.out.println(readValue);

        assertEquals(holder.i, readValue.i);
        assertEquals(holder.i0, readValue.i0);
        assertEquals(holder.i1, readValue.i1);
    }

    @Test
    public void testStrings() throws Exception
    {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(Include.NON_DEFAULT);

        Holder holder = new Holder();
        // Changing the default value of Holder.s to the default value of String
        holder.s = null;
        holder.s2 = "";
        holder.sEmpty2 = null;
        holder.sNull2 = "";
        
        String asString = objectMapper.writeValueAsString(holder);

        System.out.println(asString);

        Holder readValue = objectMapper.readValue(asString, Holder.class);
        System.out.println(readValue);

        assertEquals(holder.sEmpty, readValue.sEmpty);
        assertEquals(holder.sNull, readValue.sNull);
        assertEquals(holder.sEmpty2, readValue.sEmpty2);
        assertEquals(holder.sNull2, readValue.sNull2);
        assertEquals(holder.s, readValue.s);
        assertEquals(holder.s2, readValue.s2);
        assertEquals(holder.s3, readValue.s3);
    }

    @Test
    public void testStringsAmptyAsNull() throws Exception
    {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(Include.NON_DEFAULT);

        objectMapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);

        Holder holder = new Holder();
        // Changing the default value of Holder.s to the default value of String
        holder.s = null;
        holder.s2 = "";
        //Set null to empty default & empty to null default
        holder.sEmpty2 = null;
        holder.sNull2 = "";
        
        String asString = objectMapper.writeValueAsString(holder);

        System.out.println(asString);

        Holder readValue = objectMapper.readValue(asString, Holder.class);
        System.out.println(readValue);

        assertEquals(holder.sEmpty, readValue.sEmpty);
        assertEquals(holder.sNull, readValue.sNull);
        assertEquals(holder.sEmpty2, readValue.sEmpty2);
        assertEquals(holder.sNull2, readValue.sNull2);
        assertEquals(holder.s, readValue.s);
        assertEquals(holder.s2, readValue.s2);
        assertEquals(holder.s3, readValue.s3);
    }
}
