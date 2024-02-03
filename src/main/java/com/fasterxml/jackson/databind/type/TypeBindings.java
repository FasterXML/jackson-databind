package com.fasterxml.jackson.databind.type;

import java.lang.reflect.TypeVariable;
import java.util.*;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.util.ClassUtil;

/**
 * Helper class used for resolving type parameters for given class
 */
public class TypeBindings
    implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    private final static String[] NO_STRINGS = new String[0];

    private final static JavaType[] NO_TYPES = new JavaType[0];

    private final static TypeBindings EMPTY = new TypeBindings(NO_STRINGS, NO_TYPES, null);

    // // // Pre-resolved instances for minor optimizations

    // // // Actual member information

    /**
     * Array of type (type variable) names.
     */
    private final String[] _names;

    /**
     * Types matching names
     */
    private final JavaType[] _types;

    /**
     * Names of potentially unresolved type variables.
     *
     * @since 2.3
     */
    private final String[] _unboundVariables;

    private final int _hashCode;

    /*
    /**********************************************************************
    /* Construction
    /**********************************************************************
     */

    private TypeBindings(String[] names, JavaType[] types, String[] uvars)
    {
        _names = (names == null) ? NO_STRINGS : names;
        _types = (types == null) ? NO_TYPES : types;
        if (_names.length != _types.length) {
            throw new IllegalArgumentException("Mismatching names ("+_names.length+"), types ("+_types.length+")");
        }
        _unboundVariables = uvars;
        // hashCode and equality are based solely on _types.
        _hashCode = Arrays.hashCode(_types);
    }

    public static TypeBindings emptyBindings() {
        return EMPTY;
    }

    // Let's just canonicalize serialized EMPTY back to static instance, if need be
    protected Object readResolve() {
        if ((_names == null) || (_names.length == 0)) {
            return EMPTY;
        }
        return this;
    }

    /**
     * Factory method for constructing bindings for given class using specified type
     * parameters.
     */
    public static TypeBindings create(Class<?> erasedType, List<JavaType> typeList)
    {
        JavaType[] types = (typeList == null || typeList.isEmpty()) ?
                NO_TYPES : typeList.toArray(NO_TYPES);
        return create(erasedType, types);
    }

    public static TypeBindings create(Class<?> erasedType, JavaType[] types)
    {
        if (types == null) {
            types = NO_TYPES;
        } else switch (types.length) {
        case 1:
            return create(erasedType, types[0]);
        case 2:
            return create(erasedType, types[0], types[1]);
        }
        TypeVariable<?>[] vars = erasedType.getTypeParameters();
        String[] names;
        if (vars == null || vars.length == 0) {
            names = NO_STRINGS;
        } else {
            int len = vars.length;
            names = new String[len];
            for (int i = 0; i < len; ++i) {
                names[i] = vars[i].getName();
            }
        }
        // Check here to give better error message
        if (names.length != types.length) {
            throw new IllegalArgumentException("Cannot create TypeBindings for class "+erasedType.getName()
                   +" with "+types.length+" type parameter"
                   +((types.length == 1) ? "" : "s")+": class expects "+names.length);
        }
        return new TypeBindings(names, types, null);
    }

    public static TypeBindings create(Class<?> erasedType, JavaType typeArg1)
    {
        // 30-Oct-2015, tatu: Minor optimization for relatively common cases
        TypeVariable<?>[] vars = TypeParamStash.paramsFor1(erasedType);
        int varLen = (vars == null) ? 0 : vars.length;
        if (varLen != 1) {
            throw new IllegalArgumentException("Cannot create TypeBindings for class "+erasedType.getName()
                    +" with 1 type parameter: class expects "+varLen);
        }
        return new TypeBindings(new String[] { vars[0].getName() },
                new JavaType[] { typeArg1 }, null);
    }

    public static TypeBindings create(Class<?> erasedType, JavaType typeArg1, JavaType typeArg2)
    {
        // 30-Oct-2015, tatu: Minor optimization for relatively common cases
        TypeVariable<?>[] vars = TypeParamStash.paramsFor2(erasedType);
        int varLen = (vars == null) ? 0 : vars.length;
        if (varLen != 2) {
            throw new IllegalArgumentException("Cannot create TypeBindings for class "+erasedType.getName()
                    +" with 2 type parameters: class expects "+varLen);
        }
        return new TypeBindings(new String[] { vars[0].getName(), vars[1].getName() },
                new JavaType[] { typeArg1, typeArg2 }, null);
    }

    /**
     * Factory method for constructing bindings given names and associated types.
     */
    public static TypeBindings create(List<String> names, List<JavaType> types)
    {
        if (names == null || names.isEmpty() || types == null || types.isEmpty()) {
            return EMPTY;
        }
        return new TypeBindings(names.toArray(NO_STRINGS), types.toArray(NO_TYPES), null);
    }

    /**
     * Alternate factory method that may be called if it is possible that type
     * does or does not require type parameters; this is mostly useful for
     * collection- and map-like types.
     */
    public static TypeBindings createIfNeeded(Class<?> erasedType, JavaType typeArg1)
    {
        TypeVariable<?>[] vars = erasedType.getTypeParameters();
        int varLen = (vars == null) ? 0 : vars.length;
        if (varLen == 0) {
            return EMPTY;
        }
        if (varLen != 1) {
            throw new IllegalArgumentException("Cannot create TypeBindings for class "+erasedType.getName()
                    +" with 1 type parameter: class expects "+varLen);
        }
        return new TypeBindings(new String[] { vars[0].getName() },
                new JavaType[] { typeArg1 }, null);
    }

    /**
     * Alternate factory method that may be called if it is possible that type
     * does or does not require type parameters; this is mostly useful for
     * collection- and map-like types.
     */
    public static TypeBindings createIfNeeded(Class<?> erasedType, JavaType[] types)
    {
        TypeVariable<?>[] vars = erasedType.getTypeParameters();
        if (vars == null || vars.length == 0) {
            return EMPTY;
        }
        if (types == null) {
            types = NO_TYPES;
        }
        int len = vars.length;
        String[] names = new String[len];
        for (int i = 0; i < len; ++i) {
            names[i] = vars[i].getName();
        }
        // Check here to give better error message
        if (names.length != types.length) {
            throw new IllegalArgumentException("Cannot create TypeBindings for class "+erasedType.getName()
                   +" with "+types.length+" type parameter"
                   +((types.length == 1) ? "" : "s")+": class expects "+names.length);
        }
        return new TypeBindings(names, types, null);
    }

    /**
     * Method for creating an instance that has same bindings as this object,
     * plus an indicator for additional type variable that may be unbound within
     * this context; this is needed to resolve recursive self-references.
     */
    public TypeBindings withUnboundVariable(String name)
    {
        int len = (_unboundVariables == null) ? 0 : _unboundVariables.length;
        String[] names =  (len == 0)
                ? new String[1] : Arrays.copyOf(_unboundVariables, len+1);
        names[len] = name;
        return new TypeBindings(_names, _types, names);
    }

    /**
     * Create a new instance with the same bindings as this object, except with
     * the given variable removed. This is used to create generic types that are
     * "partially raw", i.e. only have some variables bound.
     *
     * @since 2.16
     */
    public TypeBindings withoutVariable(String name)
    {
        int index = Arrays.asList(_names).indexOf(name);
        if (index == -1) {
            return this;
        }
        JavaType[] newTypes = _types.clone();
        newTypes[index] = null;
        return new TypeBindings(_names, newTypes, _unboundVariables);
    }

    /*
    /**********************************************************************
    /* Accessors
    /**********************************************************************
     */

    /**
     * Find type bound to specified name, if there is one; returns bound type if so, null if not.
     */
    public JavaType findBoundType(String name)
    {
        for (int i = 0, len = _names.length; i < len; ++i) {
            if (name.equals(_names[i])) {
                JavaType t = _types[i];
                if (t instanceof ResolvedRecursiveType) {
                    ResolvedRecursiveType rrt = (ResolvedRecursiveType) t;
                    JavaType t2 = rrt.getSelfReferencedType();
                    if (t2 != null) {
                        t = t2;
                    } else {
                        /* 25-Feb-2016, tatu: Looks like a potential problem, but alas
                         *   we have a test where this should NOT fail and things... seem
                         *   to work. So be it.
                         */
/*
                        throw new IllegalStateException(String.format
("Unresolved ResolvedRecursiveType for parameter '%s' (index #%d; erased type %s)",
name, i, t.getRawClass()));
*/
                    }
                }
                return t;
            }
        }
        return null;
    }

    /**
     * Returns true if a shallow search of the type bindings includes a placeholder
     * type which uses reference equality, thus cannot produce cache hits. This
     * is an optimization to avoid churning memory in the cache unnecessarily.
     * Note that it is still possible for nested type information to contain such
     * placeholder types (see NestedTypes1604Test for an example) so it's vital
     * that they produce a distribution of hashCode values, even if they may push
     * reusable data out of the cache.
     */
    private boolean invalidCacheKey() {
        for (JavaType type : _types) {
            if (type instanceof IdentityEqualityType) {
                return true;
            }
        }
        return false;
    }

    public boolean isEmpty() {
        return (_types.length == 0);
    }

    /**
     * Returns number of bindings contained
     */
    public int size() {
        return _types.length;
    }

    public String getBoundName(int index)
    {
        if (index < 0 || index >= _names.length) {
            return null;
        }
        return _names[index];
    }

    /**
     * Get the type bound to the variable at {@code index}. If the type is {@link #withoutVariable(String) not bound}
     * but the index is within {@link #size()} constraints, this method returns {@link TypeFactory#unknownType()} for
     * compatibility. If the index is out of {@link #size()} constraints, this method will still return {@code null}.
     */
    public JavaType getBoundType(int index)
    {
        if (index < 0 || index >= _types.length) {
            return null;
        }
        JavaType type = _types[index];
        if (type == null) {
            type = TypeFactory.unknownType();
        }
        return type;
    }

    /**
     * Get the type bound to the variable at {@code index}. If the type is {@link #withoutVariable(String) not bound}
     * or the index is within {@link #size()} constraints, this method returns {@code null}.
     *
     * @since 2.16
     */
    public JavaType getBoundTypeOrNull(int index)
    {
        if (index < 0 || index >= _types.length) {
            return null;
        }
        return _types[index];
    }

    /**
     * Accessor for getting bound types in declaration order
     */
    public List<JavaType> getTypeParameters()
    {
        if (_types.length == 0) {
            return Collections.emptyList();
        }
        List<JavaType> list = Arrays.asList(_types);
        if (list.contains(null)) {
            // jackson-databind#4122
            list = new ArrayList<>(list);
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i) == null) {
                    list.set(i, TypeFactory.unknownType());
                }
            }
        }
        return list;
    }

    /**
     * @since 2.3
     */
    public boolean hasUnbound(String name) {
        if (_unboundVariables != null) {
            for (int i = _unboundVariables.length; --i >= 0; ) {
                if (name.equals(_unboundVariables[i])) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Factory method that will create an object that can be used as a key for
     * caching purposes by {@link TypeFactory}
     *
     * @return An object which can be used as a key in TypeFactory, or {@code null} if no key can be created.
     *
     * @since 2.8
     */
    public Object asKey(Class<?> rawBase) {
        // safe to pass _types array without copy since it is not exposed via
        // any access, nor modified by this class
        if (invalidCacheKey()) {
            // If placeholders are present, no key may be returned because the key is unhelpful without context.
            return null;
        }
        return new AsKey(rawBase, _types, _hashCode);
    }

    /*
    /**********************************************************************
    /* Standard methods
    /**********************************************************************
     */

    @Override public String toString()
    {
        if (_types.length == 0) {
            return "<>";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('<');
        for (int i = 0, len = _types.length; i < len; ++i) {
            if (i > 0) {
                sb.append(',');
            }
//            sb = _types[i].appendBriefDescription(sb);
            JavaType type = _types[i];
            if (type == null) {
                sb.append("?");
            } else {
                sb.append(type.getGenericSignature());
            }
        }
        sb.append('>');
        return sb.toString();
    }

    @Override public int hashCode() { return _hashCode; }

    @Override public boolean equals(Object o)
    {
        if (o == this) return true;
        if (!ClassUtil.hasClass(o, getClass())) {
            return false;
        }
        TypeBindings other = (TypeBindings) o;
        // hashCode and equality are based solely on _types.
        return _hashCode == other._hashCode && Arrays.equals(_types, other._types);
    }

    /*
    /**********************************************************************
    /* Package accessible methods
    /**********************************************************************
     */

    protected JavaType[] typeParameterArray() {
        return _types;
    }

    /*
    /**********************************************************************
    /* Helper classes
    /**********************************************************************
     */

    // 30-Oct-2015, tatu: Surprising, but looks like type parameters access can be bit of
    //    a hot spot. So avoid for a small number of common generic types. Note that we do
    //    need both common abstract types and concrete ones; latter for specialization

    /**
     * Helper class that contains simple logic for avoiding repeated lookups via
     * {@link Class#getTypeParameters()} as that can be a performance issue for
     * some use cases (wasteful, usually one-off or not reusing mapper).
     * Partly isolated to avoid initialization for cases where no generic types are
     * used.
     */
    static class TypeParamStash {
        private final static TypeVariable<?>[] VARS_ABSTRACT_LIST = AbstractList.class.getTypeParameters();
        private final static TypeVariable<?>[] VARS_COLLECTION = Collection.class.getTypeParameters();
        private final static TypeVariable<?>[] VARS_ITERABLE = Iterable.class.getTypeParameters();
        private final static TypeVariable<?>[] VARS_LIST = List.class.getTypeParameters();
        private final static TypeVariable<?>[] VARS_ARRAY_LIST = ArrayList.class.getTypeParameters();

        private final static TypeVariable<?>[] VARS_MAP = Map.class.getTypeParameters();
        private final static TypeVariable<?>[] VARS_HASH_MAP = HashMap.class.getTypeParameters();
        private final static TypeVariable<?>[] VARS_LINKED_HASH_MAP = LinkedHashMap.class.getTypeParameters();

        public static TypeVariable<?>[] paramsFor1(Class<?> erasedType)
        {
            if (erasedType == Collection.class) {
                return VARS_COLLECTION;
            }
            if (erasedType == List.class) {
                return VARS_LIST;
            }
            if (erasedType == ArrayList.class) {
                return VARS_ARRAY_LIST;
            }
            if (erasedType == AbstractList.class) {
                return VARS_ABSTRACT_LIST;
            }
            if (erasedType == Iterable.class) {
                return VARS_ITERABLE;
            }
            return erasedType.getTypeParameters();
        }

        public static TypeVariable<?>[] paramsFor2(Class<?> erasedType)
        {
            if (erasedType == Map.class) {
                return VARS_MAP;
            }
            if (erasedType == HashMap.class) {
                return VARS_HASH_MAP;
            }
            if (erasedType == LinkedHashMap.class) {
                return VARS_LINKED_HASH_MAP;
            }
            return erasedType.getTypeParameters();
        }
    }

    /**
     * Helper type used to allow caching of generic types
     *
     * @since 2.8
     */
    final static class AsKey {
        private final Class<?> _raw;
        private final JavaType[] _params;
        private final int _hash;

        public AsKey(Class<?> raw, JavaType[] params, int hash) {
            _raw = raw ;
            _params = params;
            _hash = 31 * raw.hashCode() + hash;
        }

        @Override
        public int hashCode() { return _hash; }

        @Override
        public boolean equals(Object o) {
            if (o == this) return true;
            if (o == null) return false;
            if (o.getClass() != getClass()) return false;
            AsKey other = (AsKey) o;

            if ((_hash == other._hash) && (_raw == other._raw)) {
                final JavaType[] otherParams = other._params;
                final int len = _params.length;

                if (len == otherParams.length) {
                    for (int i = 0; i < len; ++i) {
                        if (!Objects.equals(_params[i], otherParams[i])) {
                            return false;
                        }
                    }
                    return true;
                }
            }
            return false;
        }

        @Override
        public String toString() {
            return _raw.getName()+"<>";
        }
    }
}
