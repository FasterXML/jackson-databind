package tools.jackson.databind.ser;

import java.util.*;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonInclude;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.*;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.StdScalarSerializer;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [databind#6449]: container serializers must reuse dynamically resolved
// element/value serializers (resolved once per runtime type), for untyped,
// typed and filtered (content inclusion) cases alike
public class DynamicSerializerCaching6449Test extends DatabindTestUtil
{
    static class A { }
    static class B { }

    static List<Object> elems() {
        return List.of(new A(), new B(), new A(), new B());
    }

    // ArrayList: IndexedListSerializer
    static class ListHolder {
        public List<Object> values = new ArrayList<>(elems());
    }

    // non-List Collection: CollectionSerializer
    static class CollectionHolder {
        public Collection<Object> values = new LinkedHashSet<>(elems());
    }

    static class ArrayHolder {
        public Object[] values = elems().toArray();
    }

    static class Elems implements Iterable<Object> {
        @Override
        public Iterator<Object> iterator() { return elems().iterator(); }
    }

    static class IterableHolder {
        public Iterable<Object> values = new Elems();
    }

    static class IteratorHolder {
        public Iterator<Object> values = elems().iterator();
    }

    static class MapHolder {
        public Map<String, Object> values = new LinkedHashMap<>();
        {
            List<Object> elems = elems();
            for (int i = 0; i < elems.size(); ++i) {
                values.put("k" + i, elems.get(i));
            }
        }
    }

    static class MapEntryHolder {
        public List<Map.Entry<String, Object>> values = new ArrayList<>();
        {
            for (Object elem : elems()) {
                values.add(new AbstractMap.SimpleEntry<>("k", elem));
            }
        }
    }

    static class Counting<T> extends StdScalarSerializer<T> {
        final Map<String, Integer> counts;
        final Class<T> type;

        Counting(Class<T> type, Map<String, Integer> counts) {
            super(type);
            this.type = type;
            this.counts = counts;
        }

        @Override
        public ValueSerializer<?> createContextual(SerializationContext ctxt, BeanProperty p) {
            counts.merge(type.getSimpleName(), 1, Integer::sum);
            return this;
        }

        @Override
        public boolean isEmpty(SerializationContext ctxt, T value) {
            return false;
        }

        @Override
        public void serialize(T v, JsonGenerator g, SerializationContext ctxt) {
            g.writeString(type.getSimpleName());
        }
    }

    private static final List<Supplier<Object>> HOLDERS = List.of(
            ListHolder::new, CollectionHolder::new, ArrayHolder::new,
            IterableHolder::new, IteratorHolder::new, MapHolder::new,
            MapEntryHolder::new);

    @Test
    public void untypedResolvedOncePerType() throws Exception
    {
        _verifyResolvedOnce(b -> b);
    }

    @Test
    public void typedResolvedOncePerType() throws Exception
    {
        _verifyResolvedOnce(b -> b
                .activateDefaultTyping(BasicPolymorphicTypeValidator.builder()
                        .allowIfBaseType(Object.class)
                        .build(),
                        DefaultTyping.JAVA_LANG_OBJECT));
    }

    // Content inclusion `NON_EMPTY` makes empty checks use element/value
    // serializers too (before actual serialization)
    @Test
    public void filteredResolvedOncePerType() throws Exception
    {
        _verifyResolvedOnce(b -> b
                .changeDefaultPropertyInclusion(incl -> JsonInclude.Value.construct(
                        JsonInclude.Include.NON_EMPTY, JsonInclude.Include.NON_EMPTY)));
    }

    private void _verifyResolvedOnce(UnaryOperator<JsonMapper.Builder> config) throws Exception
    {
        // Collect counts for all holders first, to report all failures at once
        Map<String, Map<String, Integer>> expected = new TreeMap<>();
        Map<String, Map<String, Integer>> actual = new TreeMap<>();
        for (Supplier<Object> holder : HOLDERS) {
            Map<String, Integer> counts = new TreeMap<>();
            ObjectMapper mapper = config.apply(JsonMapper.builder())
                    .addModule(new SimpleModule()
                            .addSerializer(A.class, new Counting<>(A.class, counts))
                            .addSerializer(B.class, new Counting<>(B.class, counts)))
                    .build();
            List<Object> values = new ArrayList<>();
            for (int i = 0; i < 3; ++i) {
                values.add(holder.get());
            }
            mapper.writeValueAsString(values);
            String desc = holder.get().getClass().getSimpleName();
            expected.put(desc, Map.of("A", 1, "B", 1));
            actual.put(desc, counts);
        }
        assertEquals(expected, actual);
    }
}
