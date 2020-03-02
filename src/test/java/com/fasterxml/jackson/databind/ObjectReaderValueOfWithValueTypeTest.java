package com.fasterxml.jackson.databind;

import java.io.*;
import java.net.URL;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.TextNode;

import static org.junit.Assert.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// for [databind#2636]
public class ObjectReaderValueOfWithValueTypeTest
{
    final ObjectMapper MAPPER = JsonMapper.builder().build();

    static class POJO {
        public Map<String, Object> name;
    }

    private final POJO pojo = new POJO();

    @Mock
    ObjectReader objectReader;


    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testValueOfStringWithValueType() throws IOException {
        when(objectReader.readValue((String) any())).thenReturn(pojo);
        when(objectReader.forType((Class<?>) any())).thenReturn(objectReader);
        when(objectReader.readValue((String) any(), (Class<?>) any())).thenCallRealMethod();

        String source = "";
        POJO result = objectReader.readValue(source, POJO.class);

        assertEquals(result, pojo);
        verify(objectReader).forType(POJO.class);
        verify(objectReader).readValue(source);
    }


    @Test
    public void testValueOfByteArrayWithValueType() throws IOException {
        when(objectReader.forType((Class<?>) any())).thenReturn(objectReader);
        when(objectReader.readValue((byte[]) any())).thenReturn(pojo);
        when(objectReader.readValue((byte[]) any(), (Class<?>) any())).thenCallRealMethod();

        byte[] source = "{}".getBytes();
        POJO result = objectReader.readValue(source, POJO.class);

        assertEquals(result, pojo);
        verify(objectReader).forType(POJO.class);
        verify(objectReader).readValue(source);
    }

    @Test
    public void testValueOfDataInputWithValueType() throws IOException {
        when(objectReader.forType((Class<?>) any())).thenReturn(objectReader);
        when(objectReader.readValue((DataInput) any())).thenReturn(pojo);
        when(objectReader.readValue((DataInput) any(), (Class<?>) any())).thenCallRealMethod();

        DataInput source = new DataInputStream(new ByteArrayInputStream("{}".getBytes()));
        POJO result = objectReader.readValue(source, POJO.class);

        assertEquals(result, pojo);
        verify(objectReader).forType(POJO.class);
        verify(objectReader).readValue(source);
    }

    @Test
    public void testValueOfFileWithValueType() throws IOException {
        when(objectReader.forType((Class<?>) any())).thenReturn(objectReader);
        when(objectReader.readValue((File) any())).thenReturn(pojo);
        when(objectReader.readValue((File) any(), (Class<?>) any())).thenCallRealMethod();

        File source = new File("unknownpath");
        POJO result = objectReader.readValue(source, POJO.class);

        assertEquals(result, pojo);
        verify(objectReader).forType(POJO.class);
        verify(objectReader).readValue(source);
    }

    @Test
    public void testValueOfInputStreamWithValueType() throws IOException {
        when(objectReader.forType((Class<?>) any())).thenReturn(objectReader);
        when(objectReader.readValue((InputStream) any())).thenReturn(pojo);
        when(objectReader.readValue((InputStream) any(), (Class<?>) any())).thenCallRealMethod();

        InputStream source = new ByteArrayInputStream("{}".getBytes());
        POJO result = objectReader.readValue(source, POJO.class);

        assertEquals(result, pojo);
        verify(objectReader).forType(POJO.class);
        verify(objectReader).readValue(source);
    }

    @Test
    public void testValueOfJsonNodeWithValueType() throws IOException {
        when(objectReader.forType((Class<?>) any())).thenReturn(objectReader);
        when(objectReader.readValue((JsonNode) any())).thenReturn(pojo);
        when(objectReader.readValue((JsonNode) any(), (Class<?>) any())).thenCallRealMethod();

        JsonNode source = new TextNode("{}");
        POJO result = objectReader.readValue(source, POJO.class);

        assertEquals(result, pojo);
        verify(objectReader).forType(POJO.class);
        verify(objectReader).readValue(source);
    }

    @Test
    public void testValueOfReaderWithValueType() throws IOException {
        when(objectReader.forType((Class<?>) any())).thenReturn(objectReader);
        when(objectReader.readValue((Reader) any())).thenReturn(pojo);
        when(objectReader.readValue((Reader) any(), (Class<?>) any())).thenCallRealMethod();

        Reader source = new StringReader("{}");
        POJO result = objectReader.readValue(source, POJO.class);

        assertEquals(result, pojo);
        verify(objectReader).forType(POJO.class);
        verify(objectReader).readValue(source);
    }

    @Test
    public void testValueOfURLWithValueType() throws IOException {
        when(objectReader.forType((Class<?>) any())).thenReturn(objectReader);
        when(objectReader.readValue((URL) any())).thenReturn(pojo);
        when(objectReader.readValue((URL) any(), (Class<?>) any())).thenCallRealMethod();

        URL source = new URL("http://www.test.com");
        POJO result = objectReader.readValue(source, POJO.class);

        assertEquals(result, pojo);
        verify(objectReader).forType(POJO.class);
        verify(objectReader).readValue(source);
    }
}