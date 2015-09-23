package com.fasterxml.jackson.databind.type;

import java.util.*;

import com.fasterxml.jackson.databind.*;

// for [databind#938]
public class RecursiveType938Test extends BaseMapTest
{
    public static interface Ability<T> { }

    public static final class ImmutablePair<L, R> implements Map.Entry<L, R>, Ability<ImmutablePair<L, R>> {
        public final L key;
        public final R value;

        public ImmutablePair(final L key, final R value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public L getKey() {
            return key;
        }

        @Override
        public R getValue() {
            return value;
        }

        @Override
        public R setValue(final R value) {
            throw new UnsupportedOperationException();
        }
      
        static <L, R> ImmutablePair<L, R> of(final L left, final R right) {
            return new ImmutablePair<L, R>(left, right);
        }
    }

    private final ObjectMapper MAPPER = new ObjectMapper();

    public void testRecursivePair() throws Exception
    {
        JavaType t = MAPPER.constructType(ImmutablePair.class);

        assertNotNull(t);
        assertEquals(ImmutablePair.class, t.getRawClass());

        List<ImmutablePair<String, Double>> list = new ArrayList<ImmutablePair<String, Double>>();
        list.add(ImmutablePair.of("Hello World!", 123d));
        String json = MAPPER.writeValueAsString(list);

        assertNotNull(json);

        // can not deserialize with current definition, however
    }
}
