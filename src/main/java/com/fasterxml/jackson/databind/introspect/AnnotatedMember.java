package com.fasterxml.jackson.databind.introspect;

import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.util.Collections;

import com.fasterxml.jackson.databind.util.ClassUtil;

/**
 * Intermediate base class for annotated entities that are members of
 * a class; fields, methods and constructors. This is a superset
 * of things that can represent logical properties as it contains
 * constructors in addition to fields and methods.
 */
public abstract class AnnotatedMember
    extends Annotated
    implements java.io.Serializable
{
    private static final long serialVersionUID = 1L; // since 2.5

    // 19-Dec-2014, tatu: Similarly, assumed NOT to be needed in cases where
    //    owning object (ObjectMapper or relatives) is being JDK-serialized
    /**
     * Context object needed for resolving generic type associated with this
     * member (method parameter or return value, or field type).
     *
     * @since 2.7
     */
    protected final transient TypeResolutionContext _typeContext;

    // Transient since information not needed after construction, so
    // no need to persist
    protected final transient AnnotationMap _annotations;

    protected AnnotatedMember(TypeResolutionContext ctxt, AnnotationMap annotations) {
        super();
        _typeContext = ctxt;
        _annotations = annotations;
    }

    /**
     * Copy-constructor.
     *
     * @since 2.5
     */
    protected AnnotatedMember(AnnotatedMember base) {
        _typeContext = base._typeContext;
        _annotations = base._annotations;
    }
    
    /**
     * Actual physical class in which this memmber was declared.
     */
    public abstract Class<?> getDeclaringClass();

    public abstract Member getMember();

    /**
     * Accessor for {@link TypeResolutionContext} that is used for resolving
     * full generic type of this member.
     * 
     * @since 2.7
     */
    public TypeResolutionContext getTypeContext() {
        return _typeContext;
    }

    @Override
    public final <A extends Annotation> A getAnnotation(Class<A> acls) {
        if (_annotations == null) {
            return null;
        }
        return _annotations.get(acls);
    }

    @Override
    public final boolean hasAnnotation(Class<?> acls) {
        if (_annotations == null) {
            return false;
        }
        return _annotations.has(acls);
    }

    @Override
    public boolean hasOneOf(Class<? extends Annotation>[] annoClasses) {
        if (_annotations == null) {
            return false;
        }
        return _annotations.hasOneOf(annoClasses);
    }
    
    @Override
    public Iterable<Annotation> annotations() {
        if (_annotations == null) {
            return Collections.emptyList();
        }
        return _annotations.annotations();
    }
    
    @Override
    protected AnnotationMap getAllAnnotations() {
        return _annotations;
    }

    /**
     * Method called to override an annotation, usually due to a mix-in
     * annotation masking or overriding an annotation 'real' constructor
     * has.
     */
    public final boolean addOrOverride(Annotation a) {
        return _annotations.add(a);
    }

    /**
     * Method called to augment annotations, by adding specified
     * annotation if and only if it is not yet present in the
     * annotation map we have.
     */
    public final boolean addIfNotPresent(Annotation a) {
        return _annotations.addIfNotPresent(a);
    }

    /**
     * Method that can be called to modify access rights, by calling
     * {@link java.lang.reflect.AccessibleObject#setAccessible} on
     * the underlying annotated element.
     *<p>
     * Note that caller should verify that
     * {@link com.fasterxml.jackson.databind.MapperFeature#CAN_OVERRIDE_ACCESS_MODIFIERS}
     * is enabled before calling this method; as well as pass
     * <code>force</code> flag appropriately.
     * 
     * @since 2.7
     */
    public final void fixAccess(boolean force) {
        Member m = getMember();
        if (m != null) { // may be null for virtual members
            ClassUtil.checkAndFixAccess(m, force);
        }
    }

    /**
     * @deprecated Since 2.7 use {@link #fixAccess(boolean)} instead
     */
    @Deprecated
    public final void fixAccess() {
//        fixAccess(false);
        fixAccess(true);
    }

    /**
     * Optional method that can be used to assign value of
     * this member on given object, if this is a supported
     * operation for member type.
     *<p>
     * This is implemented for fields and single-argument
     * member methods; but not for constructor parameters or
     * other types of methods (like static methods)
     */
    public abstract void setValue(Object pojo, Object value)
        throws UnsupportedOperationException, IllegalArgumentException;

    /**
     * Optional method that can be used to access the value of
     * this member on given object, if this is a supported
     * operation for member type.
     *<p>
     * This is implemented for fields and no-argument
     * member methods; but not for constructor parameters or
     * other types of methods (like static methods)
     */
    public abstract Object getValue(Object pojo)
        throws UnsupportedOperationException, IllegalArgumentException;
}
