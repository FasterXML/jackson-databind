package com.fasterxml.jackson.databind.type;

import java.util.*;
import java.lang.reflect.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.util.ArrayBuilders;
import com.fasterxml.jackson.databind.util.LRUMap;

/**
 * Class used for creating concrete {@link JavaType} instances,
 * given various inputs.
 *<p>
 * Instances of this class are accessible using {@link com.fasterxml.jackson.databind.ObjectMapper}
 * as well as many objects it constructs (like
* {@link com.fasterxml.jackson.databind.DeserializationConfig} and
 * {@link com.fasterxml.jackson.databind.SerializationConfig})),
 * but usually those objects also 
 * expose convenience methods (<code>constructType</code>).
 * So, you can do for example:
 *<pre>
 *   JavaType stringType = mapper.constructType(String.class);
 *</pre>
 * However, more advanced methods are only exposed by factory so that you
 * may need to use:
 *<pre>
 *   JavaType stringCollection = mapper.getTypeFactory().constructCollectionType(List.class, String.class);
 *</pre>
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public final class TypeFactory
    implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    private final static JavaType[] NO_TYPES = new JavaType[0];

    /**
     * Globally shared singleton. Not accessed directly; non-core
     * code should use per-ObjectMapper instance (via configuration objects).
     * Core Jackson code uses {@link #defaultInstance} for accessing it.
     */
    protected final static TypeFactory instance = new TypeFactory();
    
    /*
    /**********************************************************
    /* Caching
    /**********************************************************
     */

    // // // Let's assume that a small set of core primitive/basic types
    // // // will not be modified, and can be freely shared to streamline
    // // // parts of processing
    
    protected final static SimpleType CORE_TYPE_STRING = new SimpleType(String.class);
    protected final static SimpleType CORE_TYPE_BOOL = new SimpleType(Boolean.TYPE);
    protected final static SimpleType CORE_TYPE_INT = new SimpleType(Integer.TYPE);
    protected final static SimpleType CORE_TYPE_LONG = new SimpleType(Long.TYPE);

    /**
     * Since type resolution can be expensive (specifically when resolving
     * actual generic types), we will use small cache to avoid repetitive
     * resolution of core types
     */
    protected final LRUMap<ClassKey, JavaType> _typeCache = new LRUMap<ClassKey, JavaType>(16, 100);

    /*
     * Looks like construction of {@link JavaType} instances can be
     * a bottleneck, esp. for root-level Maps, so we better do bit
     * of low-level component caching here...
     */
    
    /**
     * Lazily constructed copy of type hierarchy from {@link java.util.HashMap}
     * to its supertypes.
     */
    protected transient HierarchicType _cachedHashMapType;

    /**
     * Lazily constructed copy of type hierarchy from {@link java.util.ArrayList}
     * to its supertypes.
     */
    protected transient HierarchicType _cachedArrayListType;
    
    /*
    /**********************************************************
    /* Configuration
    /**********************************************************
     */
    
    /**
     * Registered {@link TypeModifier}s: objects that can change details
     * of {@link JavaType} instances factory constructs.
     */
    protected final TypeModifier[] _modifiers;
    
    protected final TypeParser _parser;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    private TypeFactory() {
        _parser = new TypeParser(this);
        _modifiers = null;
    }

    protected TypeFactory(TypeParser p, TypeModifier[] mods) {
        _parser = p;
        _modifiers = mods;
    }

    public TypeFactory withModifier(TypeModifier mod) 
    {
        if (_modifiers == null) {
            return new TypeFactory(_parser, new TypeModifier[] { mod });
        }
        return new TypeFactory(_parser, ArrayBuilders.insertInListNoDup(_modifiers, mod));
    }

    /**
     * Method used to access the globally shared instance, which has
     * no custom configuration. Used by <code>ObjectMapper</code> to
     * get the default factory when constructed.
     */
    public static TypeFactory defaultInstance() { return instance; }

    /*
    /**********************************************************
    /* Static methods for non-instance-specific functionality
    /**********************************************************
     */
    
    /**
     * Method for constructing a marker type that indicates missing generic
     * type information, which is handled same as simple type for
     * <code>java.lang.Object</code>.
     */
    public static JavaType unknownType() {
        return defaultInstance()._unknownType();
    }

    /**
     * Static helper method that can be called to figure out type-erased
     * call for given JDK type. It can be called statically since type resolution
     * process can never change actual type-erased class; thereby static
     * default instance is used for determination.
     */
    public static Class<?> rawClass(Type t) {
        if (t instanceof Class<?>) {
            return (Class<?>) t;
        }
        // Shouldbe able to optimize bit more in future...
        return defaultInstance().constructType(t).getRawClass();
    }
    
    /*
    /**********************************************************
    /* Type conversion, parameterization resolution methods
    /**********************************************************
     */

    /**
     * Factory method for creating a subtype of given base type, as defined
     * by specified subclass; but retaining generic type information if any.
     * Can be used, for example, to get equivalent of "HashMap&lt;String,Integer>"
     * from "Map&ltString,Integer>" by giving <code>HashMap.class</code>
     * as subclass.
     */
    public JavaType constructSpecializedType(JavaType baseType, Class<?> subclass)
    {
        // simple optimization to avoid costly introspection if type-erased type does NOT differ
        if (baseType.getRawClass() == subclass) {
            return baseType;
        }
        // Currently only SimpleType instances can become something else
        if (baseType instanceof SimpleType) {
            // and only if subclass is an array, Collection or Map
            if (subclass.isArray()
                || Map.class.isAssignableFrom(subclass)
                || Collection.class.isAssignableFrom(subclass)) {
                // need to assert type compatibility...
                if (!baseType.getRawClass().isAssignableFrom(subclass)) {
                    throw new IllegalArgumentException("Class "+subclass.getClass().getName()+" not subtype of "+baseType);
                }
                // this _should_ work, right?
                JavaType subtype = _fromClass(subclass, new TypeBindings(this, baseType.getRawClass()));
                // one more thing: handlers to copy?
                Object h = baseType.getValueHandler();
                if (h != null) {
                    subtype = subtype.withValueHandler(h);
                }
                h = baseType.getTypeHandler();
                if (h != null) {
                    subtype = subtype.withTypeHandler(h);
                }
                return subtype;
            }
        }
        // otherwise regular narrowing should work just fine
        return baseType.narrowBy(subclass);
    }

    /**
     * Factory method for constructing a {@link JavaType} out of its canonical
     * representation (see {@link JavaType#toCanonical()}).
     * 
     * @param canonical Canonical string representation of a type
     * 
     * @throws IllegalArgumentException If canonical representation is malformed,
     *   or class that type represents (including its generic parameters) is
     *   not found
     */
    public JavaType constructFromCanonical(String canonical) throws IllegalArgumentException
    {
        return _parser.parse(canonical);
    }
    
    /**
     * Method that is to figure out actual type parameters that given
     * class binds to generic types defined by given (generic)
     * interface or class.
     * This could mean, for example, trying to figure out
     * key and value types for Map implementations.
     * 
     * @param type Sub-type (leaf type) that implements <code>expType</code>
     */
    public JavaType[] findTypeParameters(JavaType type, Class<?> expType)
    {
        /* Tricky part here is that some JavaType instances have been constructed
         * from generic type (usually via TypeReference); and in those case
         * types have been resolved. Alternative is that the leaf type is type-erased
         * class, in which case this has not been done.
         * For now simplest way to handle this is to split processing in two: latter
         * case actually fully works; and former mostly works. In future may need to
         * rewrite former part, which requires changes to JavaType as well.
         */
        Class<?> raw = type.getRawClass();
        if (raw == expType) {
            // Direct type info; good since we can return it as is
            int count = type.containedTypeCount();
            if (count == 0) return null;
            JavaType[] result = new JavaType[count];
            for (int i = 0; i < count; ++i) {
                result[i] = type.containedType(i);
            }
            return result;
        }
        /* Otherwise need to go through type-erased class. This may miss cases where
         * we get generic type; ideally JavaType/SimpleType would retain information
         * about generic declaration at main level... but let's worry about that
         * if/when there are problems; current handling is an improvement over earlier
         * code.
         */
        return findTypeParameters(raw, expType, new TypeBindings(this, type));
    }

    public JavaType[] findTypeParameters(Class<?> clz, Class<?> expType) {
        return findTypeParameters(clz, expType, new TypeBindings(this, clz));
    }
    
    public JavaType[] findTypeParameters(Class<?> clz, Class<?> expType, TypeBindings bindings)
    {
        // First: find full inheritance chain
        HierarchicType subType = _findSuperTypeChain(clz, expType);
        // Caller is supposed to ensure this never happens, so:
        if (subType == null) {
            throw new IllegalArgumentException("Class "+clz.getName()+" is not a subtype of "+expType.getName());
        }
        // Ok and then go to the ultimate super-type:
        HierarchicType superType = subType;
        while (superType.getSuperType() != null) {
            superType = superType.getSuperType();
            Class<?> raw = superType.getRawClass();
            TypeBindings newBindings = new TypeBindings(this, raw);
            if (superType.isGeneric()) { // got bindings, need to resolve
                ParameterizedType pt = superType.asGeneric();
                Type[] actualTypes = pt.getActualTypeArguments();
                TypeVariable<?>[] vars = raw.getTypeParameters();
                int len = actualTypes.length;
                for (int i = 0; i < len; ++i) {
                    String name = vars[i].getName();
                    JavaType type = _constructType(actualTypes[i], bindings);
                    newBindings.addBinding(name, type);
                }
            }
            bindings = newBindings;
        }

        // which ought to be generic (if not, it's raw type)
        if (!superType.isGeneric()) {
            return null;
        }
        return bindings.typesAsArray();
    }

    /**
     * Method that can be called to figure out more specific of two
     * types (if they are related; that is, one implements or extends the
     * other); or if not related, return the primary type.
     * 
     * @param type1 Primary type to consider
     * @param type2 Secondary type to consider
     * 
     * @since 2.2
     */
    public JavaType moreSpecificType(JavaType type1, JavaType type2)
    {
        if (type1 == null) {
            return type2;
        }
        if (type2 == null) {
            return type1;
        }
        Class<?> raw1 = type1.getRawClass();
        Class<?> raw2 = type2.getRawClass();
        if (raw1 == raw2) {
            return type1;
        }
        // TODO: maybe try sub-classing, to retain generic types?
        if (raw1.isAssignableFrom(raw2)) {
            return type2;
        }
        return type1;
    }
    
    /*
    /**********************************************************
    /* Public factory methods
    /**********************************************************
     */

    public JavaType constructType(Type type) {
        return _constructType(type, null);
    }

    public JavaType constructType(Type type, TypeBindings bindings) {
        return _constructType(type, bindings);
    }
    
    public JavaType constructType(TypeReference<?> typeRef) {
        return _constructType(typeRef.getType(), null);
    }
    
    public JavaType constructType(Type type, Class<?> context) {
        TypeBindings b = (context == null) ? null : new TypeBindings(this, context);
        return _constructType(type, b);
    }

    public JavaType constructType(Type type, JavaType context) {
        TypeBindings b = (context == null) ? null : new TypeBindings(this, context);
        return _constructType(type, b);
    }
    
    /**
     * Factory method that can be used if type information is passed
     * as Java typing returned from <code>getGenericXxx</code> methods
     * (usually for a return or argument type).
     */
    protected JavaType _constructType(Type type, TypeBindings context)
    {
        JavaType resultType;

        // simple class?
        if (type instanceof Class<?>) {
            Class<?> cls = (Class<?>) type;
            resultType = _fromClass(cls, context);
        }
        // But if not, need to start resolving.
        else if (type instanceof ParameterizedType) {
            resultType = _fromParamType((ParameterizedType) type, context);
        }
        else if (type instanceof JavaType) { // [Issue#116]
            return (JavaType) type;
        }
        else if (type instanceof GenericArrayType) {
            resultType = _fromArrayType((GenericArrayType) type, context);
        }
        else if (type instanceof TypeVariable<?>) {
            resultType = _fromVariable((TypeVariable<?>) type, context);
        }
        else if (type instanceof WildcardType) {
            resultType = _fromWildcard((WildcardType) type, context);
        } else {
            // sanity check
            throw new IllegalArgumentException("Unrecognized Type: "+((type == null) ? "[null]" : type.toString()));
        }
        /* [JACKSON-521]: Need to allow TypeModifiers to alter actual type; however,
         * for now only call for simple types (i.e. not for arrays, map or collections).
         * Can be changed in future it necessary
         */
        if (_modifiers != null && !resultType.isContainerType()) {
            for (TypeModifier mod : _modifiers) {
                resultType = mod.modifyType(resultType, type, context, this);
            }
        }
        return resultType;
    }

    /*
    /**********************************************************
    /* Direct factory methods
    /**********************************************************
     */

    /**
     * Method for constructing an {@link ArrayType}.
     *<p>
     * NOTE: type modifiers are NOT called on array type itself; but are called
     * for element type (and other contained types)
     */
    public ArrayType constructArrayType(Class<?> elementType) {
        return ArrayType.construct(_constructType(elementType, null), null, null);
    }
    
    /**
     * Method for constructing an {@link ArrayType}.
     *<p>
     * NOTE: type modifiers are NOT called on array type itself; but are called
     * for contained types.
     */
    public ArrayType constructArrayType(JavaType elementType) {
        return ArrayType.construct(elementType, null, null);
    }

    /**
     * Method for constructing a {@link CollectionType}.
     *<p>
     * NOTE: type modifiers are NOT called on Collection type itself; but are called
     * for contained types.
     */
    public CollectionType constructCollectionType(Class<? extends Collection> collectionClass, Class<?> elementClass) {
        return CollectionType.construct(collectionClass, constructType(elementClass));
    }
    
    /**
     * Method for constructing a {@link CollectionType}.
     *<p>
     * NOTE: type modifiers are NOT called on Collection type itself; but are called
     * for contained types.
     */
    public CollectionType constructCollectionType(Class<? extends Collection> collectionClass, JavaType elementType) {
        return CollectionType.construct(collectionClass, elementType);
    }

    /**
     * Method for constructing a {@link CollectionLikeType}.
     *<p>
     * NOTE: type modifiers are NOT called on constructed type itself; but are called
     * for contained types.
     */
    public CollectionLikeType constructCollectionLikeType(Class<?> collectionClass, Class<?> elementClass) {
        return CollectionLikeType.construct(collectionClass, constructType(elementClass));
    }
    
    /**
     * Method for constructing a {@link CollectionLikeType}.
     *<p>
     * NOTE: type modifiers are NOT called on constructed type itself; but are called
     * for contained types.
     */
    public CollectionLikeType constructCollectionLikeType(Class<?> collectionClass, JavaType elementType) {
        return CollectionLikeType.construct(collectionClass, elementType);
    }
    
    /**
     * Method for constructing a {@link MapType} instance
     *<p>
     * NOTE: type modifiers are NOT called on constructed type itself; but are called
     * for contained types.
     */
    public MapType constructMapType(Class<? extends Map> mapClass, JavaType keyType, JavaType valueType) {
        return MapType.construct(mapClass, keyType, valueType);
    }

    /**
     * Method for constructing a {@link MapType} instance
     *<p>
     * NOTE: type modifiers are NOT called on constructed type itself; but are called
     * for contained types.
     */
    public MapType constructMapType(Class<? extends Map> mapClass, Class<?> keyClass, Class<?> valueClass) {
        return MapType.construct(mapClass, constructType(keyClass), constructType(valueClass));
    }

    /**
     * Method for constructing a {@link MapLikeType} instance
     *<p>
     * NOTE: type modifiers are NOT called on constructed type itself; but are called
     * for contained types.
     */
    public MapLikeType constructMapLikeType(Class<?> mapClass, JavaType keyType, JavaType valueType) {
        return MapLikeType.construct(mapClass, keyType, valueType);
    }
    
    /**
     * Method for constructing a {@link MapLikeType} instance
     *<p>
     * NOTE: type modifiers are NOT called on constructed type itself; but are called
     * for contained types.
     */
    public MapLikeType constructMapLikeType(Class<?> mapClass, Class<?> keyClass, Class<?> valueClass) {
        return MapType.construct(mapClass, constructType(keyClass), constructType(valueClass));
    }
    
    /**
     * Method for constructing a type instance with specified parameterization.
     */
    public JavaType constructSimpleType(Class<?> rawType, JavaType[] parameterTypes)
    {
        // Quick sanity check: must match numbers of types with expected...
        TypeVariable<?>[] typeVars = rawType.getTypeParameters();
        if (typeVars.length != parameterTypes.length) {
            throw new IllegalArgumentException("Parameter type mismatch for "+rawType.getName()
                    +": expected "+typeVars.length+" parameters, was given "+parameterTypes.length);
        }
        String[] names = new String[typeVars.length];
        for (int i = 0, len = typeVars.length; i < len; ++i) {
            names[i] = typeVars[i].getName();
        }
        JavaType resultType = new SimpleType(rawType, names, parameterTypes, null, null, false);
        return resultType;
    } 

    /**
     * Method that will force construction of a simple type, without trying to
     * check for more specialized types.
     *<p> 
     * NOTE: no type modifiers are called on type either, so calling this method
     * should only be used if caller really knows what it's doing...
     */
    public JavaType uncheckedSimpleType(Class<?> cls) {
        return new SimpleType(cls);
    }
    
    /**
     * Factory method for constructing {@link JavaType} that
     * represents a parameterized type. For example, to represent
     * type <code>List&lt;Set&lt;Integer>></code>, you could
     * call
     *<pre>
     *  TypeFactory.parametricType(List.class, Integer.class);
     *</pre>
     *<p>
     * NOTE: type modifiers are NOT called on constructed type itself; but are called
     * for contained types.
     */
    public JavaType constructParametricType(Class<?> parametrized, Class<?>... parameterClasses)
    {
        int len = parameterClasses.length;
        JavaType[] pt = new JavaType[len];
        for (int i = 0; i < len; ++i) {
            pt[i] = _fromClass(parameterClasses[i], null);
        }
        return constructParametricType(parametrized, pt);
    }

    /**
     * Factory method for constructing {@link JavaType} that
     * represents a parameterized type. For example, to represent
     * type <code>List&lt;Set&lt;Integer>></code>, you could
     * call
     *<pre>
     *  JavaType inner = TypeFactory.parametricType(Set.class, Integer.class);
     *  TypeFactory.parametricType(List.class, inner);
     *</pre>
     *<p>
     * NOTE: type modifiers are NOT called on constructed type itself; but are called
     * for contained types.
     */
    public JavaType constructParametricType(Class<?> parametrized, JavaType... parameterTypes)
    {
        JavaType resultType;
        
        // Need to check kind of class we are dealing with...
        if (parametrized.isArray()) {
            // 19-Jan-2010, tatus: should we support multi-dimensional arrays directly?
            if (parameterTypes.length != 1) {
                throw new IllegalArgumentException("Need exactly 1 parameter type for arrays ("+parametrized.getName()+")");
            }
            resultType = constructArrayType(parameterTypes[0]);
        }
        else if (Map.class.isAssignableFrom(parametrized)) {
            if (parameterTypes.length != 2) {
                throw new IllegalArgumentException("Need exactly 2 parameter types for Map types ("+parametrized.getName()+")");
            }
            resultType = constructMapType((Class<Map<?,?>>)parametrized, parameterTypes[0], parameterTypes[1]);
        }
        else if (Collection.class.isAssignableFrom(parametrized)) {
            if (parameterTypes.length != 1) {
                throw new IllegalArgumentException("Need exactly 1 parameter type for Collection types ("+parametrized.getName()+")");
            }
            resultType = constructCollectionType((Class<Collection<?>>)parametrized, parameterTypes[0]);
        } else {
            resultType = constructSimpleType(parametrized, parameterTypes);
        }
        return resultType;
    }

    /*
    /**********************************************************
    /* Direct factory methods for "raw" variants, used when
    /* parameterization is unknown
    /**********************************************************
     */

    /**
     * Method that can be used to construct "raw" Collection type; meaning that its
     * parameterization is unknown.
     * This is similar to using <code>Object.class</code> parameterization,
     * and is equivalent to calling:
     *<pre>
     *  typeFactory.constructCollectionType(collectionClass, typeFactory.unknownType());
     *<pre>
     *<p>
     * This method should only be used if parameterization is completely unavailable.
     */
    public CollectionType constructRawCollectionType(Class<? extends Collection> collectionClass) {
        return CollectionType.construct(collectionClass, unknownType());
    }

    /**
     * Method that can be used to construct "raw" Collection-like type; meaning that its
     * parameterization is unknown.
     * This is similar to using <code>Object.class</code> parameterization,
     * and is equivalent to calling:
     *<pre>
     *  typeFactory.constructCollectionLikeType(collectionClass, typeFactory.unknownType());
     *<pre>
     *<p>
     * This method should only be used if parameterization is completely unavailable.
     */
    public CollectionLikeType constructRawCollectionLikeType(Class<?> collectionClass) {
        return CollectionLikeType.construct(collectionClass, unknownType());
    }

    /**
     * Method that can be used to construct "raw" Map type; meaning that its
     * parameterization is unknown.
     * This is similar to using <code>Object.class</code> parameterization,
     * and is equivalent to calling:
     *<pre>
     *  typeFactory.constructMapType(collectionClass, typeFactory.unknownType(), typeFactory.unknownType());
     *<pre>
     *<p>
     * This method should only be used if parameterization is completely unavailable.
     */
    public MapType constructRawMapType(Class<? extends Map> mapClass) {
        return MapType.construct(mapClass, unknownType(), unknownType());
    }

    /**
     * Method that can be used to construct "raw" Map-like type; meaning that its
     * parameterization is unknown.
     * This is similar to using <code>Object.class</code> parameterization,
     * and is equivalent to calling:
     *<pre>
     *  typeFactory.constructMapLikeType(collectionClass, typeFactory.unknownType(), typeFactory.unknownType());
     *<pre>
     *<p>
     * This method should only be used if parameterization is completely unavailable.
     */
    public MapLikeType constructRawMapLikeType(Class<?> mapClass) {
        return MapLikeType.construct(mapClass, unknownType(), unknownType());
    }

    /*
    /**********************************************************
    /* Actual factory methods
    /**********************************************************
     */

    /**
     * @param context Mapping of formal parameter declarations (for generic
     *   types) into actual types
     */
    protected JavaType _fromClass(Class<?> clz, TypeBindings context)
    {
        // Very first thing: small set of core types we know well:
        if (clz == String.class) return CORE_TYPE_STRING;
        if (clz == Boolean.TYPE) return CORE_TYPE_BOOL;
        if (clz == Integer.TYPE) return CORE_TYPE_INT;
        if (clz == Long.TYPE) return CORE_TYPE_LONG;
        
        // Barring that, we may have recently constructed an instance:
        ClassKey key = new ClassKey(clz);
        JavaType result = _typeCache.get(key); // ok, cache object is synced
        if (result != null) {
            return result;
        }

        // If context was needed, weed do:
        /*
        if (context == null) {
            context = new TypeBindings(this, cls);
        }
        */
        
        // First: do we have an array type?
        if (clz.isArray()) {
            result = ArrayType.construct(_constructType(clz.getComponentType(), null), null, null);
            /* Also: although enums can also be fully resolved, there's little
             * point in doing so (T extends Enum<T>) etc.
             */
        } else if (clz.isEnum()) {
            result = new SimpleType(clz);
            /* Maps and Collections aren't quite as hot; problem is, due
             * to type erasure we often do not know typing and can only assume
             * base Object.
             */
        } else if (Map.class.isAssignableFrom(clz)) {
            result = _mapType(clz);
        } else if (Collection.class.isAssignableFrom(clz)) {
            result =  _collectionType(clz);
        } else {
            result = new SimpleType(clz);
        }
        _typeCache.put(key, result); // cache object syncs
        return result;
    }
    
    /**
     * Method used by {@link TypeParser} when generics-aware version
     * is constructed.
     */
    protected JavaType _fromParameterizedClass(Class<?> clz, List<JavaType> paramTypes)
    {
        if (clz.isArray()) { // ignore generics (should never have any)
            return ArrayType.construct(_constructType(clz.getComponentType(), null), null, null);
        }
        if (clz.isEnum()) { // ditto for enums
            return new SimpleType(clz);
        }
        if (Map.class.isAssignableFrom(clz)) {
            // First: if we do have param types, use them
            JavaType keyType, contentType;
            if (paramTypes.size() > 0) {
                keyType = paramTypes.get(0);
                contentType = (paramTypes.size() >= 2) ?
                        paramTypes.get(1) : _unknownType();
                return MapType.construct(clz, keyType, contentType);
            }
            return _mapType(clz);
        }
        if (Collection.class.isAssignableFrom(clz)) {
            if (paramTypes.size() >= 1) {
                return CollectionType.construct(clz, paramTypes.get(0));
            }
            return _collectionType(clz);
        }
        if (paramTypes.size() == 0) {
            return new SimpleType(clz);
        }
        JavaType[] pt = paramTypes.toArray(new JavaType[paramTypes.size()]);
        return constructSimpleType(clz, pt);
    }
    
    /**
     * This method deals with parameterized types, that is,
     * first class generic classes.
     */
    protected JavaType _fromParamType(ParameterizedType type, TypeBindings context)
    {
        /* First: what is the actual base type? One odd thing
         * is that 'getRawType' returns Type, not Class<?> as
         * one might expect. But let's assume it is always of
         * type Class: if not, need to add more code to resolve
         * it to Class.
         */
        Class<?> rawType = (Class<?>) type.getRawType();
        Type[] args = type.getActualTypeArguments();
        int paramCount = (args == null) ? 0 : args.length;

        JavaType[] pt;
        
        if (paramCount == 0) {
            pt = NO_TYPES;
        } else {
            pt = new JavaType[paramCount];
            for (int i = 0; i < paramCount; ++i) {
                pt[i] = _constructType(args[i], context);
            }
        }

        // Ok: Map or Collection?
        if (Map.class.isAssignableFrom(rawType)) {
            JavaType subtype = constructSimpleType(rawType, pt);
            JavaType[] mapParams = findTypeParameters(subtype, Map.class);
            if (mapParams.length != 2) {
                throw new IllegalArgumentException("Could not find 2 type parameters for Map class "+rawType.getName()+" (found "+mapParams.length+")");
            }
            return MapType.construct(rawType, mapParams[0], mapParams[1]);
        }
        if (Collection.class.isAssignableFrom(rawType)) {
            JavaType subtype = constructSimpleType(rawType, pt);
            JavaType[] collectionParams = findTypeParameters(subtype, Collection.class);
            if (collectionParams.length != 1) {
                throw new IllegalArgumentException("Could not find 1 type parameter for Collection class "+rawType.getName()+" (found "+collectionParams.length+")");
            }
            return CollectionType.construct(rawType, collectionParams[0]);
        }
        if (paramCount == 0) { // no generics
            return new SimpleType(rawType);
        }
        return constructSimpleType(rawType, pt);
    }

    
    protected JavaType _fromArrayType(GenericArrayType type, TypeBindings context)
    {
        JavaType compType = _constructType(type.getGenericComponentType(), context);
        return ArrayType.construct(compType, null, null);
    }

    protected JavaType _fromVariable(TypeVariable<?> type, TypeBindings context)
    {
        /* 26-Sep-2009, tatus: It should be possible to try "partial"
         *  resolution; meaning that it is ok not to find bindings.
         *  For now this is indicated by passing null context.
         */
        if (context == null) {
            return _unknownType();
        }

        // Ok: here's where context might come in handy!
        String name = type.getName();
        JavaType actualType = context.findType(name);
        if (actualType != null) {
            return actualType;
        }

        /* 29-Jan-2010, tatu: We used to throw exception here, if type was
         *   bound: but the problem is that this can occur for generic "base"
         *   method, overridden by sub-class. If so, we will want to ignore
         *   current type (for method) since it will be masked.
         */
        Type[] bounds = type.getBounds();

        // With type variables we must use bound information.
        // Theoretically this gets tricky, as there may be multiple
        // bounds ("... extends A & B"); and optimally we might
        // want to choose the best match. Also, bounds are optional;
        // but here we are lucky in that implicit "Object" is
        // added as bounds if so.
        // Either way let's just use the first bound, for now, and
        // worry about better match later on if there is need.

        /* 29-Jan-2010, tatu: One more problem are recursive types
         *   (T extends Comparable<T>). Need to add "placeholder"
         *   for resolution to catch those.
         */
        context._addPlaceholder(name);        
        return _constructType(bounds[0], context);
    }

    protected JavaType _fromWildcard(WildcardType type, TypeBindings context)
    {
        /* Similar to challenges with TypeVariable, we may have
         * multiple upper bounds. But it is also possible that if
         * upper bound defaults to Object, we might want to consider
         * lower bounds instead.
         *
         * For now, we won't try anything more advanced; above is
         * just for future reference.
         */
        return _constructType(type.getUpperBounds()[0], context);
    }

    private JavaType _mapType(Class<?> rawClass)
    {
        JavaType[] typeParams = findTypeParameters(rawClass, Map.class);
        // ok to have no types ("raw")
        if (typeParams == null) {
            return MapType.construct(rawClass, _unknownType(), _unknownType());
        }
        // but exactly 2 types if any found
        if (typeParams.length != 2) {
            throw new IllegalArgumentException("Strange Map type "+rawClass.getName()+": can not determine type parameters");
        }
        return MapType.construct(rawClass, typeParams[0], typeParams[1]);
    }

    private JavaType _collectionType(Class<?> rawClass)
    {
        JavaType[] typeParams = findTypeParameters(rawClass, Collection.class);
        // ok to have no types ("raw")
        if (typeParams == null) {
            return CollectionType.construct(rawClass, _unknownType());
        }
        // but exactly 2 types if any found
        if (typeParams.length != 1) {
            throw new IllegalArgumentException("Strange Collection type "+rawClass.getName()+": can not determine type parameters");
        }
        return CollectionType.construct(rawClass, typeParams[0]);
    }    

    protected JavaType _resolveVariableViaSubTypes(HierarchicType leafType, String variableName, TypeBindings bindings)
    {
        // can't resolve raw types; possible to have as-of-yet-unbound types too:
        if (leafType != null && leafType.isGeneric()) {
            TypeVariable<?>[] typeVariables = leafType.getRawClass().getTypeParameters();
            for (int i = 0, len = typeVariables.length; i < len; ++i) {
                TypeVariable<?> tv = typeVariables[i];
                if (variableName.equals(tv.getName())) {
                    // further resolution needed?
                    Type type = leafType.asGeneric().getActualTypeArguments()[i];
                    if (type instanceof TypeVariable<?>) {
                        return _resolveVariableViaSubTypes(leafType.getSubType(), ((TypeVariable<?>) type).getName(), bindings);
                    }
                    // no we're good for the variable (but it may have parameterization of its own)
                    return _constructType(type, bindings);
                }
            }
        }
        return _unknownType();
    }
    
    protected JavaType _unknownType() {
        return new SimpleType(Object.class);
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    /**
     * Helper method used to find inheritance (implements, extends) path
     * between given types, if one exists (caller generally checks before
     * calling this method). Returned type represents given <b>subtype</b>,
     * with supertype linkage extending to <b>supertype</b>.
     */
    protected HierarchicType  _findSuperTypeChain(Class<?> subtype, Class<?> supertype)
    {
        // If super-type is a class (not interface), bit simpler
        if (supertype.isInterface()) {
            return _findSuperInterfaceChain(subtype, supertype);
        }
        return _findSuperClassChain(subtype, supertype);
    }

    protected HierarchicType _findSuperClassChain(Type currentType, Class<?> target)
    {
        HierarchicType current = new HierarchicType(currentType);
        Class<?> raw = current.getRawClass();
        if (raw == target) {
            return current;
        }
        // Otherwise, keep on going down the rat hole...
        Type parent = raw.getGenericSuperclass();
        if (parent != null) {
            HierarchicType sup = _findSuperClassChain(parent, target);
            if (sup != null) {
                sup.setSubType(current);
                current.setSuperType(sup);
                return current;
            }
        }
        return null;
    }

    protected HierarchicType _findSuperInterfaceChain(Type currentType, Class<?> target)
    {
        HierarchicType current = new HierarchicType(currentType);
        Class<?> raw = current.getRawClass();
        if (raw == target) {
            return new HierarchicType(currentType);
        }
        // Otherwise, keep on going down the rat hole; first implemented interfaces
        /* 16-Aug-2011, tatu: Minor optimization based on profiled hot spot; let's
         *   try caching certain commonly needed cases
         */
        if (raw == HashMap.class) {
            if (target == Map.class) {
                return _hashMapSuperInterfaceChain(current);
            }
        }
        if (raw == ArrayList.class) {
            if (target == List.class) {
                return _arrayListSuperInterfaceChain(current);
            }
        }
        return _doFindSuperInterfaceChain(current, target);
    }
    
    protected HierarchicType _doFindSuperInterfaceChain(HierarchicType current, Class<?> target)
    {
        Class<?> raw = current.getRawClass();
        Type[] parents = raw.getGenericInterfaces();
        // as long as there are superclasses
        // and unless we have already seen the type (<T extends X<T>>)
        if (parents != null) {
            for (Type parent : parents) {
                HierarchicType sup = _findSuperInterfaceChain(parent, target);
                if (sup != null) {
                    sup.setSubType(current);
                    current.setSuperType(sup);
                    return current;
                }
            }
        }
        // and then super-class if any
        Type parent = raw.getGenericSuperclass();
        if (parent != null) {
            HierarchicType sup = _findSuperInterfaceChain(parent, target);
            if (sup != null) {
                sup.setSubType(current);
                current.setSuperType(sup);
                return current;
            }
        }
        return null;
    }

    protected synchronized HierarchicType _hashMapSuperInterfaceChain(HierarchicType current)
    {
        if (_cachedHashMapType == null) {
            HierarchicType base = current.deepCloneWithoutSubtype();
            _doFindSuperInterfaceChain(base, Map.class);
            _cachedHashMapType = base.getSuperType();
        }
        HierarchicType t = _cachedHashMapType.deepCloneWithoutSubtype();
        current.setSuperType(t);
        t.setSubType(current);
        return current;
    }

    protected synchronized HierarchicType _arrayListSuperInterfaceChain(HierarchicType current)
    {
        if (_cachedArrayListType == null) {
            HierarchicType base = current.deepCloneWithoutSubtype();
            _doFindSuperInterfaceChain(base, List.class);
            _cachedArrayListType = base.getSuperType();
        }
        HierarchicType t = _cachedArrayListType.deepCloneWithoutSubtype();
        current.setSuperType(t);
        t.setSubType(current);
        return current;
    }
}
