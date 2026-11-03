package tools.jackson.databind.type;

import java.io.Serializable;
import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for generic type handling in serialization/deserialization:
 * bounded generics, generic fields in subtypes, local types, type aliases,
 * and polymorphic collection types.
 */
@SuppressWarnings("serial")
public class GenericTypeTest extends DatabindTestUtil
{
    /*
    /**********************************************************
    /* Helper types for bounded generics [JACKSON-778], [databind#537]
    /**********************************************************
     */

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
    /**********************************************************
    /* Helper types for generic fields in subtypes [JACKSON-677], [JACKSON-887]
    /**********************************************************
     */

    // [JACKSON-677]
    static class Result677<T> {
        public static class Success677<K> extends Result677<K> {
            public K value;

            public Success677() { }
            public Success677(K k) { value = k; }
        }
    }

    // [JACKSON-887]
    static abstract class BaseType<T> {
        public T value;

        public final static class SubType<T extends Number> extends BaseType<T>
        {
        }
    }

    /*
    /**********************************************************
    /* Helper types for local type resolution [databind#609]
    /**********************************************************
     */

    static class EntityContainer {
        RuleForm entity;

        @SuppressWarnings("unchecked")
        public <T extends RuleForm> T getEntity() { return (T) entity; }
        public <T extends RuleForm> void setEntity(T e) { entity = e; }
    }

    static class RuleForm {
        public int value;

        public RuleForm() { }
        public RuleForm(int v) { value = v; }
    }

    /*
    /**********************************************************
    /* Helper types for type aliases [databind#743]
    /**********************************************************
     */

    public static abstract class Base743<T> {
        public T inconsequential = null;
    }

    public static abstract class BaseData743<T> {
        public T dataObj;
    }

    public static class Child743 extends Base743<Long> {
        public static class ChildData extends BaseData743<List<String>> { }
    }

    /*
    /**********************************************************
    /* Helper types for polymorphic collection [databind#936]
    /**********************************************************
     */

    static class StringyList<T extends Serializable> implements Collection<T> {
        private Collection<T> _stuff;

        @JsonCreator
        public StringyList(Collection<T> src) {
            _stuff = new ArrayList<T>(src);
        }

        public StringyList() {
            _stuff = new ArrayList<T>();
        }

        @Override
        public boolean add(T arg) {
            return _stuff.add(arg);
        }

        @Override
        public boolean addAll(Collection<? extends T> args) {
            return _stuff.addAll(args);
        }

        @Override
        public void clear() {
            _stuff.clear();
        }

        @Override
        public boolean contains(Object arg) {
            return _stuff.contains(arg);
        }

        @Override
        public boolean containsAll(Collection<?> args) {
            return _stuff.containsAll(args);
        }

        @Override
        public boolean isEmpty() {
            return _stuff.isEmpty();
        }

        @Override
        public Iterator<T> iterator() {
            return _stuff.iterator();
        }

        @Override
        public boolean remove(Object arg) {
            return _stuff.remove(arg);
        }

        @Override
        public boolean removeAll(Collection<?> args) {
            return _stuff.removeAll(args);
        }

        @Override
        public boolean retainAll(Collection<?> args) {
            return _stuff.retainAll(args);
        }

        @Override
        public int size() {
            return _stuff.size();
        }

        @Override
        public Object[] toArray() {
            return _stuff.toArray();
        }

        @Override
        public <X> X[] toArray(X[] arg) {
            return _stuff.toArray(arg);
        }
    }

    /*
    /**********************************************************
    /* Test methods, bounded generics
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testLowerBound() throws Exception
    {
        IntBeanWrapper<?> result = MAPPER.readValue("{\"wrapped\":{\"x\":3}}",
                IntBeanWrapper.class);
        assertNotNull(result);
        assertEquals(IntBean.class, result.wrapped.getClass());
        assertEquals(3, result.wrapped.x);
    }

    // Test related to type bound handling problem within [JACKSON-190]
    @Test
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

    @Test
    public void testGenericsComplex() throws Exception
    {
        DoubleRange in = new DoubleRange(-0.5, 0.5);
        String json = MAPPER.writeValueAsString(in);
        DoubleRange out = MAPPER.readValue(json, DoubleRange.class);
        assertNotNull(out);
        assertEquals(-0.5, out.start);
        assertEquals(0.5, out.end);
    }

    // [JACKSON-778]
    @Test
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

        ResultSetWithDoc<MyDoc> rs = MAPPER.readValue(json, type);
        Document d = rs.rows.iterator().next().d;

        assertEquals(MyDoc.class, d.getClass());
    }

    // [databind#537]
    @Test
    public void testCrossReferencingGenericBounds() throws Exception
    {
        AnnotatedValueSimple<Integer> item = new AnnotatedValueSimple<Integer>(5);
        CbFailing<AnnotatedValueSimple<Integer>, Integer> codebook = new CbFailing<AnnotatedValueSimple<Integer>, Integer>(item);
        String json = MAPPER.writeValueAsString(codebook);
        assertNotNull(json);
    }

    /*
    /**********************************************************
    /* Test methods, generic fields in subtypes [JACKSON-677], [JACKSON-887]
    /**********************************************************
     */

    // [JACKSON-677]
    @Test
    public void testGenericFieldInSubtype677() throws Exception
    {
        JavaType t677 = MAPPER.constructType(Result677.Success677.class);
        assertNotNull(t677);
        Result677.Success677<Integer> s = new Result677.Success677<Integer>(Integer.valueOf(4));
        String json = MAPPER.writeValueAsString(s);
        assertEquals("{\"value\":4}", json);
    }

    // [JACKSON-887]
    @Test
    public void testInnerTypeWithBounds() throws Exception
    {
        BaseType.SubType<?> r = MAPPER.readValue("{}", BaseType.SubType.class);
        assertNotNull(r);
    }

    /*
    /**********************************************************
    /* Test methods, local type resolution [databind#609]
    /**********************************************************
     */

    // [databind#609]
    @Test
    public void testLocalPartialType609() throws Exception {
        EntityContainer input = new EntityContainer();
        input.entity = new RuleForm(12);
        String json = MAPPER.writeValueAsString(input);

        EntityContainer output = MAPPER.readValue(json, EntityContainer.class);
        assertEquals(12, output.getEntity().value);
    }

    /*
    /**********************************************************
    /* Test methods, type aliases [databind#743]
    /**********************************************************
     */

    // [databind#743]
    @Test
    public void testAliasResolutionIssue743() throws Exception
    {
        String s3 = "{\"dataObj\" : [ \"one\", \"two\", \"three\" ] }";

        Child743.ChildData d = MAPPER.readValue(s3, Child743.ChildData.class);
        assertNotNull(d.dataObj);
        assertEquals(3, d.dataObj.size());
    }

    /*
    /**********************************************************
    /* Test methods, polymorphic collection [databind#936]
    /**********************************************************
     */

    // [databind#936]
    @Test
    public void testPolymorphicWithOverride() throws Exception
    {
        JavaType type = MAPPER.getTypeFactory().constructCollectionType(StringyList.class, String.class);

        StringyList<String> list = new StringyList<String>();
        list.add("value 1");
        list.add("value 2");

        String serialized = MAPPER.writeValueAsString(list);

        StringyList<String> deserialized = MAPPER.readValue(serialized, type);

        assertNotNull(deserialized);
    }
}
