package com.fasterxml.jackson.databind.interop;

import java.io.*;

import com.fasterxml.jackson.databind.*;

public class ExceptionSerializableTest1195 extends BaseMapTest
{
    abstract static class ClassToRead {
        public int x;
    }

    static class ContainerClassToRead {
        public ClassToRead classToRead;
    }

    public void testExceptionSerializability() throws Exception
    {
        final ObjectMapper mapper = new ObjectMapper();
        try {
            mapper.readValue("{\"type\": \"B\"}", ClassToRead.class);
            fail("Should not have passed");
        } catch (JsonMappingException e) {
            ObjectOutputStream stream = new ObjectOutputStream(new ByteArrayOutputStream());
            try {
                stream.writeObject(e);
                stream.close();
            } catch (Exception e2) {
                fail("Failed to serialize "+e.getClass().getName()+": "+e2);
            }
        }
        try {
            mapper.readValue("{\"classToRead\": {\"type\": \"B\"}}", ContainerClassToRead.class);
            fail("Should not have passed");
        } catch (JsonMappingException e) {
            ObjectOutputStream stream = new ObjectOutputStream(new ByteArrayOutputStream());
            try {
                stream.writeObject(e);
                stream.close();
            } catch (Exception e2) {
                fail("Failed to serialize "+e.getClass().getName()+": "+e2);
            }
        }
    }
}
