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
     * Fluent factory method that will construct a new instance that uses specified
     * instance annotations instead of currently configured ones.
     *
     * @since 2.9 (promoted from `Annotated`)
     */
    public abstract Annotated withAnnotations(AnnotationMap fallback);

    /**
     * Actual physical class in which this memmber was declared.
     */
    public abstract Class<?> getDeclaringClass();

    public abstract Member getMember();

    public String getFullName() {
        return getDeclaringClass().getName() + "#" + getName();
    }

    /**
     * Accessor for {@link TypeResolutionContext} that is used for resolving
     * full generic type of this member.
     *
     * @since 2.7
     *
     * @deprecated Since 2.9
     */
    @Deprecated
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
    @Deprecated
    public Iterable<Annotation> annotations() {
        if (_annotations == null) {
            return Collections.emptyList();
        }
        return _annotations.annotations();
    }

    /**
     *<p>
     * NOTE: promoted in 2.9 from `Annotated` up
     */
    public AnnotationMap getAllAnnotations() { // alas, used by at least one module, hence public
        return _annotations;
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
