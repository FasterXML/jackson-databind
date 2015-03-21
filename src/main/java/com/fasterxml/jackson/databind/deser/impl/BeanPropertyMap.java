package com.fasterxml.jackson.databind.deser.impl;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.util.NameTransformer;

/**
 * Helper class used for storing mapping from property name to
 * {@link SettableBeanProperty} instances.
 *<p>
 * Note that this class is used instead of generic {@link java.util.HashMap}
 * for bit of performance gain (and some memory savings): although default
 * implementation is very good for generic use cases, it can be streamlined
 * a bit for specific use case we have. Even relatively small improvements
 * matter since this is directly on the critical path during deserialization,
 * as it is done for each and every POJO property deserialized.
 */
public abstract class BeanPropertyMap
    implements Iterable<SettableBeanProperty>, java.io.Serializable
{
    private static final long serialVersionUID = 2L;

    /**
     * @since 2.5
     */
    protected final boolean _caseInsensitive;

    protected BeanPropertyMap(boolean caseInsensitive)
    {
        _caseInsensitive = caseInsensitive;
    }

    /**
     * @since 2.6
     */
    public static BeanPropertyMap construct(Collection<SettableBeanProperty> props, boolean caseInsensitive) {
        if (props.isEmpty()) {
            return new Small(caseInsensitive);
        }
        Iterator<SettableBeanProperty> it = props.iterator();
        switch (props.size()) {
        case 1:
            return new Small(caseInsensitive, it.next());
        case 2:
            return new Small(caseInsensitive, it.next(), it.next());
        case 3:
            return new Small(caseInsensitive, it.next(), it.next(), it.next());
        }
        return new Default(props, caseInsensitive);
    }
    
    /**
     * Fluent copy method that creates a new instance that is a copy
     * of this instance except for one additional property that is
     * passed as the argument.
     * Note that method does not modify this instance but constructs
     * and returns a new one.
     * 
     * @since 2.0
     */
    public abstract BeanPropertyMap withProperty(SettableBeanProperty newProperty);

    public abstract BeanPropertyMap assignIndexes();

    /**
     * Factory method for constructing a map where all entries use given
     * prefix
     */
    public BeanPropertyMap renameAll(NameTransformer transformer)
    {
        if (transformer == null || (transformer == NameTransformer.NOP)) {
            return this;
        }
        Iterator<SettableBeanProperty> it = iterator();
        ArrayList<SettableBeanProperty> newProps = new ArrayList<SettableBeanProperty>();
        while (it.hasNext()) {
            SettableBeanProperty prop = it.next();
            String newName = transformer.transform(prop.getName());
            prop = prop.withSimpleName(newName);
            JsonDeserializer<?> deser = prop.getValueDeserializer();
            if (deser != null) {
                @SuppressWarnings("unchecked")
                JsonDeserializer<Object> newDeser = (JsonDeserializer<Object>)
                    deser.unwrappingDeserializer(transformer);
                if (newDeser != deser) {
                    prop = prop.withValueDeserializer(newDeser);
                }
            }
            newProps.add(prop);
        }
        // should we try to re-index? Ordering probably changed but called probably doesn't want changes...
        return construct(newProps, _caseInsensitive);
    }

    // Confining this case insensitivity to this function (and the find method) in case we want to
    // apply a particular locale to the lower case function.  For now, using the default.
    protected final String getPropertyName(SettableBeanProperty prop) {
        return _caseInsensitive ? prop.getName().toLowerCase() : prop.getName();
    }

    /*
    /**********************************************************
    /* Iterable, for convenient iterating over all properties
    /**********************************************************
     */

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Properties=[");
        int count = 0;
        for (SettableBeanProperty prop : getPropertiesInInsertionOrder()) {
            if (prop == null) {
                continue;
            }
            if (count++ > 0) {
                sb.append(", ");
            }
            sb.append(prop.getName());
            sb.append('(');
            sb.append(prop.getType());
            sb.append(')');
        }
        sb.append(']');
        return sb.toString();
    }

    /**
     * Accessor for traversing over all contained properties.
     */
    @Override
    public abstract Iterator<SettableBeanProperty> iterator();
    
    /**
     * Method that will re-create initial insertion-ordering of
     * properties contained in this map. Note that if properties
     * have been removed, array may contain nulls; otherwise
     * it should be consecutive.
     * 
     * @since 2.1
     */
    public abstract SettableBeanProperty[] getPropertiesInInsertionOrder();

    /*
    /**********************************************************
    /* Public API
    /**********************************************************
     */

    public abstract int size();

    public abstract SettableBeanProperty find(String key);

    /**
     * Convenience method that tries to find property with given name, and
     * if it is found, call {@link SettableBeanProperty#deserializeAndSet}
     * on it, and return true; or, if not found, return false.
     * Note, too, that if deserialization is attempted, possible exceptions
     * are wrapped if and as necessary, so caller need not handle those.
     * 
     * @since 2.5
     */
    public abstract boolean findDeserializeAndSet(JsonParser p, DeserializationContext ctxt,
            Object bean, String key) throws IOException;

    /**
     * @since 2.3
     */
    public abstract SettableBeanProperty find(int propertyIndex);

    /**
     * Specialized method that can be used to replace an existing entry
     * (note: entry MUST exist; otherwise exception is thrown) with
     * specified replacement.
     */
    public abstract void replace(SettableBeanProperty property);

    /**
     * Specialized method for removing specified existing entry.
     * NOTE: entry MUST exist, otherwise an exception is thrown.
     */
    public abstract void remove(SettableBeanProperty property);

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    /**
     * @since 2.5
     */
    protected void wrapAndThrow(Throwable t, Object bean, String fieldName, DeserializationContext ctxt)
        throws IOException
    {
        // inlined 'throwOrReturnThrowable'
        while (t instanceof InvocationTargetException && t.getCause() != null) {
            t = t.getCause();
        }
        // Errors to be passed as is
        if (t instanceof Error) {
            throw (Error) t;
        }
        // StackOverflowErrors are tricky ones; need to be careful...
        boolean wrap = (ctxt == null) || ctxt.isEnabled(DeserializationFeature.WRAP_EXCEPTIONS);
        // Ditto for IOExceptions; except we may want to wrap JSON exceptions
        if (t instanceof IOException) {
            if (!wrap || !(t instanceof JsonProcessingException)) {
                throw (IOException) t;
            }
        } else if (!wrap) { // [JACKSON-407] -- allow disabling wrapping for unchecked exceptions
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
        }
        throw JsonMappingException.wrapWithPath(t, bean, fieldName);
    }

    /*
    /**********************************************************
    /* Implementations
    /**********************************************************
     */

    /**
     * Note on implementation: can not make most fields final because of 'remove'
     * operation.
     */
    protected static class Small
        extends BeanPropertyMap
    {
        private static final long serialVersionUID = 1L;

        protected String key1, key2, key3;

        protected SettableBeanProperty prop1, prop2, prop3;

        protected int size;

        public Small(boolean caseInsensitive) {
            super(caseInsensitive);
            size = 0;
            key1 = key2 = key3 = null;
            prop1 = prop2 = prop3 = null;
        }

        public Small(boolean caseInsensitive, SettableBeanProperty p1) {
            super(caseInsensitive);
            size = 1;
            key1 = p1.getName();
            prop1 = p1;
            key2 = key3 = null;
            prop2 = prop3 = null;
        }

        public Small(boolean caseInsensitive, SettableBeanProperty p1, SettableBeanProperty p2) {
            super(caseInsensitive);
            size = 2;
            key1 = p1.getName();
            prop1 = p1;
            key2 = p2.getName();
            prop2 = p2;
            key3 = null;
            prop3 = null;
        }

        public Small(boolean caseInsensitive, SettableBeanProperty p1, SettableBeanProperty p2, SettableBeanProperty p3) {
            super(caseInsensitive);
            size = 2;
            key1 = p1.getName();
            prop1 = p1;
            key2 = p2.getName();
            prop2 = p2;
            key3 = p3.getName();
            prop3 = p3;
        }
        
        @Override
        public BeanPropertyMap withProperty(SettableBeanProperty newProperty) {
            // !!! TBI
            throw new UnsupportedOperationException();
        }

        @Override
        public BeanPropertyMap assignIndexes() {
            int ix = 0;
            if (prop1 != null) {
                prop1.assignIndex(ix++);
            }
            if (prop2 != null) {
                prop2.assignIndex(ix++);
            }
            if (prop3 != null) {
                prop3.assignIndex(ix++);
            }
            return this;
        }

        @Override
        public Iterator<SettableBeanProperty> iterator() {
            if (size == 0) {
                return Collections.<SettableBeanProperty>emptyList().iterator();
            }
            ArrayList<SettableBeanProperty> list = new ArrayList<SettableBeanProperty>();
            list.add(prop1);
            if (size > 1) {
                list.add(prop2);
                if (size > 2) {
                    list.add(prop3);
                }
            }
            return list.iterator();
        }

        @Override
        public SettableBeanProperty[] getPropertiesInInsertionOrder() {
            SettableBeanProperty[] props = new SettableBeanProperty[size];
            switch (size) {
            case 3:
                props[2] = prop3;
            case 2:
                props[1] = prop2;
            case 1:
                props[0] = prop1;
            }
            return props;
        }

        @Override
        public int size() {
            return size;
        }

        @Override
        public SettableBeanProperty find(String key) {
            if (key == key1) return prop1;
            if (key == key2) return prop2;
            if (key == key3) return prop3;
            return _findWithEquals(key);
        }

        private SettableBeanProperty _findWithEquals(String key) {
            if (key.equals(key1)) return prop1;
            if (key.equals(key2)) return prop2;
            if (key.equals(key3)) return prop3;
            return null;
        }
        
        @Override
        public boolean findDeserializeAndSet(JsonParser p,
                DeserializationContext ctxt, Object bean, String key) throws IOException {
            if (_caseInsensitive) {
                key = key.toLowerCase();
            }
            SettableBeanProperty prop = find(key);
            if (prop == null) {
                return false;
            }
            try {
                prop.deserializeAndSet(p, ctxt, bean);
            } catch (Exception e) {
                wrapAndThrow(e, bean, key, ctxt);
            }
            return true;

        }

        @Override
        public SettableBeanProperty find(int index) {
            switch (size) {
            case 3:
                if (prop3.getPropertyIndex() == index) return prop3;
            case 2:
                if (prop2.getPropertyIndex() == index) return prop2;
            case 1:
                if (prop1.getPropertyIndex() == index) return prop1;
            }
            return null;
        }

        @Override
        public void replace(SettableBeanProperty prop) {
            final String key = prop.getName();
            switch (size) {
            case 3:
                if (key.equals(key3)) {
                    prop3 = prop;
                    return;
                }
            case 2:
                if (key.equals(key2)) {
                    prop2 = prop;
                    return;
                }
            case 1:
                if (key.equals(key1)) {
                    prop1 = prop;
                    return;
                }
            }
            throw new NoSuchElementException("No entry '"+key+"' found, can't replace");
        }

        @Override
        public void remove(SettableBeanProperty property) {
            // !!! TBI
            throw new UnsupportedOperationException();
        }
    }

    protected static class Default extends BeanPropertyMap
    {
        private static final long serialVersionUID = 1L;

        private final static class Bucket implements java.io.Serializable
        {
            private static final long serialVersionUID = 1L;
    
            public final Bucket next;
            public final String key;
            public final SettableBeanProperty value;
    
            public final int index;
            
            public Bucket(Bucket next, String key, SettableBeanProperty value, int index)
            {
                this.next = next;
                this.key = key;
                this.value = value;
                this.index = index;
            }
        }

        private final static class IteratorImpl implements Iterator<SettableBeanProperty>
        {
            private final Bucket[] _buckets;

            private Bucket _currentBucket;

            private int _nextBucketIndex;
            
            public IteratorImpl(Bucket[] buckets) {
                _buckets = buckets;
                // need to initialize to point to first entry...
                int i = 0;
                for (int len = _buckets.length; i < len; ) {
                    Bucket b = _buckets[i++];
                    if (b != null) {
                        _currentBucket = b;
                        break;
                    }
                }
                _nextBucketIndex = i;
            }

            @Override
            public boolean hasNext() {
                return _currentBucket != null;
            }

            @Override
            public SettableBeanProperty next()
            {
                Bucket curr = _currentBucket;
                if (curr == null) { // sanity check
                    throw new NoSuchElementException();
                }
                // need to advance, too
                Bucket b = curr.next;
                while (b == null && _nextBucketIndex < _buckets.length) {
                    b = _buckets[_nextBucketIndex++];
                }
                _currentBucket = b;
                return curr.value;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        }
        
        private final Bucket[] _buckets;
        
        private final int _hashMask;

        private final int _size;

        /**
         * Counter we use to keep track of insertion order of properties
         * (to be able to recreate insertion order when needed).
         *<p>
         * Note: is kept up-to-date with additions, but can NOT handle
         * removals (i.e. "holes" may be left)
         */
        protected int _nextBucketIndex = 0;

        public Default(Collection<SettableBeanProperty> properties, boolean caseInsensitive)
        {
            super(caseInsensitive);
            _size = properties.size();
            int bucketCount = findSize(_size);
            _hashMask = bucketCount-1;
            Bucket[] buckets = new Bucket[bucketCount];
            for (SettableBeanProperty property : properties) {
                String key = getPropertyName(property);
                int index = key.hashCode() & _hashMask;
                buckets[index] = new Bucket(buckets[index], key, property, _nextBucketIndex++);
            }
            _buckets = buckets;
        }

        private Default(Bucket[] buckets, int size, int index, boolean caseInsensitive)
        {
            super(caseInsensitive);
            _buckets = buckets;
            _size = size;
            _hashMask = buckets.length-1;
            _nextBucketIndex = index;
        }

        private final static int findSize(int size)
        {
            // For small enough results (32 or less), we'll require <= 50% fill rate; otherwise 80%
            int needed = (size <= 32) ? (size + size) : (size + (size >> 2));
            int result = 2;
            while (result < needed) {
                result += result;
            }
            return result;
        }

        @Override
        public BeanPropertyMap withProperty(SettableBeanProperty newProperty)
        {
            // first things first: can just copy hash area:
            final int bcount = _buckets.length;
            Bucket[] newBuckets = new Bucket[bcount];
            System.arraycopy(_buckets, 0, newBuckets, 0, bcount);
            final String propName = getPropertyName(newProperty);
            // and then see if it's add or replace:
            SettableBeanProperty oldProp = find(propName);
            if (oldProp == null) { // add
                // first things first: add or replace?
                 // can do a straight copy, since all additions are at the front
                 // and then insert the new property:
                 int index = propName.hashCode() & _hashMask;
                 newBuckets[index] = new Bucket(newBuckets[index],
                         propName, newProperty, _nextBucketIndex++);
                 return new Default(newBuckets, _size+1, _nextBucketIndex, _caseInsensitive);
            }
            // replace: easy, close + replace
            BeanPropertyMap newMap = new Default(newBuckets, bcount, _nextBucketIndex, _caseInsensitive);
            newMap.replace(newProperty);
            return newMap;
        }

        @Override
        public BeanPropertyMap assignIndexes()
        {
            // order is arbitrary, but stable:
            int index = 0;
            for (Bucket bucket : _buckets) {
                while (bucket != null) {
                    bucket.value.assignIndex(index++);
                    bucket = bucket.next;
                }
            }
            return this;
        }
        
        @Override
        public int size() { return _size; }

        @Override
        public void remove(SettableBeanProperty property)
        {
            // Mostly this is the same as code with 'replace', just bit simpler...
            String name = getPropertyName(property);
            int index = name.hashCode() & (_buckets.length-1);
            Bucket tail = null;
            boolean found = false;
            // slightly complex just because chain is immutable, must recreate
            for (Bucket bucket = _buckets[index]; bucket != null; bucket = bucket.next) {
                // match to remove?
                if (!found && bucket.key.equals(name)) {
                    found = true;
                } else {
                    tail = new Bucket(tail, bucket.key, bucket.value, bucket.index);
                }
            }
            if (!found) { // must be found
                throw new NoSuchElementException("No entry '"+property+"' found, can't remove");
            }
            _buckets[index] = tail;
        }

        @Override
        public void replace(SettableBeanProperty property)
        {
            String name = getPropertyName(property);
            int index = name.hashCode() & (_buckets.length-1);

            /* This is bit tricky just because buckets themselves
             * are immutable, so we need to recreate the chain. Fine.
             */
            Bucket tail = null;
            int foundIndex = -1;
            
            for (Bucket bucket = _buckets[index]; bucket != null; bucket = bucket.next) {
                // match to remove?
                if (foundIndex < 0 && bucket.key.equals(name)) {
                    foundIndex = bucket.index;
                } else {
                    tail = new Bucket(tail, bucket.key, bucket.value, bucket.index);
                }
            }
            // Not finding specified entry is error, so:
            if (foundIndex < 0) {
                throw new NoSuchElementException("No entry '"+property+"' found, can't replace");
            }
            /* So let's attach replacement in front: useful also because
             * it allows replacement even when iterating over entries
             */
            _buckets[index] = new Bucket(tail, name, property, foundIndex);
        }
        @Override
        public Iterator<SettableBeanProperty> iterator() {
            return new IteratorImpl(_buckets);
        }
        
        @Override
        public SettableBeanProperty[] getPropertiesInInsertionOrder()
        {
            int len = _nextBucketIndex;
            SettableBeanProperty[] result = new SettableBeanProperty[len];
            for (Bucket root : _buckets) {
                for (Bucket bucket = root; bucket != null; bucket = bucket.next) {
                    result[bucket.index] = bucket.value;
                }
            }
            return result;
        }

        @Override
        public SettableBeanProperty find(int propertyIndex)
        {
            for (int i = 0, end = _buckets.length; i < end; ++i) {
                for (Bucket bucket = _buckets[i]; bucket != null; bucket = bucket.next) {
                    if (bucket.index == propertyIndex) {
                        return bucket.value;
                    }
                }
            }
            return null;
        }

        @Override
        public SettableBeanProperty find(String key)
        {
            if (key == null) {
                throw new IllegalArgumentException("Can not pass null property name");
            }
            if (_caseInsensitive) {
                key = key.toLowerCase();
            }
            int index = key.hashCode() & _hashMask;
            Bucket bucket = _buckets[index];
            // Let's unroll first lookup since that is null or match in 90+% cases
            if (bucket == null) {
                return null;
            }
            // Primarily we do just identity comparison as keys should be interned
            if (bucket.key == key) {
                return bucket.value;
            }
            while ((bucket = bucket.next) != null) {
                if (bucket.key == key) {
                    return bucket.value;
                }
            }
            // Do we need fallback for non-interned Strings?
            return _findWithEquals(key, index);
        }

        @Override
        public boolean findDeserializeAndSet(JsonParser p, DeserializationContext ctxt,
                Object bean, String key) throws IOException
        {
            if (_caseInsensitive) {
                key = key.toLowerCase();
            }
            int index = key.hashCode() & _hashMask;
            Bucket bucket = _buckets[index];
            // Let's unroll first lookup since that is null or match in 90+% cases
            if (bucket == null) {
                return false;
            }
            // Primarily we do just identity comparison as keys should be interned
            if (bucket.key == key) {
                try {
                    bucket.value.deserializeAndSet(p, ctxt, bean);
                } catch (Exception e) {
                    wrapAndThrow(e, bean, key, ctxt);
                }
                return true;
            } 
            return _findDeserializeAndSet2(p, ctxt, bean, key, index);
        }

        private final boolean _findDeserializeAndSet2(JsonParser p, DeserializationContext ctxt,
                Object bean, String key, int index) throws IOException
        {
            SettableBeanProperty prop = null;
            Bucket bucket = _buckets[index];
            while (true) {
                if ((bucket = bucket.next) == null) {
                    prop = _findWithEquals(key, index);
                    if (prop == null) {
                        return false;
                    }
                    break;
                }
                if (bucket.key == key) {
                    prop = bucket.value;
                    break;
                }
            }
            try {
                prop.deserializeAndSet(p, ctxt, bean);
            } catch (Exception e) {
                wrapAndThrow(e, bean, key, ctxt);
            }
            return true;
        }
        
        protected SettableBeanProperty _findWithEquals(String key, int index)
        {
            Bucket bucket = _buckets[index];
            while (bucket != null) {
                if (key.equals(bucket.key)) {
                    return bucket.value;
                }
                bucket = bucket.next;
            }
            return null;
        }
    }
}
