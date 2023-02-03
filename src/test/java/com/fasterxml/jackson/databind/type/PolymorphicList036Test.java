package com.fasterxml.jackson.databind.type;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.*;

// For [databind#936], losing parametric type information it seems
public class PolymorphicList036Test extends BaseMapTest
{
    // note: would prefer using CharSequence, but while abstract, that's deserialized
    // just fine as ... String
    static class StringyList<T extends java.io.Serializable> implements Collection<T> {
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

    private final ObjectMapper MAPPER = new ObjectMapper();

    public void testPolymorphicWithOverride() throws Exception
    {
        JavaType type = MAPPER.getTypeFactory().constructCollectionType(StringyList.class, String.class);

        StringyList<String> list = new StringyList<String>();
        list.add("value 1");
        list.add("value 2");

        String serialized = MAPPER.writeValueAsString(list);
//        System.out.println(serialized);

        StringyList<String> deserialized = MAPPER.readValue(serialized, type);
//        System.out.println(deserialized);

        assertNotNull(deserialized);
    }
}
