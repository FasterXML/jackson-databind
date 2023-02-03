package com.fasterxml.jackson.databind.type;

import java.io.Serializable;
import java.util.*;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;

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

    // [databind#537]
    interface AnnotatedValue<E> {
        public String getAnnotation();
        public E getValue();
    }

    static class AnnotatedValueSimple<E>
        implements AnnotatedValue<E>
    {
        protected E value;

        protected AnnotatedValueSimple() { }
        public AnnotatedValueSimple(E v) { value = v; }

        @Override
        public String getAnnotation() { return null; }

        @Override
        public E getValue() { return value; }
    }

    static class CbFailing<E extends AnnotatedValue<ID>, ID>
    {
        private E item;

        public CbFailing(E item) {
            this.item = item;
        }

        public E getItem() {
            return item;
        }

        public ID getId() {
            return item.getValue();
        }
    }

    /*
    /*******************************************************
    /* Unit tests
    /*******************************************************
     */

    private final ObjectMapper MAPPER = new ObjectMapper();

    public void testLowerBound() throws Exception
    {
        IntBeanWrapper<?> result = MAPPER.readValue("{\"wrapped\":{\"x\":3}}",
                IntBeanWrapper.class);
        assertNotNull(result);
        assertEquals(IntBean.class, result.wrapped.getClass());
        assertEquals(3, result.wrapped.x);
    }

    // Test related to type bound handling problem within [JACKSON-190]
    public void testBounded() throws Exception
    {
        BoundedWrapper<IntBean> result = MAPPER.readValue
            ("{\"values\":[ {\"x\":3} ] } ", new TypeReference<BoundedWrapper<IntBean>>() {});
        List<?> list = result.values;
        assertEquals(1, list.size());
        Object ob = list.get(0);
        assertEquals(IntBean.class, ob.getClass());
        assertEquals(3, result.values.get(0).x);
    }

    public void testGenericsComplex() throws Exception
    {
        DoubleRange in = new DoubleRange(-0.5, 0.5);
        String json = MAPPER.writeValueAsString(in);
        DoubleRange out = MAPPER.readValue(json, DoubleRange.class);
        assertNotNull(out);
        assertEquals(-0.5, out.start);
        assertEquals(0.5, out.end);
    }

    public void testIssue778() throws Exception
    {
        String json = "{\"rows\":[{\"d\":{}}]}";

        final TypeReference<?> typeRef = new TypeReference<ResultSetWithDoc<MyDoc>>() {};

        // First, verify type introspection:

        JavaType type = MAPPER.getTypeFactory().constructType(typeRef);
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
        ResultSetWithDoc<MyDoc> rs = MAPPER.readValue(json, type);
        Document d = rs.rows.iterator().next().d;

        assertEquals(MyDoc.class, d.getClass()); //expected MyDoc but was Document
    }

    // [databind#537]
    public void test() throws Exception
    {
        AnnotatedValueSimple<Integer> item = new AnnotatedValueSimple<Integer>(5);
        CbFailing<AnnotatedValueSimple<Integer>, Integer> codebook = new CbFailing<AnnotatedValueSimple<Integer>, Integer>(item);
        String json = MAPPER.writeValueAsString(codebook);
        assertNotNull(json);
    }
}
