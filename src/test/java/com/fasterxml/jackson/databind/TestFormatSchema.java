package com.fasterxml.jackson.databind;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.base.ParserBase;
import com.fasterxml.jackson.core.base.GeneratorBase;

/**
 * Basic tests to ensure that {@link FormatSchema} instances are properly
 * passed to {@link JsonGenerator} and {@link JsonParser} instances if
 * mapper, reader or writer is configured with one.
 */
public class TestFormatSchema extends BaseMapTest
{
    /*
    /**********************************************************************
    /* Helper classes
    /**********************************************************************
     */

    static class MySchema implements FormatSchema {
        @Override
        public String getSchemaType() { return "test"; }
    }

    static class FactoryWithSchema extends JsonFactory
    {
        @Override
        public String getFormatName() { return "test"; }

        @Override
        public boolean canUseSchema(FormatSchema schema) {
            return (schema instanceof MySchema);
        }

        private static final long serialVersionUID = 1L;
        @Override
        protected JsonParser _createParser(Reader r, IOContext ctxt)
            throws IOException
        {
            return new ParserWithSchema(ctxt, _parserFeatures);
        }

        @Override
        protected JsonGenerator _createGenerator(Writer out, IOContext ctxt) throws IOException
        {
            return new GeneratorWithSchema(_generatorFeatures, _objectCodec);
        }
    }

    // Ugly, but easiest way to get schema back is to throw exception...
    @SuppressWarnings("serial")
    static class SchemaException extends RuntimeException
    {
        public final FormatSchema _schema;

        public SchemaException(FormatSchema s) {
            _schema = s;
        }
    }

    static class ParserWithSchema extends ParserBase
    {
        public ParserWithSchema(IOContext ioCtxt, int features)
        {
            super(ioCtxt, features);
        }

        @Override
        public void setSchema(FormatSchema schema) {
            throw new SchemaException(schema);
        }

        @Override
        protected void _finishString() throws IOException { }

        @Override
        public byte[] getBinaryValue(Base64Variant b64variant) {
            return null;
        }

        @Override
        public byte[] getEmbeddedObject() {
            return null;
        }

        @Override
        public String getText() throws IOException {
            return null;
        }

        @Override
        public char[] getTextCharacters() throws IOException {
            return null;
        }

        @Override
        public int getTextLength() throws IOException {
            return 0;
        }

        @Override
        public int getTextOffset() throws IOException {
            return 0;
        }

        @Override
        public JsonToken nextToken() throws IOException {
            return null;
        }

        @Override
        public ObjectCodec getCodec() {
            return null;
        }

        @Override
        public void setCodec(ObjectCodec c) { }

        @Override
        protected void _closeInput() throws IOException {
        }

        @Override
        public int readBinaryValue(Base64Variant b64variant, OutputStream out) {
            return 0;
        }
    }

    static class GeneratorWithSchema extends GeneratorBase
    {
        public GeneratorWithSchema(int features, ObjectCodec codec)
        {
            super(features, codec);
        }

        @Override
        public void setSchema(FormatSchema schema) {
            throw new SchemaException(schema);
        }

        @Override
        protected void _releaseBuffers() { }

        @Override
        protected void _verifyValueWrite(String typeMsg) throws IOException { }

        @Override
        public void flush() throws IOException { }

        @Override
        public void writeBinary(Base64Variant b64variant, byte[] data,
                int offset, int len) throws IOException { }

        @Override
        public void writeBoolean(boolean state) throws IOException { }

        @Override
        public void writeFieldName(String name) throws IOException { }

        @Override
        public void writeNull() throws IOException { }

        @Override
        public void writeNumber(short v) throws IOException { }

        @Override
        public void writeNumber(int v) throws IOException { }

        @Override
        public void writeNumber(long v) throws IOException { }

        @Override
        public void writeNumber(BigInteger v) throws IOException { }

        @Override
        public void writeNumber(double d) throws IOException { }

        @Override
        public void writeNumber(float f) throws IOException { }

        @Override
        public void writeNumber(BigDecimal dec) throws IOException { }

        @Override
        public void writeNumber(String encodedValue) throws IOException { }

        @Override
        public void writeRaw(String text) throws IOException { }

        @Override
        public void writeRaw(String text, int offset, int len) { }

        @Override
        public void writeRaw(char[] text, int offset, int len) { }

        @Override
        public void writeRaw(char c) throws IOException { }

        @Override
        public void writeRawUTF8String(byte[] text, int offset, int length) { }

        @Override
        public void writeString(String text) throws IOException { }

        @Override
        public void writeString(char[] text, int offset, int len) { }

        @Override
        public void writeUTF8String(byte[] text, int offset, int length) { }

        @Override
        public void writeStartArray() { }

        @Override
        public void writeEndArray() throws IOException { }

        @Override
        public void writeStartObject() { }

        @Override
        public void writeEndObject() { }

        @Override
        public int writeBinary(Base64Variant b64variant, InputStream data, int dataLength) {
            return -1;
        }
    }

    /*
    /**********************************************************************
    /* Unit tests
    /**********************************************************************
     */

    public void testFormatForParsers() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper(new FactoryWithSchema());
        MySchema s = new MySchema();
        StringReader r = new StringReader("{}");
        //  bit ugly, but can't think of cleaner simple way to check this...
        try {
            mapper.reader(s).forType(Object.class).readValue(r);
            fail("Excpected exception");
        } catch (SchemaException e) {
            assertSame(s, e._schema);
        }
    }

    public void testFormatForGenerators() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper(new FactoryWithSchema());
        MySchema s = new MySchema();
        StringWriter sw = new StringWriter();
        //  bit ugly, but can't think of cleaner simple way to check this...
        try {
            mapper.writer(s).writeValue(sw, "Foobar");
            fail("Excpected exception");
        } catch (SchemaException e) {
            assertSame(s, e._schema);
        }
    }

}
