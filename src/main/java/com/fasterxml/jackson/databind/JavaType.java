package com.fasterxml.jackson.databind;

import java.lang.reflect.Modifier;
import java.util.List;

import com.fasterxml.jackson.core.type.ResolvedType;
import com.fasterxml.jackson.databind.type.TypeBindings;
import com.fasterxml.jackson.databind.type.TypeFactory;

/**
 * Base class for type token classes used both to contain information
 * and as keys for deserializers.
 *<p>
 * Instances can (only) be constructed by
 * <code>com.fasterxml.jackson.databind.type.TypeFactory</code>.
 *<p>
 * Since 2.2 this implements {@link java.lang.reflect.Type} to allow
 * it to be pushed through interfaces that only expose that type.
 */
public abstract class JavaType
    extends ResolvedType
    implements java.io.Serializable, // 2.1
        java.lang.reflect.Type // 2.2
{
    private static final long serialVersionUID = 1;

    /**
     * This is the nominal type-erased Class that would be close to the
     * type represented (but not exactly type, due to type erasure: type
     * instance may have more information on this).
     * May be an interface or abstract class, so instantiation
     * may not be possible.
     */
    protected final Class<?> _class;

    protected final int _hash;

    /**
     * Optional handler (codec) that can be attached to indicate 
     * what to use for handling (serializing, deserializing) values of
     * this specific type.
     *<p>
     * Note: untyped (i.e. caller has to cast) because it is used for
     * different kinds of handlers, with unrelated types.
     */
    protected final Object _valueHandler;

    /**
     * Optional handler that can be attached to indicate how to handle
     * additional type metadata associated with this type.
     *<p>
     * Note: untyped (i.e. caller has to cast) because it is used for
     * different kinds of handlers, with unrelated types.
     */
    protected final Object _typeHandler;

    /**
     * Whether entities defined with this type should be handled using
     * static typing (as opposed to dynamic runtime type) or not.
     * 
     * @since 2.2
     */
    protected final boolean _asStatic;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    /**
     * @param raw "Raw" (type-erased) class for this type
     * @param additionalHash Additional hash code to use, in addition
     *   to hash code of the class name 
     */
    protected JavaType(Class<?> raw, int additionalHash,
            Object valueHandler, Object typeHandler, boolean asStatic)
    {
        _class = raw;
        _hash = raw.getName().hashCode() + additionalHash;
        _valueHandler = valueHandler;
        _typeHandler = typeHandler;
        _asStatic = asStatic;
    }

    /**
     * Copy-constructor used when refining/upgrading type instances.
     *
     * @since 2.7
     */
    protected JavaType(JavaType base) 
    {
        _class = base._class;
        _hash = base._hash;
        _valueHandler = base._valueHandler;
        _typeHandler = base._typeHandler;
        _asStatic = base._asStatic;
    }

    /**
     * "Copy method" that will construct a new instance that is identical to
     * this instance, except that it will have specified type handler assigned.
     * 
     * @return Newly created type instance
     */
    public abstract JavaType withTypeHandler(Object h);

    /**
     * Mutant factory method that will construct a new instance that is identical to
     * this instance, except that it will have specified content type (element type
     * for arrays, value type for Maps and so forth) handler assigned.
     * 
     * @return Newly created type instance, with given 
     */
    public abstract JavaType withContentTypeHandler(Object h);

    /**
     * Mutant factory method that will construct a new instance that is identical to
     * this instance, except that it will have specified value handler assigned.
     * 
     * @return Newly created type instance
     */
    public abstract JavaType withValueHandler(Object h);

    /**
     * Mutant factory method that will construct a new instance that is identical to
     * this instance, except that it will have specified content value handler assigned.
     * 
     * @return Newly created type instance
     */
    public abstract JavaType withContentValueHandler(Object h);

    /**
     * Mutant factory method that may be called on structured types
     * that have a so-called content type (element of arrays, value type
     * of Maps, referenced type of referential types),
     * and will construct a new instance that is identical to
     * this instance, except that it has specified content type, instead of current
     * one. If content type is already set to given type, <code>this</code> is returned.
     * If type does not have a content type (which is the case with
     * <code>SimpleType</code>), {@link IllegalArgumentException}
     * will be thrown.
     * 
     * @return Newly created type instance
     *
     * @since 2.7
     */
    public abstract JavaType withContentType(JavaType contentType);

    /**
     * Method that can be called to get a type instance that indicates
     * that values of the type should be handled using "static typing" for purposes
     * of serialization (as opposed to "dynamic" aka runtime typing):
     * meaning that no runtime information is needed for determining serializers to use.
     * The main use case is to allow forcing of specific root value serialization type,
     * and specifically in resolving serializers for contained types (element types
     * for arrays, Collections and Maps).
     * 
     * @since 2.2
     */
    public abstract JavaType withStaticTyping();
    
    /*
    /**********************************************************
    /* Type coercion fluent factory methods
    /**********************************************************
     */

    /**
     * Mutant factory method that will try to create and return a sub-type instance
     * for known parameterized types; for other types will return `null` to indicate
     * that no just refinement makes necessary sense, without trying to detect
     * special status through implemented interfaces.
     *
     * @since 2.7
     */
    public abstract JavaType refine(Class<?> rawType, TypeBindings bindings,
            JavaType superClass, JavaType[] superInterfaces);
    
    /**
     * Legacy method used for forcing sub-typing of this type into
     * type specified by specific type erasure.
     * Deprecated as of 2.7 as such specializations really ought to
     * go through {@link TypeFactory}, not directly via {@link JavaType}.
     *
     * @since 2.7
     */
    @Deprecated
    public JavaType forcedNarrowBy(Class<?> subclass)
    {
        if (subclass == _class) { // can still optimize for simple case
            return this;
        }
        JavaType result = _narrow(subclass);
        // TODO: these checks should NOT actually be needed; above should suffice:
        if (_valueHandler != result.<Object>getValueHandler()) {
            result = result.withValueHandler(_valueHandler);
        }
        if (_typeHandler != result.<Object>getTypeHandler()) {
            result = result.withTypeHandler(_typeHandler);
        }
        return result;
    }

    @Deprecated // since 2.7
    protected abstract JavaType _narrow(Class<?> subclass);

    /*
    /**********************************************************
    /* Implementation of ResolvedType API
    /**********************************************************
     */

    @Override
    public final Class<?> getRawClass() { return _class; }

    /**
     * Method that can be used to check whether this type has
     * specified Class as its type erasure. Put another way, returns
     * true if instantiation of this Type is given (type-erased) Class.
     */
    @Override
    public final boolean hasRawClass(Class<?> clz) { return _class == clz; }

    /**
     * @since 2.6
     */
    public final boolean isTypeOrSubTypeOf(Class<?> clz) {
        return (_class == clz) || (clz.isAssignableFrom(_class));
    }

    @Override
    public boolean isAbstract() {
        return Modifier.isAbstract(_class.getModifiers());
    }

    /**
     * Convenience method for checking whether underlying Java type
     * is a concrete class or not: abstract classes and interfaces
     * are not.
     */
    @Override
    public boolean isConcrete() {
        int mod = _class.getModifiers();
        if ((mod & (Modifier.INTERFACE | Modifier.ABSTRACT)) == 0) {
            return true;
        }
        /* 19-Feb-2010, tatus: Holy mackarel; primitive types
         *    have 'abstract' flag set...
         */
        return _class.isPrimitive();
    }

    @Override
    public boolean isThrowable() { return Throwable.class.isAssignableFrom(_class); }

    @Override
    public boolean isArrayType() { return false; }

    @Override
    public final boolean isEnumType() { return _class.isEnum(); }

    @Override
    public final boolean isInterface() { return _class.isInterface(); }

    @Override
    public final boolean isPrimitive() { return _class.isPrimitive(); }

    @Override
    public final boolean isFinal() { return Modifier.isFinal(_class.getModifiers()); }

    /**
     * @return True if type represented is a container type; this includes
     *    array, Map and Collection types.
     */
    @Override
    public abstract boolean isContainerType();

    /**
     * @return True if type is either true {@link java.util.Collection} type,
     *    or something similar (meaning it has at least one type parameter,
     *    which describes type of contents)
     */
    @Override
    public boolean isCollectionLikeType() { return false; }

    /**
     * @return True if type is either true {@link java.util.Map} type,
     *    or something similar (meaning it has at least two type parameter;
     *    first one describing key type, second value type)
     */
    @Override
    public boolean isMapLikeType() { return false; }

    /**
     * Convenience method, short-hand for
     *<code>
     *   getRawClass() == Object.class
     *</code>
     * and used to figure if we basically have "untyped" type object.
     *
     * @since 2.5
     */
    public final boolean isJavaLangObject() { return _class == Object.class; }

    /**
     * Accessor for checking whether handlers for dealing with values of
     * this type should use static typing (as opposed to dynamic typing).
     * Note that while value of 'true' does mean that static typing is to
     * be used, value of 'false' may still be overridden by other settings.
     * 
     * @since 2.2
     */
    public final boolean useStaticType() { return _asStatic; }

    /*
    /**********************************************************
    /* Public API, type parameter access; pass-through
    /**********************************************************
     */

    @Override
    public boolean hasGenericTypes() { return containedTypeCount() > 0; }

    @Override
    public JavaType getKeyType() { return null; }

    @Override
    public JavaType getContentType() { return null; }

    @Override // since 2.6
    public JavaType getReferencedType() { return null; }

    @Override
    public abstract int containedTypeCount();

    @Override
    public abstract JavaType containedType(int index);
       
    @Deprecated // since 2.7
    @Override
    public abstract String containedTypeName(int index);

    @Deprecated // since 2.7
    @Override
    public Class<?> getParameterSource() {
        return null;
    }

    /*
    /**********************************************************
    /* Extended API beyond ResolvedType
    /**********************************************************
     */
    
    // NOTE: not defined in Resolved type
    /**
     * Convenience method that is functionally same as:
     *<code>
     * JavaType t = containedType(index);
     * if (t == null) {
     *    t = TypeFactory.unknownType();
     * }
     *</code>
     * and typically used to eliminate need for null checks for common case
     * where we just want to check if containedType is available first; and
     * if not, use "unknown type" (which translates to <code>java.lang.Object</code>
     * basically).
     *
     * @since 2.5
     */
    public JavaType containedTypeOrUnknown(int index) {
        JavaType t = containedType(index);
        return (t == null)  ? TypeFactory.unknownType() : t;
    }

    /**
     * @since 2.7
     */
    public abstract TypeBindings getBindings();

    /**
     * Method that may be called to find representation of given type
     * within type hierarchy of this type: either this type (if this
     * type has given erased type), one of its supertypes that has the
     * erased types, or null if target is neither this type or any of its
     * supertypes.
     *
     * @since 2.7
     */
    public abstract JavaType findSuperType(Class<?> erasedTarget);

    /**
     * Accessor for finding fully resolved parent class of this type,
     * if it has one; null if not.
     *
     * @since 2.7
     */
    public abstract JavaType getSuperClass();

    /**
     * Accessor for finding fully resolved interfaces this type implements,
     * if any; empty array if none.
     *
     * @since 2.7
     */
    public abstract List<JavaType> getInterfaces();

    /**
     * Method that may be used to find paramaterization this type has for
     * given type-erased generic target type.
     *
     * @since 2.7
     */
    public abstract JavaType[] findTypeParameters(Class<?> expType);

    /*
    /**********************************************************
    /* Semi-public API, accessing handlers
    /**********************************************************
     */
    
    /**
     * Method for accessing value handler associated with this type, if any
     */
    @SuppressWarnings("unchecked")
    public <T> T getValueHandler() { return (T) _valueHandler; }

    /**
     * Method for accessing type handler associated with this type, if any
     */
    @SuppressWarnings("unchecked")
    public <T> T getTypeHandler() { return (T) _typeHandler; }

    /**
     * @since 2.7
     */
    public Object getContentValueHandler() { return null; }

    /**
     * @since 2.7
     */
    public Object getContentTypeHandler() { return null; }    

    /**
     * @since 2.6
     */
    public boolean hasValueHandler() { return _valueHandler != null; }

    /*
    /**********************************************************
    /* Support for producing signatures
    /**********************************************************
     */
    
    //public abstract String toCanonical();

    /**
     * Method for accessing signature that contains generic
     * type information, in form compatible with JVM 1.5
     * as per JLS. It is a superset of {@link #getErasedSignature},
     * in that generic information can be automatically removed
     * if necessary (just remove outermost
     * angle brackets along with content inside)
     */
    public String getGenericSignature() {
        StringBuilder sb = new StringBuilder(40);
        getGenericSignature(sb);
        return sb.toString();        
    }

    /**
     * 
     * @param sb StringBuilder to append signature to
     * 
     * @return StringBuilder that was passed in; returned to allow
     * call chaining
     */
    public abstract StringBuilder getGenericSignature(StringBuilder sb);
    
    /**
     * Method for accessing signature without generic
     * type information, in form compatible with all versions
     * of JVM, and specifically used for type descriptions
     * when generating byte code.
     */
    public String getErasedSignature() {
        StringBuilder sb = new StringBuilder(40);
        getErasedSignature(sb);
        return sb.toString();
    }

    /**
     * Method for accessing signature without generic
     * type information, in form compatible with all versions
     * of JVM, and specifically used for type descriptions
     * when generating byte code.
     * 
     * @param sb StringBuilder to append signature to
     * 
     * @return StringBuilder that was passed in; returned to allow
     * call chaining
     */
    public abstract StringBuilder getErasedSignature(StringBuilder sb);

    /*
    /**********************************************************
    /* Standard methods; let's make them abstract to force override
    /**********************************************************
     */

    @Override
    public abstract String toString();

    @Override
    public abstract boolean equals(Object o);

    @Override
    public final int hashCode() { return _hash; }
}
