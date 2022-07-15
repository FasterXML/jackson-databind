package tools.jackson.databind.introspect;

import java.lang.reflect.*;

import tools.jackson.databind.JavaType;
import tools.jackson.databind.util.ClassUtil;

/**
 * Object that represents non-static (and usually non-transient/volatile)
 * fields of a class.
 */
public final class AnnotatedField
    extends AnnotatedMember
{
    /**
     * Actual {@link Field} used for access.
     */
    protected final Field _field;

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    public AnnotatedField(TypeResolutionContext contextClass, Field field, AnnotationMap annMap)
    {
        super(contextClass, annMap);
        _field = field;
    }
    
    @Override
    public AnnotatedField withAnnotations(AnnotationMap ann) {
        return new AnnotatedField(_typeContext, _field, ann);
    }

    /*
    /**********************************************************************
    /* Annotated impl
    /**********************************************************************
     */

    @Override
    public Field getAnnotated() { return _field; }

    @Override
    public int getModifiers() { return _field.getModifiers(); }

    @Override
    public String getName() { return _field.getName(); }

    @Override
    public Class<?> getRawType() {
        return _field.getType();
    }

    @Override
    public JavaType getType() {
        return _typeContext.resolveType(_field.getGenericType());
    }

    /*
    /**********************************************************************
    /* AnnotatedMember impl
    /**********************************************************************
     */

    @Override
    public Class<?> getDeclaringClass() { return _field.getDeclaringClass(); }

    @Override
    public Member getMember() { return _field; }

    @Override
    public void setValue(Object pojo, Object value) throws IllegalArgumentException
    {
        try {
            _field.set(pojo, value);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("Failed to setValue() for field "
                    +getFullName()+": "+e.getMessage(), e);
        }
    }

    @Override
    public Object getValue(Object pojo) throws IllegalArgumentException
    {
        try {
            return _field.get(pojo);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("Failed to getValue() for field "
                    +getFullName()+": "+e.getMessage(), e);
        }
    }

    /*
    /**********************************************************************
    /* Extended API, generic
    /**********************************************************************
     */

    public int getAnnotationCount() { return _annotations.size(); }

    /**
     * @since 2.6
     */
    public boolean isTransient() { return Modifier.isTransient(getModifiers()); }
    
    @Override
    public int hashCode() {
        return _field.getName().hashCode();
    }
    
    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!ClassUtil.hasClass(o, getClass())) {
            return false;
        }

        AnnotatedField other = (AnnotatedField) o;
        if (other._field == null) {
            return _field == null;
        } else {
            return other._field.equals(_field);
        }
    }

    @Override
    public String toString() {
        return "[field "+getFullName()+"]";
    }
}

