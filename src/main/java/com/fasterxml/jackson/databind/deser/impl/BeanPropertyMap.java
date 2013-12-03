package com.fasterxml.jackson.databind.deser.impl;

import java.util.*;

import com.fasterxml.jackson.databind.JsonDeserializer;
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
public final class BeanPropertyMap
    implements Iterable<SettableBeanProperty>,
        java.io.Serializable // since 2.1
{
    private static final long serialVersionUID = 1L;

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
    private int _nextBucketIndex = 0;

    public BeanPropertyMap(Collection<SettableBeanProperty> properties)
    {
        _size = properties.size();
        int bucketCount = findSize(_size);
        _hashMask = bucketCount-1;
        Bucket[] buckets = new Bucket[bucketCount];
        for (SettableBeanProperty property : properties) {
            String key = property.getName();
            int index = key.hashCode() & _hashMask;
            buckets[index] = new Bucket(buckets[index], key, property, _nextBucketIndex++);
        }
        _buckets = buckets;
    }

    private BeanPropertyMap(Bucket[] buckets, int size, int index)
    {
        _buckets = buckets;
        _size = size;
        _hashMask = buckets.length-1;
        _nextBucketIndex = index;
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
    public BeanPropertyMap withProperty(SettableBeanProperty newProperty)
    {
        // first things first: can just copy hash area:
        final int bcount = _buckets.length;
        Bucket[] newBuckets = new Bucket[bcount];
        System.arraycopy(_buckets, 0, newBuckets, 0, bcount);
        final String propName = newProperty.getName();
        // and then see if it's add or replace:
        SettableBeanProperty oldProp = find(newProperty.getName());
        if (oldProp == null) { // add
            // first things first: add or replace?
    	        // can do a straight copy, since all additions are at the front
    	        // and then insert the new property:
    	        int index = propName.hashCode() & _hashMask;
    	        newBuckets[index] = new Bucket(newBuckets[index],
    	                propName, newProperty, _nextBucketIndex++);
    	        return new BeanPropertyMap(newBuckets, _size+1, _nextBucketIndex);
        }
        // replace: easy, close + replace
        BeanPropertyMap newMap = new BeanPropertyMap(newBuckets, bcount, _nextBucketIndex);
        newMap.replace(newProperty);
        return newMap;
    }

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
        return new BeanPropertyMap(newProps);
    }
    
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
    public Iterator<SettableBeanProperty> iterator() {
        return new IteratorImpl(_buckets);
    }
    
    /**
     * Method that will re-create initial insertion-ordering of
     * properties contained in this map. Note that if properties
     * have been removed, array may contain nulls; otherwise
     * it should be consecutive.
     * 
     * @since 2.1
     */
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

    /*
    /**********************************************************
    /* Public API
    /**********************************************************
     */

    public int size() { return _size; }

    public SettableBeanProperty find(String key)
    {
        if (key == null) {
            throw new IllegalArgumentException("Can not pass null property name");
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

    /**
     * @since 2.3
     */
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
    
    /**
     * Specialized method that can be used to replace an existing entry
     * (note: entry MUST exist; otherwise exception is thrown) with
     * specified replacement.
     */
    public void replace(SettableBeanProperty property)
    {
        String name = property.getName();
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

    /**
     * Specialized method for removing specified existing entry.
     * NOTE: entry MUST exist, otherwise an exception is thrown.
     */
    public void remove(SettableBeanProperty property)
    {
        // Mostly this is the same as code with 'replace', just bit simpler...
        String name = property.getName();
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
    
    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */
    
    private SettableBeanProperty _findWithEquals(String key, int index)
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

    /*
    /**********************************************************
    /* Helper beans
    /**********************************************************
     */
    
    private final static class Bucket
        implements java.io.Serializable
    {
        private static final long serialVersionUID = 1L;

        public final Bucket next;
        public final String key;
        public final SettableBeanProperty value;

        /**
         * Index that indicates insertion order of the bucket
         */
        public final int index;
        
        public Bucket(Bucket next, String key, SettableBeanProperty value, int index)
        {
            this.next = next;
            this.key = key;
            this.value = value;
            this.index = index;
        }
    }

    private final static class IteratorImpl
        implements Iterator<SettableBeanProperty>
    {
        /**
         * Buckets of the map
         */
        private final Bucket[] _buckets;

        /**
         * Bucket that contains next value to return (if any); null if nothing more to iterate
         */
        private Bucket _currentBucket;

        /**
         * Index of the next bucket in bucket array to check.
         */
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
}
