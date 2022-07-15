package tools.jackson.databind.ser.impl;

import java.util.Arrays;

import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.SerializerProvider;
import tools.jackson.databind.ValueSerializer;

/**
 * Helper container used for resolving serializers for dynamic (possibly but not
 * necessarily polymorphic) properties: properties whose type is not forced
 * to use dynamic (declared) type and that are not final.
 * If so, serializer to use can only be established once actual value type is known.
 * Since this happens a lot unless static typing is forced (or types are final)
 * this implementation is optimized for efficiency.
 * Instances are immutable; new instances are created with factory methods: this
 * is important to ensure correct multi-threaded access.
 */
public abstract class PropertySerializerMap
    implements java.io.Serializable
{
    private static final long serialVersionUID = 3L;

    /**
     * Configuration setting that determines what happens when maximum
     * size (currently 8) is reached: if true, will "start from beginning";
     * if false, will simply stop adding new entries.
     */
    protected final boolean _resetWhenFull;

    protected PropertySerializerMap(boolean resetWhenFull) {
        _resetWhenFull = resetWhenFull;
    }

    protected PropertySerializerMap(PropertySerializerMap base) {
        _resetWhenFull = base._resetWhenFull;
    }

    /**
     * Main lookup method. Takes a "raw" type since usage is always from
     * place where parameterization is fixed such that there cannot be
     * type-parametric variations.
     */
    public abstract ValueSerializer<Object> serializerFor(Class<?> type);

    /**
     * Method called if initial lookup fails, when looking for a primary
     * serializer (one that is directly attached to a property).
     * Will both find serializer
     * and construct new map instance if warranted, and return both.
     */
    public final SerializerAndMapResult findAndAddPrimarySerializer(JavaType type,
            SerializerProvider provider, BeanProperty property)
    {
        ValueSerializer<Object> serializer = provider.findPrimaryPropertySerializer(type, property);
        return new SerializerAndMapResult(serializer, newWith(type.getRawClass(), serializer));
    }

    /**
     * Method called if initial lookup fails, when looking for a non-primary
     * serializer (one that is not directly attached to a property).
     * Will both find serializer
     * and construct new map instance if warranted, and return both.
     */
    public final SerializerAndMapResult findAndAddSecondarySerializer(Class<?> type,
            SerializerProvider provider, BeanProperty property)
    {
        ValueSerializer<Object> serializer = provider.findContentValueSerializer(type, property);
        return new SerializerAndMapResult(serializer, newWith(type, serializer));
    }

    public final SerializerAndMapResult findAndAddSecondarySerializer(JavaType type,
            SerializerProvider provider, BeanProperty property)
    {
        ValueSerializer<Object> serializer = provider.findContentValueSerializer(type, property);
        return new SerializerAndMapResult(serializer, newWith(type.getRawClass(), serializer));
    }

    /**
     * Method called if initial lookup fails, when looking for a root value
     * serializer: one that is not directly attached to a property, but needs to
     * have {@link tools.jackson.databind.jsontype.TypeSerializer} wrapped
     * around it. Will both find the serializer
     * and construct new map instance if warranted, and return both.
     */
    public final SerializerAndMapResult findAndAddRootValueSerializer(Class<?> type,
            SerializerProvider provider)
    {
        ValueSerializer<Object> serializer = provider.findTypedValueSerializer(type, false);
        return new SerializerAndMapResult(serializer, newWith(type, serializer));
    }

    public final SerializerAndMapResult findAndAddRootValueSerializer(JavaType type,
            SerializerProvider provider)
    {
        ValueSerializer<Object> serializer = provider.findTypedValueSerializer(type, false);
        return new SerializerAndMapResult(serializer, newWith(type.getRawClass(), serializer));
    }

    /**
     * Method called if initial lookup fails, when looking for a key
     * serializer (possible attached indirectly to a property)
     * Will both find serializer
     * and construct new map instance if warranted, and return both.
     */
    public final SerializerAndMapResult findAndAddKeySerializer(Class<?> type,
            SerializerProvider provider, BeanProperty property)
    {
        ValueSerializer<Object> serializer = provider.findKeySerializer(type, property);
        return new SerializerAndMapResult(serializer, newWith(type, serializer));
    }
    
    /**
     * Method that can be used to 'register' a serializer that caller has resolved
     * without help of this map.
     */
    public final SerializerAndMapResult addSerializer(Class<?> type, ValueSerializer<Object> serializer) {
        return new SerializerAndMapResult(serializer, newWith(type, serializer));
    }

    public final SerializerAndMapResult addSerializer(JavaType type, ValueSerializer<Object> serializer) {
        return new SerializerAndMapResult(serializer, newWith(type.getRawClass(), serializer));
    }

    public abstract PropertySerializerMap newWith(Class<?> type, ValueSerializer<Object> serializer);

    public static PropertySerializerMap emptyForProperties() {
        return Empty.FOR_PROPERTIES;
    }

    public static PropertySerializerMap emptyForRootValues() {
        return Empty.FOR_ROOT_VALUES;
    }

    /*
    /**********************************************************************
    /* Helper classes
    /**********************************************************************
     */

    /**
     * Value class used for returning tuple that has both serializer
     * that was retrieved and new map instance
     */
    public final static class SerializerAndMapResult
    {
        public final ValueSerializer<Object> serializer;
        public final PropertySerializerMap map;
        
        public SerializerAndMapResult(ValueSerializer<Object> serializer,
                PropertySerializerMap map)
        {
            this.serializer = serializer;
            this.map = map;
        }
    }

    /**
     * Trivial container for bundling type + serializer entries.
     */
    private final static class TypeAndSerializer
    {
        public final Class<?> type;
        public final ValueSerializer<Object> serializer;

        public TypeAndSerializer(Class<?> type, ValueSerializer<Object> serializer) {
            this.type = type;
            this.serializer = serializer;
        }
    }

    /*
    /**********************************************************************
    /* Implementations
    /**********************************************************************
     */

    /**
     * Bogus instance that contains no serializers; used as the default
     * map with new serializers.
     */
    private final static class Empty extends PropertySerializerMap
        implements java.io.Serializable // since 3.0
    {
        private static final long serialVersionUID = 3L;

        // No root serializers; do not reset when full
        public final static Empty FOR_PROPERTIES = new Empty(false);

        // Yes, root serializers; do reset when full
        public final static Empty FOR_ROOT_VALUES = new Empty(true);

        protected Empty(boolean resetWhenFull) {
            super(resetWhenFull);
        }

        // @since 3.0
        public static Empty emptyFor(PropertySerializerMap src) {
            return (src._resetWhenFull) ? FOR_ROOT_VALUES : FOR_PROPERTIES;
        }
        
        Object readResolve() { // for JDK serialization (since 3.0)
            return emptyFor(this);
        }

        @Override
        public ValueSerializer<Object> serializerFor(Class<?> type) {
            return null; // empty, nothing to find
        }        

        @Override
        public PropertySerializerMap newWith(Class<?> type, ValueSerializer<Object> serializer) {
            return new Single(this, type, serializer);
        }
    }

    /**
     * Map that contains a single serializer; although seemingly silly
     * this is probably the most commonly used variant because many
     * theoretically dynamic or polymorphic types just have single
     * actual type.
     */
    private final static class Single extends PropertySerializerMap
        implements java.io.Serializable // since 3.0
    {
        private static final long serialVersionUID = 3L;

        private final Class<?> _type;
        private final ValueSerializer<Object> _serializer;

        public Single(PropertySerializerMap base, Class<?> type, ValueSerializer<Object> serializer) {
            super(base);
            _type = type;
            _serializer = serializer;
        }

        Object writeReplace() { // for JDK serialization (since 3.0)
            return Empty.emptyFor(this);
        }

        @Override
        public ValueSerializer<Object> serializerFor(Class<?> type)
        {
            if (type == _type) {
                return _serializer;
            }
            return null;
        }

        @Override
        public PropertySerializerMap newWith(Class<?> type, ValueSerializer<Object> serializer) {
            return new Double(this, _type, _serializer, type, serializer);
        }
    }

    private final static class Double extends PropertySerializerMap
        implements java.io.Serializable // since 3.0
    {
        private static final long serialVersionUID = 3L;

        private final Class<?> _type1, _type2;
        private final ValueSerializer<Object> _serializer1, _serializer2;

        public Double(PropertySerializerMap base,
                Class<?> type1, ValueSerializer<Object> serializer1,
                Class<?> type2, ValueSerializer<Object> serializer2)
        {
            super(base);
            _type1 = type1;
            _serializer1 = serializer1;
            _type2 = type2;
            _serializer2 = serializer2;
        }

        Object writeReplace() { // for JDK serialization (since 3.0)
            return Empty.emptyFor(this);
        }

        @Override
        public ValueSerializer<Object> serializerFor(Class<?> type)
        {
            if (type == _type1) {
                return _serializer1;
            }
            if (type == _type2) {
                return _serializer2;
            }
            return null;
        }        

        @Override
        public PropertySerializerMap newWith(Class<?> type, ValueSerializer<Object> serializer) {
            // Ok: let's just create generic one
            TypeAndSerializer[] ts = new TypeAndSerializer[3];
            ts[0] = new TypeAndSerializer(_type1, _serializer1);
            ts[1] = new TypeAndSerializer(_type2, _serializer2);
            ts[2] = new TypeAndSerializer(type, serializer);
            return new Multi(this, ts);
        }
    }
    
    private final static class Multi extends PropertySerializerMap
        implements java.io.Serializable // since 3.0
    {
        private static final long serialVersionUID = 3L;

        /**
         * Let's limit number of serializers we actually cache; linear
         * lookup won't scale too well beyond smallish number, and if
         * we really want to support larger collections should use
         * a hash map. But it seems unlikely this is a common use
         * case so for now let's just stop building after hard-coded
         * limit. 8 sounds like a reasonable stab for now.
         */
        private final static int MAX_ENTRIES = 8;
        
        private final TypeAndSerializer[] _entries;

        public Multi(PropertySerializerMap base, TypeAndSerializer[] entries) {
            super(base);
            _entries = entries;
        }

        Object writeReplace() { // for JDK serialization (since 3.0)
            return Empty.emptyFor(this);
        }

        @Override
        public ValueSerializer<Object> serializerFor(Class<?> type)
        {
            // Always have first 3 populated so
            TypeAndSerializer entry;
            entry = _entries[0];
            if (entry.type == type) return entry.serializer;
            entry = _entries[1];
            if (entry.type == type) return entry.serializer;
            entry = _entries[2];
            if (entry.type == type) return entry.serializer;

            switch (_entries.length) {
            case 8:
                entry = _entries[7];
                if (entry.type == type) return entry.serializer;
            case 7:
                entry = _entries[6];
                if (entry.type == type) return entry.serializer;
            case 6:
                entry = _entries[5];
                if (entry.type == type) return entry.serializer;
            case 5:
                entry = _entries[4];
                if (entry.type == type) return entry.serializer;
            case 4:
                entry = _entries[3];
                if (entry.type == type) return entry.serializer;
            default:
            }
            return null;
        }

        @Override
        public PropertySerializerMap newWith(Class<?> type, ValueSerializer<Object> serializer)
        {
            int len = _entries.length;
            // Will only grow up to N entries. We could consider couple of alternatives after
            // this if we wanted to... but for now, two main choices make most sense
            if (len == MAX_ENTRIES) {
                if (_resetWhenFull) {
                    return new Single(this, type, serializer);
                }
                return this;
            }
            TypeAndSerializer[] entries = Arrays.copyOf(_entries, len+1);
            entries[len] = new TypeAndSerializer(type, serializer);
            return new Multi(this, entries);
        }
    }
}
