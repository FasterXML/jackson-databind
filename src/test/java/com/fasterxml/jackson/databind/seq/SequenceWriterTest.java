package com.fasterxml.jackson.databind.seq;

import java.io.StringWriter;
import java.util.*;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.io.SerializedString;
import com.fasterxml.jackson.databind.*;

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
                .writeValues(strw);
        w.write(new Bean(13))
            .write(new Bean(-6))
            .writeAll(new Bean[] { new Bean(3), new Bean(1) })
            .writeAll(Arrays.asList(new Bean(5), new Bean(7)))
        ;
        w.close();
        assertEquals(aposToQuotes("{'a':13}\n{'a':-6}\n{'a':3}\n{'a':1}\n{'a':5}\n{'a':7}"),
                strw.toString());

        strw = new StringWriter();
        JsonGenerator gen = WRITER.getFactory().createGenerator(strw);
        w = WRITER
                .withRootValueSeparator(new SerializedString("/"))
                .writeValues(gen);
        w.write(new Bean(1))
            .write(new Bean(2));
        w.close();
        gen.close();
        assertEquals(aposToQuotes("{'a':1}/{'a':2}"),
                strw.toString());
    }

    public void testSimpleArray() throws Exception
    {
        StringWriter strw = new StringWriter();
        SequenceWriter w = WRITER.writeValuesAsArray(strw);
        w.write(new Bean(1))
            .write(new Bean(2))
            .writeAll(new Bean[] { new Bean(-7), new Bean(2) });
        w.close();
        assertEquals(aposToQuotes("[{'a':1},{'a':2},{'a':-7},{'a':2}]"),
                strw.toString());

        strw = new StringWriter();
        JsonGenerator gen = WRITER.getFactory().createGenerator(strw);
        w = WRITER.writeValuesAsArray(gen);
        Collection<Bean> bean = Collections.singleton(new Bean(3));
        w.write(new Bean(1))
            .write(null)
            .writeAll((Iterable<Bean>) bean);
        w.close();
        gen.close();
        assertEquals(aposToQuotes("[{'a':1},null,{'a':3}]"),
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
        assertEquals(aposToQuotes("{'type':'A','value':3}\n{'type':'A','value':4}"),
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
        assertEquals(aposToQuotes("[{'type':'A','value':-1},{'type':'A','value':6}]"),
                strw.toString());
    }

    @SuppressWarnings("resource")
    public void testPolymorphicArrayWithType() throws Exception
    {
        StringWriter strw = new StringWriter();
        SequenceWriter w = WRITER
                .forType(PolyBase.class)
                .writeValuesAsArray(strw);
        w.write(new ImplA(-1))
            .write(new ImplB(3))
            .write(new ImplA(7))
            .close();
        assertEquals(aposToQuotes("[{'type':'A','value':-1},{'type':'B','b':3},{'type':'A','value':7}]"),
                strw.toString());
    }
}
