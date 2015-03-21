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
    implements Iterable<SettableBeanProperty>,
        java.io.Serializable
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
        return new Default(caseInsensitive, props);
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

        Iterator<SettableBeanProperty> it = iterator();
        while (it.hasNext()) {
            SettableBeanProperty prop = it.next();
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
    public void replace(SettableBeanProperty property) {
        throw new NoSuchElementException("No entry '"+property.getName()+"' found, can't replace");
    }

    /**
     * Specialized method for removing specified existing entry.
     * NOTE: entry MUST exist, otherwise an exception is thrown.
     */
    public void remove(SettableBeanProperty property) {
        throw new NoSuchElementException("No entry '"+property.getName()+"' found, can't remove");
    }

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
            size = 3;
            key1 = p1.getName();
            prop1 = p1;
            key2 = p2.getName();
            prop2 = p2;
            key3 = p3.getName();
            prop3 = p3;
        }

        @Override
        public BeanPropertyMap withProperty(SettableBeanProperty prop)
        {
            final String key = prop.getName();
            // First: replace existing one?
            switch (size) {
            case 3:
                if (key.equals(key3)) {
                    prop3 = prop;
                    return this;
                }
            case 2:
                if (key.equals(key2)) {
                    prop2 = prop;
                    return this;
                }
            case 1:
                if (key.equals(key1)) {
                    prop1 = prop;
                    return this;
                }
            }

            // If not, append. Easy if we aren't yet full
            switch (size) {
            case 2:
                return new Small(_caseInsensitive, prop1, prop2, prop);
            case 1:
                return new Small(_caseInsensitive, prop1, prop);
            case 0:
                return new Small(_caseInsensitive, prop);
            }
            // But if we have all 3, "upgrade"
            prop.assignIndex(3);
            List<SettableBeanProperty> props = new ArrayList<SettableBeanProperty>(4);
            props.add(prop1);
            props.add(prop2);
            props.add(prop3);
            props.add(prop);
            return new Default(_caseInsensitive, props);
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
            if (size == 1) {
                return Collections.singleton(prop1).iterator();
            }
            ArrayList<SettableBeanProperty> list = new ArrayList<SettableBeanProperty>();
            list.add(prop1);
            list.add(prop2);
            if (size > 2) {
                list.add(prop3);
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
            if (prop != null) {
                try {
                    prop.deserializeAndSet(p, ctxt, bean);
                } catch (Exception e) {
                    wrapAndThrow(e, bean, key, ctxt);
                }
                return true;
            }
            return false;
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
            super.replace(prop);
        }

        @Override
        public void remove(SettableBeanProperty prop) {
            final String key = prop.getName();
            switch (size) {
            case 3:
                if (key.equals(key3)) {
                    prop3 = null;
                    key3 = null;
                    size = 2;
                    return;
                }
            case 2:
                if (key.equals(key2)) {
                    prop2 = prop3;
                    key2 = key3;
                    --size;
                    return;
                }
            case 1:
                if (key.equals(key1)) {
                    prop1 = prop2;
                    key1 = key2;
                    prop2 = prop3;
                    key2 = key3;
                    --size;
                    return;
                }
            }
            super.remove(prop);
        }
    }

    /**
     * For non-trivial number of properties let's use a hash map.
     * Alas, can not use {@link com.fasterxml.jackson.databind.util.CompactStringObjectMap}
     * as is (need to add index), but structure is similar.
     */
    protected final static class Default extends BeanPropertyMap
    {
        private static final long serialVersionUID = 1L;

        private int _hashMask, _size, _spillCount;

        private String[] _keys;

        /**
         * Hash area for properties, sparsely populated. Starts with the primary
         * hash (size of {@link #_size}), followed by secondary (with half the size
         * of primary), and with optional variable size spillover.
         */
        private SettableBeanProperty[] _propsHash;

        /**
         * Array of properties in the exact order they were handed in. This is
         * used by as-array serialization, deserialization.
         */
        private SettableBeanProperty[] _propsInOrder;
        
        /**
         * Counter we use to keep track of insertion order of properties
         * (to be able to recreate insertion order when needed).
         *<p>
         * Note: is kept up-to-date with additions, but can NOT handle
         * removals (i.e. "holes" may be left)
         */
        protected int _nextBucketIndex = 0;

        public Default(boolean caseInsensitive, Collection<SettableBeanProperty> props)
        {
            super(caseInsensitive);
            _propsInOrder = props.toArray(new SettableBeanProperty[props.size()]);
            init(props);
        }
        
        protected void init(Collection<SettableBeanProperty> props)
        {
            _size = props.size();
            
            // First: calculate size of primary hash area
            final int size = findSize(_size);
            _hashMask = size-1;

            // and allocate enough to contain primary/secondary, expand for spillovers as need be
            int alloc = size + (size>>1);
            String[] keys = new String[alloc];
            SettableBeanProperty[] fieldHash = new SettableBeanProperty[alloc];
            int spills = 0;

            for (SettableBeanProperty prop : props) {
                String key = getPropertyName(prop);
                int slot = key.hashCode() & _hashMask;

                // primary slot not free?
                if (keys[slot] != null) {
                    // secondary?
                    slot = size + (slot >> 1);
                    if (keys[slot] != null) {
                        // ok, spill over.
                        slot = size + (size >> 1) + spills;
                        ++spills;
                        if (slot >= keys.length) {
                            keys = Arrays.copyOf(keys, keys.length + 4);
                            fieldHash = Arrays.copyOf(fieldHash, fieldHash.length + 4);
                        }
                    }
                }
                keys[slot] = key;
                fieldHash[slot] = prop;
            }
            _keys = keys;
            _propsHash = fieldHash;
            _spillCount = spills;
        }

        private final static int findSize(int size)
        {
            if (size <= 5) {
                return 8;
            }
            if (size <= 12) {
                return 16;
            }
            int needed = size + (size >> 2); // at most 80% full
            int result = 32;
            while (result < needed) {
                result += result;
            }
            return result;
        }

        @Override
        public BeanPropertyMap withProperty(SettableBeanProperty newProp)
        {
            // First: may be able to just replace?
            String key = getPropertyName(newProp);
            for (int i = 0, end = _propsHash.length; i < end; ++i) {
                SettableBeanProperty prop = _propsHash[i];
                if ((prop != null) && prop.getName().equals(key)) {
                    _propsHash[i] = newProp;
                    return this;
                }
            }
            // If not, re-create
            List<SettableBeanProperty> props = properties();
            props.add(newProp);
            init(props);
            ++_nextBucketIndex;

            // should we just create a new one? Or is resetting ok?
            
            return this;
        }

        @Override
        public BeanPropertyMap assignIndexes()
        {
            // order is arbitrary, but stable:
            int index = 0;
            for (SettableBeanProperty prop : _propsHash) {
                if (prop != null) {
                    prop.assignIndex(index++);
                }
            }
            _nextBucketIndex = index;
            return this;
        }
        
        @Override
        public int size() { return _size; }

        @Override
        public void remove(SettableBeanProperty propToRm)
        {
            ArrayList<SettableBeanProperty> props = new ArrayList<SettableBeanProperty>(_size);
            String key = getPropertyName(propToRm);
            boolean found = false;
            for (SettableBeanProperty prop : _propsHash) {
                if (prop == null) {
                    continue;
                }
                if (!found) {
                    found = key.equals(prop.getName());
                    if (found) {
                        // need to leave a hole here
                        _propsInOrder[_findFromOrdered(prop)] = null;
                        continue;
                    }
                }
                props.add(prop);
            }
            if (found) {
                init(props);
                return;
            }
            super.remove(propToRm);
        }

        @Override
        public void replace(SettableBeanProperty newProp)
        {
            String key = getPropertyName(newProp);
            for (int i = 0, end = _propsHash.length; i < end; ++i) {
                SettableBeanProperty prop = _propsHash[i];
                if ((prop != null) && prop.getName().equals(key)) {
                    _propsHash[i] = newProp;
                    // also, replace in in-order
                    _propsInOrder[_findFromOrdered(prop)] = newProp;
                    return;
                }
            }
            super.replace(newProp);
        }

        private List<SettableBeanProperty> properties() {
            ArrayList<SettableBeanProperty> p = new ArrayList<SettableBeanProperty>(_nextBucketIndex);
            for (SettableBeanProperty prop : _propsHash) {
                if (prop != null) {
                    p.add(prop);
                }
            }
            return p;
        }
        
        @Override
        public Iterator<SettableBeanProperty> iterator() {
            return properties().iterator();
        }
        
        @Override
        public SettableBeanProperty[] getPropertiesInInsertionOrder()
        {
            return _propsInOrder;
        }

        @Override
        public SettableBeanProperty find(int index)
        {
            for (SettableBeanProperty prop : _propsHash) {
                if ((prop != null) && (index == prop.getPropertyIndex())) {
                    return prop;
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
            int slot = key.hashCode() & _hashMask;
            String match = _keys[slot];
            if ((match == key) || key.equals(match)) {
                return _propsHash[slot];
            }
            if (match == null) {
                return null;
            }
            // no? secondary?
            slot = (_hashMask+1) + (slot>>1);
            match = _keys[slot];
            if (key.equals(match)) {
                return _propsHash[slot];
            }
            // or spill?
            return _findFromSpill(key);
        }

        @Override
        public boolean findDeserializeAndSet(JsonParser p, DeserializationContext ctxt,
                Object bean, String key) throws IOException
        {
            final SettableBeanProperty prop = find(key);
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

        private SettableBeanProperty _findFromSpill(String key) {
            int hashSize = _hashMask+1;
            int i = hashSize + (hashSize>>1);
            for (int end = i + _spillCount; i < end; ++i) {
                if (key.equals(_keys[i])) {
                    return _propsHash[1];
                }
            }
            return null;
        }

        private int _findFromOrdered(SettableBeanProperty prop) {
            for (int i = 0, end = _propsInOrder.length; i < end; ++i) {
                if (_propsInOrder[i] == prop) {
                    return i;
                }
            }
            throw new IllegalStateException("Illegal state: property '"+prop.getName()+"' missing from _propsInOrder");
        }
    }
}
