package com.fasterxml.jackson.databind.seq;

import java.io.Closeable;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.io.SerializedString;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;

public class SequenceWriterTest extends BaseMapTest
{
    static class Bean {
        public int a;

        public Bean(int value) { a = value; }

        @Override
        public boolean equals(Object o) {
            if (o == null || o.getClass() != getClass()) return false;
            Bean other = (Bean) o;
            return other.a == this.a;
        }
        @Override public int hashCode() { return a; }
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
    static class PolyBase {
    }

    @JsonTypeName("A")
    static class ImplA extends PolyBase {
        public int value;

        public ImplA(int v) { value = v; }
    }

    @JsonTypeName("B")
    static class ImplB extends PolyBase {
        public int b;

        public ImplB(int v) { b = v; }
    }

    static class BareBase {
        public int a = 1;
    }

    @JsonPropertyOrder({ "a", "b" })
    static class BareBaseExt extends BareBase {
        public int b = 2;
    }

    static class BareBaseCloseable extends BareBase
        implements Closeable
    {
        public int c = 3;

        boolean closed = false;

        @Override
        public void close() throws IOException {
            closed = true;
        }
    }

    static class CloseableValue implements Closeable
    {
        public int x;

        public boolean closed;

        @Override
        public void close() throws IOException {
            closed = true;
        }
    }

    /*
    /**********************************************************
    /* Test methods, simple writes
    /**********************************************************
     */

    private final ObjectMapper MAPPER = new ObjectMapper();
    private final ObjectWriter WRITER = MAPPER.writer()
            .withRootValueSeparator("\n");

    public void testSimpleNonArray() throws Exception
    {
        StringWriter strw = new StringWriter();
        SequenceWriter w = WRITER
                .forType(Bean.class)
                .writeValues(strw);
        w.write(new Bean(13))
            .write(new Bean(-6))
            .writeAll(new Bean[] { new Bean(3), new Bean(1) })
            .writeAll(Arrays.asList(new Bean(5), new Bean(7)))
        ;
        w.close();
        assertEquals(a2q("{'a':13}\n{'a':-6}\n{'a':3}\n{'a':1}\n{'a':5}\n{'a':7}"),
                strw.toString());

        strw = new StringWriter();
        JsonGenerator gen = WRITER.createGenerator(strw);
        w = WRITER
                .withRootValueSeparator(new SerializedString("/"))
                .writeValues(gen);
        w.write(new Bean(1))
            .write(new Bean(2));
        w.close();
        gen.close();
        assertEquals(a2q("{'a':1}/{'a':2}"),
                strw.toString());
    }

    public void testSimpleNonArrayNoSeparator() throws Exception
    {
        final String EXP = a2q("{'a':1}{'a':2}");

        // Also, ok to specify no separator
        StringWriter strw = new StringWriter();
        try (SequenceWriter seq = WRITER.withRootValueSeparator("")
                .writeValues(strw)) {
            seq.write(new Bean(1))
                .write(new Bean(2));
        }
        assertEquals(EXP, strw.toString());

        // 08-Mar-2021, tatu: Note that attempting to set RVS to `null`
        //   will not work in 2.x.
    }

    public void testSimpleArray() throws Exception
    {
        StringWriter strw = new StringWriter();
        SequenceWriter w = WRITER.writeValuesAsArray(strw);
        w.write(new Bean(1))
            .write(new Bean(2))
            .writeAll(new Bean[] { new Bean(-7), new Bean(2) });
        w.close();
        assertEquals(a2q("[{'a':1},{'a':2},{'a':-7},{'a':2}]"),
                strw.toString());

        strw = new StringWriter();
        JsonGenerator gen = WRITER.createGenerator(strw);
        w = WRITER.writeValuesAsArray(gen);
        Collection<Bean> bean = Collections.singleton(new Bean(3));
        w.write(new Bean(1))
            .write(null)
            .writeAll((Iterable<Bean>) bean);
        w.close();
        gen.close();
        assertEquals(a2q("[{'a':1},null,{'a':3}]"),
                strw.toString());
    }

    /*
    /**********************************************************
    /* Test methods, polymorphic writes
    /**********************************************************
     */

    @SuppressWarnings("resource")
    public void testPolymorphicNonArrayWithoutType() throws Exception
    {
        StringWriter strw = new StringWriter();
        SequenceWriter w = WRITER
                .writeValues(strw);
        w.write(new ImplA(3))
            .write(new ImplA(4))
            .close();
        assertEquals(a2q("{'type':'A','value':3}\n{'type':'A','value':4}"),
                strw.toString());
    }

    @SuppressWarnings("resource")
    public void testPolymorphicArrayWithoutType() throws Exception
    {
        StringWriter strw = new StringWriter();
        SequenceWriter w = WRITER
                .writeValuesAsArray(strw);
        w.write(new ImplA(-1))
            .write(new ImplA(6))
            .close();
        assertEquals(a2q("[{'type':'A','value':-1},{'type':'A','value':6}]"),
                strw.toString());
    }

    public void testPolymorphicArrayWithType() throws Exception
    {
        StringWriter strw = new StringWriter();
        SequenceWriter w = WRITER
                .forType(PolyBase.class)
                .writeValuesAsArray(strw);
        w.write(new ImplA(-1))
            .write(new ImplB(3))
            .write(new ImplA(7));
        w.flush();
        w.close();
        assertEquals(a2q("[{'type':'A','value':-1},{'type':'B','b':3},{'type':'A','value':7}]"),
                strw.toString());
    }

    @SuppressWarnings("resource")
    public void testSimpleCloseable() throws Exception
    {
        JsonMapper mapper = JsonMapper.builder().enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY).build();
        ObjectWriter w = mapper.writer()
                .with(SerializationFeature.CLOSE_CLOSEABLE);
        CloseableValue input = new CloseableValue();
        assertFalse(input.closed);
        StringWriter out = new StringWriter();
        SequenceWriter seq = w.writeValues(out);
        input = new CloseableValue();
        assertFalse(input.closed);
        seq.write(input);
        assertTrue(input.closed);
        seq.close();
        input.close();
        assertEquals(a2q("{'closed':false,'x':0}"), out.toString());
    }

    public void testWithExplicitType() throws Exception
    {
        ObjectWriter w = MAPPER.writer()
                // just for fun (and higher coverage):
                .without(SerializationFeature.FLUSH_AFTER_WRITE_VALUE)
                .with(SerializationFeature.CLOSE_CLOSEABLE);
        StringWriter out = new StringWriter();
        SequenceWriter seq = w.writeValues(out);
        // first full, as-is
        seq.write(new BareBaseExt());
        // but then just base type (no 'b' field)
        seq.write(new BareBaseExt(), MAPPER.constructType(BareBase.class));

        // one more. And note! Check for Closeable is for _value_, not type
        // so it's fine to expect closing here
        BareBaseCloseable cl = new BareBaseCloseable();
        seq.write(cl, MAPPER.constructType(BareBase.class));
        assertTrue(cl.closed);
        cl.close();

        seq.close();
        seq.flush();
        assertEquals(a2q("{'a':1,'b':2} {'a':1} {'a':1}"), out.toString());
    }
}
