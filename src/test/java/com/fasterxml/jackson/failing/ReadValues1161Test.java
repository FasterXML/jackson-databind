package com.fasterxml.jackson.failing;

import java.util.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;

@SuppressWarnings("resource")
public class ReadValues1161Test extends BaseMapTest
{
    static class Data1161 {
        enum Type {
            A, B, C;

            @Override
            public String toString() {
                return name().toLowerCase();
            };
        };

        public Type type;
        public String value;
    }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    private final ObjectMapper MAPPER = new ObjectMapper();

    public void testDeserProps1161() throws Exception
    {
        final String src = "[ { \"type\": \"a\", \"value\": \"1\" }, { \"type\": \"b\", \"value\": \"2\" }]";
        MappingIterator<Data1161> iterator = MAPPER
                .reader()
                .with(DeserializationFeature.READ_ENUMS_USING_TO_STRING)
                .forType(Data1161.class)
                .readValues(src);
        assertTrue(iterator.hasNext());
        Data1161 item = iterator.nextValue();
        assertNotNull(item);
        assertSame(Data1161.Type.A, item.type);
        iterator.close();
    }
}
