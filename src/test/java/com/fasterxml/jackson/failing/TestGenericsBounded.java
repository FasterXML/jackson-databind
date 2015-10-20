package com.fasterxml.jackson.failing;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;

import java.io.Serializable;

// Two unit tests that worked pre-2.7, but now fail.
@SuppressWarnings("serial")
public class TestGenericsBounded
    extends BaseMapTest
{
    static class Range<E extends Comparable<E>> implements Serializable
    {
         protected E start, end;

         public Range(){ }
         public Range(E start, E end) {
             this.start = start;
             this.end = end;
         }

         public E getEnd() { return end; }
         public void setEnd(E e) { end = e; }

         public E getStart() { return start; }
         public void setStart(E s) {
             start = s;
         }
    }

    static class DoubleRange extends Range<Double> {
        public DoubleRange() { }
        public DoubleRange(Double s, Double e) { super(s, e); }
    }
     
    static class BoundedWrapper<A extends Serializable>
    {
        public List<A> values;
    }

    static class IntBean implements Serializable
    {
        public int x;
    }

    static class IntBeanWrapper<T extends IntBean> {
        public T wrapped;
    }

    // Types for [JACKSON-778]

    static class Document {}
    static class Row {}
    static class RowWithDoc<D extends Document> extends Row {
        @JsonProperty("d") D d;
    }
    static class ResultSet<R extends Row> {
        @JsonProperty("rows") List<R> rows;
    }
    static class ResultSetWithDoc<D extends Document> extends ResultSet<RowWithDoc<D>> {}

    static class MyDoc extends Document {}

    /*
    /*******************************************************
    /* Unit tests
    /*******************************************************
     */

    public void testGenericsComplex() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        DoubleRange in = new DoubleRange(-0.5, 0.5);
        String json = m.writeValueAsString(in);
        DoubleRange out = m.readValue(json, DoubleRange.class);
        assertNotNull(out);
        assertEquals(-0.5, out.start);
        assertEquals(0.5, out.end);
    }

    public void testIssue778() throws Exception
    {
        final ObjectMapper mapper = new ObjectMapper();
        String json = "{\"rows\":[{\"d\":{}}]}";

        final TypeReference<?> typeRef = new TypeReference<ResultSetWithDoc<MyDoc>>() {};

        // First, verify type introspection:

        JavaType type = mapper.getTypeFactory().constructType(typeRef);
        JavaType resultSetType = type.findSuperType(ResultSet.class);
        assertNotNull(resultSetType);
        assertEquals(1, resultSetType.containedTypeCount());

        JavaType rowType = resultSetType.containedType(0);
        assertNotNull(rowType);
        assertEquals(RowWithDoc.class, rowType.getRawClass());
        
        assertEquals(1, rowType.containedTypeCount());
        JavaType docType = rowType.containedType(0);
        assertEquals(MyDoc.class, docType.getRawClass());

        // type passed is correct, but somehow it gets mangled when passed...
        ResultSetWithDoc<MyDoc> rs = mapper.readValue(json, type);
        Document d = rs.rows.iterator().next().d;
    
        assertEquals(MyDoc.class, d.getClass()); //expected MyDoc but was Document
    }
}
