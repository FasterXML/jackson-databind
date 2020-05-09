package com.fasterxml.jackson.databind.util;

import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.AnnotatedClassResolver;
import org.junit.Test;

import static org.junit.Assert.*;

public class RecordUtilTest {

    @Test
    public void isRecord() {
        assertTrue(RecordUtil.isRecord(SimpleRecord.class));
        assertFalse(RecordUtil.isRecord(String.class));
    }

    @Test
    public void getRecordComponents() {
        assertArrayEquals(new String[]{"name", "id"}, RecordUtil.getRecordComponents(SimpleRecord.class));
        assertArrayEquals(new String[]{}, RecordUtil.getRecordComponents(String.class));
    }

    record SimpleRecord(String name, int id) {
        public SimpleRecord(int id) {
            this("", id);
        }
    }

    @Test
    public void getCanonicalConstructor() {
        DeserializationConfig config = new ObjectMapper().deserializationConfig();

        assertNotNull(null, RecordUtil.getCanonicalConstructor(
                AnnotatedClassResolver.resolve(config,
                        config.constructType(SimpleRecord.class),
                        null
                )));

        assertNull(null, RecordUtil.getCanonicalConstructor(
                AnnotatedClassResolver.resolve(config,
                        config.constructType(String.class),
                        null
                )));
    }

}