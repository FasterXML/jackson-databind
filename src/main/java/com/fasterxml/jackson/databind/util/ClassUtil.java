package com.fasterxml.jackson.databind.util;

import java.io.Closeable;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;

public final class ClassUtil
{
    private final static Class<?> CLS_OBJECT = Object.class;

    private final static Annotation[] NO_ANNOTATIONS = new Annotation[0];
    private final static Ctor[] NO_CTORS = new Ctor[0];

    private final static Iterator<?> EMPTY_ITERATOR = Collections.emptyIterator();

    /*
    /**********************************************************
    /* Simple factory methods
    /**********************************************************
     */

    /**
     * @since 2.7
     */
    @SuppressWarnings("unchecked")
    public static <T> Iterator<T> emptyIterator() {
        return (Iterator<T>) EMPTY_ITERATOR;
    }

    /*
    /**********************************************************
    /* Methods that deal with inheritance
    /**********************************************************
     */

    /**
     * Method that will find all sub-classes and implemented interfaces
     * of a given class or interface. Classes are listed in order of
     * precedence, starting with the immediate super-class, followed by
     * interfaces class directly declares to implemented, and then recursively
     * followed by parent of super-class and so forth.
     * Note that <code>Object.class</code> is not included in the list
     * regardless of whether <code>endBefore</code> argument is defined or not.
     *
     * @param endBefore Super-type to NOT include in results, if any; when
     *    encountered, will be ignored (and no super types are checked).
     *
     * @since 2.7
     */
    public static List<JavaType> findSuperTypes(JavaType type, Class<?> endBefore,
            boolean addClassItself) {
        if ((type == null) || type.hasRawClass(endBefore) || type.hasRawClass(Object.class)) {
            return Collections.emptyList();
        }
        List<JavaType> result = new ArrayList<JavaType>(8);
        _addSuperTypes(type, endBefore, result, addClassItself);
        return result;
    }

    /**
     * @since 2.7
     */
    public static List<Class<?>> findRawSuperTypes(Class<?> cls, Class<?> endBefore, boolean addClassItself) {
        if ((cls == null) || (cls == endBefore) || (cls == Object.class)) {
            return Collections.emptyList();
        }
        List<Class<?>> result = new ArrayList<Class<?>>(8);
        _addRawSuperTypes(cls, endBefore, result, addClassItself);
        return result;
    }

    /**
     * Method for finding all super classes (but not super interfaces) of given class,
     * starting with the immediate super class and ending in the most distant one.
     * Class itself is included if <code>addClassItself</code> is true.
     *
     * @since 2.7
     */
    public static List<Class<?>> findSuperClasses(Class<?> cls, Class<?> endBefore,
            boolean addClassItself) {
        List<Class<?>> result = new LinkedList<Class<?>>();
        if ((cls != null) && (cls != endBefore))  {
            if (addClassItself) {
                result.add(cls);
            }
            while ((cls = cls.getSuperclass()) != null) {
                if (cls == endBefore) {
                    break;
                }
                result.add(cls);
            }
        }
        return result;
    }

    @Deprecated // since 2.7
    public static List<Class<?>> findSuperTypes(Class<?> cls, Class<?> endBefore) {
        return findSuperTypes(cls, endBefore, new ArrayList<Class<?>>(8));
    }

    @Deprecated // since 2.7
    public static List<Class<?>> findSuperTypes(Class<?> cls, Class<?> endBefore, List<Class<?>> result) {
        _addRawSuperTypes(cls, endBefore, result, false);
        return result;
    }

    private static void _addSuperTypes(JavaType type, Class<?> endBefore, Collection<JavaType> result,
            boolean addClassItself)
    {
        if (type == null) {
            return;
        }
        final Class<?> cls = type.getRawClass();
        if (cls == endBefore || cls == Object.class) { return; }
        if (addClassItself) {
            if (result.contains(type)) { // already added, no need to check supers
                return;
            }
            result.add(type);
        }
        for (JavaType intCls : type.getInterfaces()) {
            _addSuperTypes(intCls, endBefore, result, true);
        }
        _addSuperTypes(type.getSuperClass(), endBefore, result, true);
    }

    private static void _addRawSuperTypes(Class<?> cls, Class<?> endBefore, Collection<Class<?>> result, boolean addClassItself) {
        if (cls == endBefore || cls == null || cls == Object.class) { return; }
        if (addClassItself) {
            if (result.contains(cls)) { // already added, no need to check supers
                return;
            }
            result.add(cls);
        }
        for (Class<?> intCls : _interfaces(cls)) {
            _addRawSuperTypes(intCls, endBefore, result, true);
        }
        _addRawSuperTypes(cls.getSuperclass(), endBefore, result, true);
    }

    /*
    /**********************************************************
    /* Class type detection methods
    /**********************************************************
     */

    /**
     * @return Null if class might be a bean; type String (that identifies
     *   why it's not a bean) if not
     */
    public static String canBeABeanType(Class<?> type)
    {
        // First: language constructs that ain't beans:
        if (type.isAnnotation()) {
            return "annotation";
        }
        if (type.isArray()) {
            return "array";
        }
        if (type.isEnum()) {
            return "enum";
        }
        if (type.isPrimitive()) {
            return "primitive";
        }

        // Anything else? Seems valid, then
        return null;
    }
    
    public static String isLocalType(Class<?> type, boolean allowNonStatic)
    {
        /* As per [JACKSON-187], GAE seems to throw SecurityExceptions
         * here and there... and GAE itself has a bug, too
         * (see []). Bah. So we need to catch some wayward exceptions on GAE
         */
        try {
            // one more: method locals, anonymous, are not good:
            if (hasEnclosingMethod(type)) {
                return "local/anonymous";
            }
            
            /* But how about non-static inner classes? Can't construct
             * easily (theoretically, we could try to check if parent
             * happens to be enclosing... but that gets convoluted)
             */
            if (!allowNonStatic) {
                if (!Modifier.isStatic(type.getModifiers())) {
                    if (getEnclosingClass(type) != null) {
                        return "non-static member class";
                    }
                }
            }
        }
        catch (SecurityException e) { }
        catch (NullPointerException e) { }
        return null;
    }

    /**
     * Method for finding enclosing class for non-static inner classes
     */
    public static Class<?> getOuterClass(Class<?> type)
    {
        // as above, GAE has some issues...
        try {
            // one more: method locals, anonymous, are not good:
            if (hasEnclosingMethod(type)) {
                return null;
            }
            if (!Modifier.isStatic(type.getModifiers())) {
                return getEnclosingClass(type);
            }
        } catch (SecurityException e) { }
        return null;
    }
    
    
    /**
     * Helper method used to weed out dynamic Proxy types; types that do
     * not expose concrete method API that we could use to figure out
     * automatic Bean (property) based serialization.
     */
    public static boolean isProxyType(Class<?> type)
    {
        // As per [databind#57], should NOT disqualify JDK proxy:
        /*
        // Then: well-known proxy (etc) classes
        if (Proxy.isProxyClass(type)) {
            return true;
        }
        */
        String name = type.getName();
        // Hibernate uses proxies heavily as well:
        if (name.startsWith("net.sf.cglib.proxy.")
            || name.startsWith("org.hibernate.proxy.")) {
            return true;
        }
        // Not one of known proxies, nope:
        return false;
    }

    /**
     * Helper method that checks if given class is a concrete one;
     * that is, not an interface or abstract class.
     */
    public static boolean isConcrete(Class<?> type)
    {
        int mod = type.getModifiers();
        return (mod & (Modifier.INTERFACE | Modifier.ABSTRACT)) == 0;
    }

    public static boolean isConcrete(Member member)
    {
        int mod = member.getModifiers();
        return (mod & (Modifier.INTERFACE | Modifier.ABSTRACT)) == 0;
    }
    
    public static boolean isCollectionMapOrArray(Class<?> type)
    {
        if (type.isArray()) return true;
        if (Collection.class.isAssignableFrom(type)) return true;
        if (Map.class.isAssignableFrom(type)) return true;
        return false;
    }

    public static boolean isBogusClass(Class<?> cls) {
        return (cls == Void.class || cls == Void.TYPE
                || cls == com.fasterxml.jackson.databind.annotation.NoClass.class);
    }

    public static boolean isNonStaticInnerClass(Class<?> cls) {
        return !Modifier.isStatic(cls.getModifiers())
                && (getEnclosingClass(cls) != null);
    }

    /**
     * @since 2.7
     */
    public static boolean isObjectOrPrimitive(Class<?> cls) {
        return (cls == CLS_OBJECT) || cls.isPrimitive();
    }

    /**
     * @since 2.9
     */
    public static boolean hasClass(Object inst, Class<?> raw) {
        // 10-Nov-2016, tatu: Could use `Class.isInstance()` if we didn't care
        //    about being exactly that type
        return (inst != null) && (inst.getClass() == raw);
    }

    /**
     * @since 2.9
     */
    public static void verifyMustOverride(Class<?> expType, Object instance,
            String method)
    {
        if (instance.getClass() != expType) {
            throw new IllegalStateException(String.format(
                    "Sub-class %s (of class %s) must override method '%s'",
                instance.getClass().getName(), expType.getName(), method));
        }
    }

    /*
    /**********************************************************
    /* Method type detection methods
    /**********************************************************
     */

    /**
     * @deprecated Since 2.6 not used; may be removed before 3.x
     */
    @Deprecated // since 2.6
    public static boolean hasGetterSignature(Method m)
    {
        // First: static methods can't be getters
        if (Modifier.isStatic(m.getModifiers())) {
            return false;
        }
        // Must take no args
        Class<?>[] pts = m.getParameterTypes();
        if (pts != null && pts.length != 0) {
            return false;
        }
        // Can't be a void method
        if (Void.TYPE == m.getReturnType()) {
            return false;
        }
        // Otherwise looks ok:
        return true;
    }

    /*
    /**********************************************************
    /* Exception handling; simple re-throw
    /**********************************************************
     */

    /**
     * Helper method that will check if argument is an {@link Error},
     * and if so, (re)throw it; otherwise just return
     *
     * @since 2.9
     */
    public static Throwable throwIfError(Throwable t) {
        if (t instanceof Error) {
            throw (Error) t;
        }
        return t;
    }

    /**
     * Helper method that will check if argument is an {@link RuntimeException},
     * and if so, (re)throw it; otherwise just return
     *
     * @since 2.9
     */
    public static Throwable throwIfRTE(Throwable t) {
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        return t;
    }

    /**
     * Helper method that will check if argument is an {@link IOException},
     * and if so, (re)throw it; otherwise just return
     *
     * @since 2.9
     */
    public static Throwable throwIfIOE(Throwable t) throws IOException {
        if (t instanceof IOException) {
            throw (IOException) t;
        }
        return t;
    }

    /*
    /**********************************************************
    /* Exception handling; other
    /**********************************************************
     */
    
    /**
     * Method that can be used to find the "root cause", innermost
     * of chained (wrapped) exceptions.
     */
    public static Throwable getRootCause(Throwable t)
    {
        while (t.getCause() != null) {
            t = t.getCause();
        }
        return t;
    }

    /**
     * Method that works like by calling {@link #getRootCause} and then
     * either throwing it (if instanceof {@link IOException}), or
     * return.
     *
     * @since 2.8
     */
    public static Throwable throwRootCauseIfIOE(Throwable t) throws IOException {
        return throwIfIOE(getRootCause(t));
    }

    /**
     * Method that will wrap 't' as an {@link IllegalArgumentException} if it
     * is a checked exception; otherwise (runtime exception or error) throw as is
     */
    public static void throwAsIAE(Throwable t) {
        throwAsIAE(t, t.getMessage());
    }

    /**
     * Method that will wrap 't' as an {@link IllegalArgumentException} (and with
     * specified message) if it
     * is a checked exception; otherwise (runtime exception or error) throw as is
     */
    public static void throwAsIAE(Throwable t, String msg)
    {
        throwIfRTE(t);
        throwIfError(t);
        throw new IllegalArgumentException(msg, t);
    }

    /**
     * @since 2.9
     */
    public static <T> T throwAsMappingException(DeserializationContext ctxt,
            IOException e0) throws JsonMappingException {
        if (e0 instanceof JsonMappingException) {
            throw (JsonMappingException) e0;
        }
        JsonMappingException e = JsonMappingException.from(ctxt, e0.getMessage());
        e.initCause(e0);
        throw e;
    }

    /**
     * Method that will locate the innermost exception for given Throwable;
     * and then wrap it as an {@link IllegalArgumentException} if it
     * is a checked exception; otherwise (runtime exception or error) throw as is
     */
    public static void unwrapAndThrowAsIAE(Throwable t)
    {
        throwAsIAE(getRootCause(t));
    }

    /**
     * Method that will locate the innermost exception for given Throwable;
     * and then wrap it as an {@link IllegalArgumentException} if it
     * is a checked exception; otherwise (runtime exception or error) throw as is
     */
    public static void unwrapAndThrowAsIAE(Throwable t, String msg)
    {
        throwAsIAE(getRootCause(t), msg);
    }

    /**
     * Helper method that encapsulate logic in trying to close output generator
     * in case of failure; useful mostly in forcing flush()ing as otherwise
     * error conditions tend to be hard to diagnose. However, it is often the
     * case that output state may be corrupt so we need to be prepared for
     * secondary exception without masking original one.
     *
     * @since 2.8
     */
    public static void closeOnFailAndThrowAsIAE(JsonGenerator g, Exception fail)
            throws IOException
    {
        /* 04-Mar-2014, tatu: Let's try to prevent auto-closing of
         *    structures, which typically causes more damage.
         */
        g.disable(JsonGenerator.Feature.AUTO_CLOSE_JSON_CONTENT);
        try {
            g.close();
        } catch (Exception e) {
            fail.addSuppressed(e);
        }
        throwIfIOE(fail);
        throwIfRTE(fail);
        throw new RuntimeException(fail);
    }

    /**
     * Helper method that encapsulate logic in trying to close given {@link Closeable}
     * in case of failure; useful mostly in forcing flush()ing as otherwise
     * error conditions tend to be hard to diagnose. However, it is often the
     * case that output state may be corrupt so we need to be prepared for
     * secondary exception without masking original one.
     *
     * @since 2.8
     */
    public static void closeOnFailAndThrowAsIAE(JsonGenerator g,
            Closeable toClose, Exception fail)
        throws IOException
    {
        if (g != null) {
            g.disable(JsonGenerator.Feature.AUTO_CLOSE_JSON_CONTENT);
            try {
                g.close();
            } catch (Exception e) {
                fail.addSuppressed(e);
            }
        }
        if (toClose != null) {
            try {
                toClose.close();
            } catch (Exception e) {
                fail.addSuppressed(e);
            }
        }
        throwIfIOE(fail);
        throwIfRTE(fail);
        throw new RuntimeException(fail);
    }

    /*
    /**********************************************************
    /* Instantiation
    /**********************************************************
     */

    /**
     * Method that can be called to try to create an instantiate of
     * specified type. Instantiation is done using default no-argument
     * constructor.
     *
     * @param canFixAccess Whether it is possible to try to change access
     *   rights of the default constructor (in case it is not publicly
     *   accessible) or not.
     *
     * @throws IllegalArgumentException If instantiation fails for any reason;
     *    except for cases where constructor throws an unchecked exception
     *    (which will be passed as is)
     */
    public static <T> T createInstance(Class<T> cls, boolean canFixAccess)
        throws IllegalArgumentException
    {
        Constructor<T> ctor = findConstructor(cls, canFixAccess);
        if (ctor == null) {
            throw new IllegalArgumentException("Class "+cls.getName()+" has no default (no arg) constructor");
        }
        try {
            return ctor.newInstance();
        } catch (Exception e) {
            ClassUtil.unwrapAndThrowAsIAE(e, "Failed to instantiate class "+cls.getName()+", problem: "+e.getMessage());
            return null;
        }
    }

    public static <T> Constructor<T> findConstructor(Class<T> cls, boolean forceAccess)
        throws IllegalArgumentException
    {
        try {
            Constructor<T> ctor = cls.getDeclaredConstructor();
            if (forceAccess) {
                checkAndFixAccess(ctor, forceAccess);
            } else {
                // Has to be public...
                if (!Modifier.isPublic(ctor.getModifiers())) {
                    throw new IllegalArgumentException("Default constructor for "+cls.getName()+" is not accessible (non-public?): not allowed to try modify access via Reflection: can not instantiate type");
                }
            }
            return ctor;
        } catch (NoSuchMethodException e) {
            ;
        } catch (Exception e) {
            ClassUtil.unwrapAndThrowAsIAE(e, "Failed to find default constructor of class "+cls.getName()+", problem: "+e.getMessage());
        }
        return null;
    }

    /*
    /**********************************************************
    /* Class name, description access
    /**********************************************************
     */

    /**
     * @since 2.9
     */
    public static Class<?> classOf(Object inst) {
        if (inst == null) {
            return null;
        }
        return inst.getClass();
    }
    
    /**
     * @since 2.9
     */
    public static <T> T nonNull(T valueOrNull, T defaultValue) {
        return (valueOrNull == null) ? defaultValue : valueOrNull;
    }

    /**
     * @since 2.9
     */
    public static String nullOrToString(Object value) {
        if (value == null) {
            return null;
        }
        return value.toString();
    }

    /**
     * @since 2.9
     */
    public static String nonNullString(String str) {
        if (str == null) {
            return "";
        }
        return str;
    }

    /**
     * Returns either quoted value (with double-quotes) -- if argument non-null
     * String -- or String NULL (no quotes) (if null).
     *
     * @since 2.9
     */
    public static String quotedOr(Object str, String forNull) {
        if (str == null) {
            return forNull;
        }
        return String.format("\"%s\"", str);
    }

    /*
    /**********************************************************
    /* Type name handling methods
    /**********************************************************
     */
    
    /**
     * Helper method used to construct appropriate description
     * when passed either type (Class) or an instance; in latter
     * case, class of instance is to be used.
     */
    public static String getClassDescription(Object classOrInstance)
    {
        if (classOrInstance == null) {
            return "unknown";
        }
        Class<?> cls = (classOrInstance instanceof Class<?>) ?
            (Class<?>) classOrInstance : classOrInstance.getClass();
        return cls.getName();
    }
    
    /**
     * @since 2.9
     */
    public static String classNameOf(Object inst) {
        if (inst == null) {
            return "[null]";
        }
        return inst.getClass().getName();
    }

    /**
     * Returns either `cls.getName()` (if `cls` not null),
     * or "[null]" if `cls` is null.
     *
     * @since 2.9
     */
    public static String nameOf(Class<?> cls) {
        if (cls == null) {
            return "[null]";
        }
        if (cls.isArray()) {
            return nameOf(cls.getComponentType())+"[]";
        }
        if (cls.isPrimitive()) {
            cls.getSimpleName();
        }
        return cls.getName();
    }
    
    /**
     * Returns either (double-)quoted `named.getName()` (if `named` not null),
     * or "[null]" if `named` is null.
     *
     * @since 2.9
     */
    public static String nameOf(Named named) {
        if (named == null) {
            return "[null]";
        }
        return String.format("'%s'", named.getName());
    }

    /*
    /**********************************************************
    /* Primitive type support
    /**********************************************************
     */
    
    /**
     * Helper method used to get default value for wrappers used for primitive types
     * (0 for Integer etc)
     */
    public static Object defaultValue(Class<?> cls)
    {
        if (cls == Integer.TYPE) {
            return Integer.valueOf(0);
        }
        if (cls == Long.TYPE) {
            return Long.valueOf(0L);
        }
        if (cls == Boolean.TYPE) {
            return Boolean.FALSE;
        }
        if (cls == Double.TYPE) {
            return Double.valueOf(0.0);
        }
        if (cls == Float.TYPE) {
            return Float.valueOf(0.0f);
        }
        if (cls == Byte.TYPE) {
            return Byte.valueOf((byte) 0);
        }
        if (cls == Short.TYPE) {
            return Short.valueOf((short) 0);
        }
        if (cls == Character.TYPE) {
            return '\0';
        }
        throw new IllegalArgumentException("Class "+cls.getName()+" is not a primitive type");
    }

    /**
     * Helper method for finding wrapper type for given primitive type (why isn't
     * there one in JDK?)
     */
    public static Class<?> wrapperType(Class<?> primitiveType)
    {
        if (primitiveType == Integer.TYPE) {
            return Integer.class;
        }
        if (primitiveType == Long.TYPE) {
            return Long.class;
        }
        if (primitiveType == Boolean.TYPE) {
            return Boolean.class;
        }
        if (primitiveType == Double.TYPE) {
            return Double.class;
        }
        if (primitiveType == Float.TYPE) {
            return Float.class;
        }
        if (primitiveType == Byte.TYPE) {
            return Byte.class;
        }
        if (primitiveType == Short.TYPE) {
            return Short.class;
        }
        if (primitiveType == Character.TYPE) {
            return Character.class;
        }
        throw new IllegalArgumentException("Class "+primitiveType.getName()+" is not a primitive type");
    }

    /**
     * Method that can be used to find primitive type for given class if (but only if)
     * it is either wrapper type or primitive type; returns `null` if type is neither.
     *
     * @since 2.7
     */
    public static Class<?> primitiveType(Class<?> type)
    {
        if (type.isPrimitive()) {
            return type;
        }
        
        if (type == Integer.class) {
            return Integer.TYPE;
        }
        if (type == Long.class) {
            return Long.TYPE;
        }
        if (type == Boolean.class) {
            return Boolean.TYPE;
        }
        if (type == Double.class) {
            return Double.TYPE;
        }
        if (type == Float.class) {
            return Float.TYPE;
        }
        if (type == Byte.class) {
            return Byte.TYPE;
        }
        if (type == Short.class) {
            return Short.TYPE;
        }
        if (type == Character.class) {
            return Character.TYPE;
        }
        return null;
    }

    /*
    /**********************************************************
    /* Access checking/handling methods
    /**********************************************************
     */

    /**
     * Equivalent to call:
     *<pre>
     *   checkAndFixAccess(member, false);
     *</pre>
     *
     * @deprecated Since 2.7 call variant that takes boolean flag.
     */
    @Deprecated
    public static void checkAndFixAccess(Member member) {
        checkAndFixAccess(member, false);
    }

    /**
     * Method that is called if a {@link Member} may need forced access,
     * to force a field, method or constructor to be accessible: this
     * is done by calling {@link AccessibleObject#setAccessible(boolean)}.
     * 
     * @param member Accessor to call <code>setAccessible()</code> on.
     * @param force Whether to always try to make accessor accessible (true),
     *   or only if needed as per access rights (false)
     *
     * @since 2.7
     */
    public static void checkAndFixAccess(Member member, boolean force)
    {
        // We know all members are also accessible objects...
        AccessibleObject ao = (AccessibleObject) member;

        /* 14-Jan-2009, tatu: It seems safe and potentially beneficial to
         *   always to make it accessible (latter because it will force
         *   skipping checks we have no use for...), so let's always call it.
         */
        try {
            if (force || 
                    (!Modifier.isPublic(member.getModifiers())
                            || !Modifier.isPublic(member.getDeclaringClass().getModifiers()))) {
                ao.setAccessible(true);
            }
        } catch (SecurityException se) {
            // 17-Apr-2009, tatu: Related to [JACKSON-101]: this can fail on platforms like
            // Google App Engine); so let's only fail if we really needed it...
            if (!ao.isAccessible()) {
                Class<?> declClass = member.getDeclaringClass();
                throw new IllegalArgumentException("Can not access "+member+" (from class "+declClass.getName()+"; failed to set access: "+se.getMessage());
            }
        }
    }

    /*
    /**********************************************************
    /* Enum type detection
    /**********************************************************
     */

    /**
     * Helper method that can be used to dynamically figure out
     * enumeration type of given {@link EnumSet}, without having
     * access to its declaration.
     * Code is needed to work around design flaw in JDK.
     */
    public static Class<? extends Enum<?>> findEnumType(EnumSet<?> s)
    {
        // First things first: if not empty, easy to determine
        if (!s.isEmpty()) {
            return findEnumType(s.iterator().next());
        }
        // Otherwise need to locate using an internal field
        return EnumTypeLocator.instance.enumTypeFor(s);
    }

    /**
     * Helper method that can be used to dynamically figure out
     * enumeration type of given {@link EnumSet}, without having
     * access to its declaration.
     * Code is needed to work around design flaw in JDK.
     */
    public static Class<? extends Enum<?>> findEnumType(EnumMap<?,?> m)
    {
        if (!m.isEmpty()) {
            return findEnumType(m.keySet().iterator().next());
        }
        // Otherwise need to locate using an internal field
        return EnumTypeLocator.instance.enumTypeFor(m);
    }

    /**
     * Helper method that can be used to dynamically figure out formal
     * enumeration type (class) for given enumeration. This is either
     * class of enum instance (for "simple" enumerations), or its
     * superclass (for enums with instance fields or methods)
     */
    @SuppressWarnings("unchecked")
    public static Class<? extends Enum<?>> findEnumType(Enum<?> en)
    {
        // enums with "body" are sub-classes of the formal type
    	Class<?> ec = en.getClass();
    	if (ec.getSuperclass() != Enum.class) {
    	    ec = ec.getSuperclass();
    	}
    	return (Class<? extends Enum<?>>) ec;
    }

    /**
     * Helper method that can be used to dynamically figure out formal
     * enumeration type (class) for given class of an enumeration value.
     * This is either class of enum instance (for "simple" enumerations),
     * or its superclass (for enums with instance fields or methods)
     */
    @SuppressWarnings("unchecked")
    public static Class<? extends Enum<?>> findEnumType(Class<?> cls)
    {
        // enums with "body" are sub-classes of the formal type
        if (cls.getSuperclass() != Enum.class) {
            cls = cls.getSuperclass();
        }
        return (Class<? extends Enum<?>>) cls;
    }

    /**
     * A method that will look for the first Enum value annotated with the given Annotation.
     * <p>
     * If there's more than one value annotated, the first one found will be returned. Which one exactly is used is undetermined.
     *
     * @param enumClass The Enum class to scan for a value with the given annotation
     * @param annotationClass The annotation to look for.
     * @return the Enum value annotated with the given Annotation or {@code null} if none is found.
     * @throws IllegalArgumentException if there's a reflection issue accessing the Enum
     * @since 2.8
     */
    public static <T extends Annotation> Enum<?> findFirstAnnotatedEnumValue(Class<Enum<?>> enumClass, Class<T> annotationClass)
    {
        Field[] fields = getDeclaredFields(enumClass);
        for (Field field : fields) {
            if (field.isEnumConstant()) {
                Annotation defaultValueAnnotation = field.getAnnotation(annotationClass);
                if (defaultValueAnnotation != null) {
                    final String name = field.getName();
                    for (Enum<?> enumValue : enumClass.getEnumConstants()) {
                        if (name.equals(enumValue.name())) {
                            return enumValue;
                        }
                    }
                }
            }
        }
        return null;
    }

    /*
    /**********************************************************
    /* Jackson-specific stuff
    /**********************************************************
     */

    /**
     * Method that can be called to determine if given Object is the default
     * implementation Jackson uses; as opposed to a custom serializer installed by
     * a module or calling application. Determination is done using
     * {@link JacksonStdImpl} annotation on handler (serializer, deserializer etc)
     * class.
     *<p>
     * NOTE: passing `null` is legal, and will result in <code>true</code>
     * being returned.
     */
    public static boolean isJacksonStdImpl(Object impl) {
        return (impl == null) || isJacksonStdImpl(impl.getClass());
    }

    public static boolean isJacksonStdImpl(Class<?> implClass) {
        return (implClass.getAnnotation(JacksonStdImpl.class) != null);
    }

    /*
    /**********************************************************
    /* Access to various Class definition aspects; possibly
    /* cacheable; and attempts was made in 2.7.0 - 2.7.7; however
    /* unintented retention (~= memory leak) wrt [databind#1363]
    /* resulted in removal of caching
    /**********************************************************
     */

    /**
     * @since 2.7
     */
    public static String getPackageName(Class<?> cls) {
        Package pkg = cls.getPackage();
        return (pkg == null) ? null : pkg.getName();
    }

    /**
     * @since 2.7
     */
    public static boolean hasEnclosingMethod(Class<?> cls) {
        return !isObjectOrPrimitive(cls) && (cls.getEnclosingMethod() != null);
    }

    /**
     * @since 2.7
     */
    public static Field[] getDeclaredFields(Class<?> cls) {
        return cls.getDeclaredFields();
    }

    /**
     * @since 2.7
     */
    public static Method[] getDeclaredMethods(Class<?> cls) {
        return cls.getDeclaredMethods();
    }

    /**
     * @since 2.7
     */
    public static Annotation[] findClassAnnotations(Class<?> cls) {
        if (isObjectOrPrimitive(cls)) {
            return NO_ANNOTATIONS;
        }
        return cls.getDeclaredAnnotations();
    }

    /**
     * Helper method that gets methods declared in given class; usually a simple thing,
     * but sometimes (as per [databind#785]) more complicated, depending on classloader
     * setup.
     *
     * @since 2.9
     */
    public static Method[] getClassMethods(Class<?> cls)
    {
        try {
            return ClassUtil.getDeclaredMethods(cls);
        } catch (final NoClassDefFoundError ex) {
            // One of the methods had a class that was not found in the cls.getClassLoader.
            // Maybe the developer was nice and has a different class loader for this context.
            final ClassLoader loader = Thread.currentThread().getContextClassLoader();
            if (loader == null){
                // Nope... this is going to end poorly
                throw ex;
            }
            final Class<?> contextClass;
            try {
                contextClass = loader.loadClass(cls.getName());
            } catch (ClassNotFoundException e) {
                ex.addSuppressed(e);
                throw ex;
            }
            return contextClass.getDeclaredMethods(); // Cross fingers
        }
    }
    
    /**
     * @since 2.7
     */
    public static Ctor[] getConstructors(Class<?> cls) {
        // Note: can NOT skip abstract classes as they may be used with mix-ins
        // and for regular use shouldn't really matter.
        if (cls.isInterface() || isObjectOrPrimitive(cls)) {
            return NO_CTORS;
        }
        Constructor<?>[] rawCtors = cls.getDeclaredConstructors();
        final int len = rawCtors.length;
        Ctor[] result = new Ctor[len];
        for (int i = 0; i < len; ++i) {
            result[i] = new Ctor(rawCtors[i]);
        }
        return result;
    }

    // // // Then methods that do NOT cache access but were considered
    // // // (and could be added to do caching if it was proven effective)

    /**
     * @since 2.7
     */
    public static Class<?> getDeclaringClass(Class<?> cls) {
        return isObjectOrPrimitive(cls) ? null : cls.getDeclaringClass();
    }

    /**
     * @since 2.7
     */
    public static Type getGenericSuperclass(Class<?> cls) {
        return cls.getGenericSuperclass();
    }

    /**
     * @since 2.7
     */
    public static Type[] getGenericInterfaces(Class<?> cls) {
        return cls.getGenericInterfaces();
    }

    /**
     * @since 2.7
     */
    public static Class<?> getEnclosingClass(Class<?> cls) {
        // Caching does not seem worthwhile, as per profiling
        return isObjectOrPrimitive(cls) ? null : cls.getEnclosingClass();
    }

    private static Class<?>[] _interfaces(Class<?> cls) {
        return cls.getInterfaces();
    }

    /*
    /**********************************************************
    /* Helper classes
    /**********************************************************
     */

    /**
     * Inner class used to contain gory details of how we can determine
     * details of instances of common JDK types like {@link EnumMap}s.
     */
    private static class EnumTypeLocator
    {
        final static EnumTypeLocator instance = new EnumTypeLocator();

        private final Field enumSetTypeField;
        private final Field enumMapTypeField;
    	
        private EnumTypeLocator() {
            //JDK uses following fields to store information about actual Enumeration
            // type for EnumSets, EnumMaps...
    	        enumSetTypeField = locateField(EnumSet.class, "elementType", Class.class);
    	        enumMapTypeField = locateField(EnumMap.class, "elementType", Class.class);
        }

        @SuppressWarnings("unchecked")
        public Class<? extends Enum<?>> enumTypeFor(EnumSet<?> set)
        {
            if (enumSetTypeField != null) {
                return (Class<? extends Enum<?>>) get(set, enumSetTypeField);
            }
            throw new IllegalStateException("Can not figure out type for EnumSet (odd JDK platform?)");
        }

        @SuppressWarnings("unchecked")
        public Class<? extends Enum<?>> enumTypeFor(EnumMap<?,?> set)
        {
            if (enumMapTypeField != null) {
                return (Class<? extends Enum<?>>) get(set, enumMapTypeField);
            }
            throw new IllegalStateException("Can not figure out type for EnumMap (odd JDK platform?)");
        }
    	
        private Object get(Object bean, Field field)
        {
            try {
                return field.get(bean);
            } catch (Exception e) {
                throw new IllegalArgumentException(e);
            }
        }
    	
        private static Field locateField(Class<?> fromClass, String expectedName, Class<?> type)
        {
            Field found = null;
    	        // First: let's see if we can find exact match:
            Field[] fields = getDeclaredFields(fromClass);
    	        for (Field f : fields) {
    	            if (expectedName.equals(f.getName()) && f.getType() == type) {
    	                found = f;
    	                break;
    	            }
    	        }
    	        // And if not, if there is just one field with the type, that field
    	        if (found == null) {
    	            for (Field f : fields) {
    	                if (f.getType() == type) {
    	                    // If more than one, can't choose
    	                    if (found != null) return null;
    	                    found = f;
    	                }
    	            }
    	        }
    	        if (found != null) { // it's non-public, need to force accessible
    	            try {
    	                found.setAccessible(true);
    	            } catch (Throwable t) { }
    	        }
    	        return found;
        }
    }

    /*
    /**********************************************************
    /* Helper classed used for caching
    /**********************************************************
     */

    /**
     * Value class used for caching Constructor declarations; used because
     * caching done by JDK appears to be somewhat inefficient for some use cases.
     *
     * @since 2.7
     */
    public final static class Ctor
    {
        public final Constructor<?> _ctor;

        private Annotation[] _annotations;

        private  Annotation[][] _paramAnnotations;
        
        private int _paramCount = -1;
        
        public Ctor(Constructor<?> ctor) {
            _ctor = ctor;
        }

        public Constructor<?> getConstructor() {
            return _ctor;
        }

        public int getParamCount() {
            int c = _paramCount;
            if (c < 0) {
                c = _ctor.getParameterTypes().length;
                _paramCount = c;
            }
            return c;
        }

        public Class<?> getDeclaringClass() {
            return _ctor.getDeclaringClass();
        }

        public Annotation[] getDeclaredAnnotations() {
            Annotation[] result = _annotations;
            if (result == null) {
                result = _ctor.getDeclaredAnnotations();
                _annotations = result;
            }
            return result;
        }

        public  Annotation[][] getParameterAnnotations() {
            Annotation[][] result = _paramAnnotations;
            if (result == null) {
                result = _ctor.getParameterAnnotations();
                _paramAnnotations = result;
            }
            return result;
        }
    }
}
