package tools.jackson.databind;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import tools.jackson.databind.annotation.JsonPOJOBuilder;
import tools.jackson.databind.introspect.*;
import tools.jackson.databind.util.Annotations;
import tools.jackson.databind.util.Converter;

/**
 * Basic container for information gathered by {@link ClassIntrospector} to
 * help in constructing serializers and deserializers.
 * Note that the one implementation type is
 * {@link tools.jackson.databind.introspect.BasicBeanDescription},
 * meaning that it is safe to upcast to that type.
 */
public abstract class BeanDescription
{
    /**
     * Bean type information, including raw class and possible
     * generics information
     */
    protected final JavaType _type;

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    protected BeanDescription(JavaType type) {
        _type = type;
    }

    /*
    /**********************************************************************
    /* Simple accessors
    /**********************************************************************
     */

    /**
     * Method for accessing declared type of bean being introspected,
     * including full generic type information (from declaration)
     */
    public JavaType getType() { return _type; }

    public Class<?> getBeanClass() { return _type.getRawClass(); }

    public boolean isRecordType() { return _type.isRecordType(); }

    public boolean isNonStaticInnerClass() {
        return getClassInfo().isNonStaticInnerClass();
    }

    /**
     * Method for accessing low-level information about Class this
     * item describes.
     */
    public abstract AnnotatedClass getClassInfo();

    /**
     * Accessor for getting information about Object Id expected to
     * be used for this POJO type, if any.
     */
    public abstract ObjectIdInfo getObjectIdInfo();

    /**
     * Method for checking whether class being described has any
     * annotations recognized by registered annotation introspector.
     */
    public abstract boolean hasKnownClassAnnotations();

    /**
     * Method for accessing collection of annotations the bean
     * class has.
     */
    public abstract Annotations getClassAnnotations();

    /*
    /**********************************************************************
    /* Basic API for finding properties
    /**********************************************************************
     */

    /**
     * @return Ordered Map with logical property name as key, and
     *    matching getter method as value.
     */
    public abstract List<BeanPropertyDefinition> findProperties();

    public abstract Set<String> getIgnoredPropertyNames();

    /**
     * Method for locating all back-reference properties (setters, fields) bean has
     */
    public abstract List<BeanPropertyDefinition> findBackReferences();

    /*
    /**********************************************************************
    /* Basic API for finding Creators, related information
    /**********************************************************************
     */

    /**
     * Helper method that will return all non-default constructors (that is,
     * constructors that take one or more arguments) this class has.
     */
    public abstract List<AnnotatedConstructor> getConstructors();

    /**
     * Method similar to {@link #getConstructors()} except will also introspect
     * {@code JsonCreator.Mode} and filter out ones marked as not applicable and
     * include mode (or lack thereof) for remaining constructors.
     *<p>
     * Note that no other filtering (regarding visibility or other annotations)
     * is performed
     *
     * @since 2.13
     */
    public abstract List<AnnotatedAndMetadata<AnnotatedConstructor, JsonCreator.Mode>> getConstructorsWithMode();

    /**
     * Helper method that will check all static methods of the bean class
     * that seem like factory methods eligible to be used as Creators.
     * This requires that the static method:
     *<ol>
     * <li>Returns type compatible with bean type (same or subtype)
     *  </li>
     * <li>Is recognized from either explicit annotation (usually {@code @JsonCreator}
     *   OR naming:
     *   names {@code valueOf()} and {@code fromString()} are recognized but
     *   only for 1-argument factory methods, and in case of {@code fromString()}
     *   argument type must further be either {@code String} or {@code CharSequence}.
     *  </li>
     *</ol>
     * Note that caller typically applies further checks for things like visibility.
     *
     * @return List of static methods considered as possible Factory methods
     */
    public abstract List<AnnotatedMethod> getFactoryMethods();

    /**
     * Method similar to {@link #getFactoryMethods()} but will return {@code JsonCreator.Mode}
     * metadata along with qualifying factory method candidates.
     *
     * @since 2.13
     */
    public abstract List<AnnotatedAndMetadata<AnnotatedMethod, JsonCreator.Mode>> getFactoryMethodsWithMode();

    /**
     * Method that will locate the no-arg constructor for this class,
     * if it has one, and that constructor has not been marked as
     * ignorable.
     */
    public abstract AnnotatedConstructor findDefaultConstructor();

    /**
     * Method that is replacing earlier Creator introspection access methods.
     *
     * @since 2.18
     *
     * @return Container for introspected Creator candidates, if any
     */
    public abstract PotentialCreators getPotentialCreators();

    /*
    /**********************************************************************
    /* Basic API for finding property accessors
    /**********************************************************************
     */

    /**
     * Method for locating accessor (readable field, or "getter" method)
     * that has
     * {@link com.fasterxml.jackson.annotation.JsonKey} annotation,
     * if any. If multiple ones are found,
     * an error is reported by throwing {@link IllegalArgumentException}
     */
    public AnnotatedMember findJsonKeyAccessor() {
        return null;
    }

    /**
     * Method for locating accessor (readable field, or "getter" method)
     * that has
     * {@link com.fasterxml.jackson.annotation.JsonValue} annotation,
     * if any. If multiple ones are found,
     * an error is reported by throwing {@link IllegalArgumentException}
     */
    public abstract AnnotatedMember findJsonValueAccessor();

    public abstract AnnotatedMember findAnyGetter();

    /**
     * Method used to locate a mutator (settable field, or 2-argument set method)
     * of introspected class that
     * implements {@link com.fasterxml.jackson.annotation.JsonAnySetter}.
     * If no such mutator exists null is returned. If more than one are found,
     * an exception is thrown.
     * Additional checks are also made to see that method signature
     * is acceptable: needs to take 2 arguments, first one String or
     * Object; second any can be any type.
     */
    public abstract AnnotatedMember findAnySetterAccessor();

    public abstract AnnotatedMethod findMethod(String name, Class<?>[] paramTypes);

    /*
    /**********************************************************************
    /* Basic API, class configuration
    /**********************************************************************
     */

    /**
     * Method for finding annotation-indicated inclusion definition (if any);
     * possibly overriding given default value.
     *<p>
     * NOTE: does NOT use global inclusion default settings as the base, unless
     * passed as `defValue`.
     */
    public abstract JsonInclude.Value findPropertyInclusion(JsonInclude.Value defValue);

    /**
     * Method for checking what is the expected format for POJO, as
     * defined by possible annotations (but NOT config overrides)
     *
     * @deprecated Since 3.0
     */
    @Deprecated // since 3.0
    public abstract JsonFormat.Value findExpectedFormat();

    /**
     * Method for checking what is the expected format for POJO, as
     * defined by possible annotations and possible per-type config overrides.
     */
    public abstract JsonFormat.Value findExpectedFormat(Class<?> baseType);

    /**
     * Method for finding {@link Converter} used for serializing instances
     * of this class.
     */
    public abstract Converter<Object,Object> findSerializationConverter();

    /**
     * Method for finding {@link Converter} used for serializing instances
     * of this class.
     */
    public abstract Converter<Object,Object> findDeserializationConverter();

    /**
     * Accessor for possible description for the bean type, used for constructing
     * documentation.
     */
    public String findClassDescription() { return null; }

    /*
    /**********************************************************************
    /* Basic API, other
    /**********************************************************************
     */

    public abstract Map<Object, AnnotatedMember> findInjectables();

    /**
     * Method for checking if the POJO type has annotations to
     * indicate that a builder is to be used for instantiating
     * instances and handling data binding, instead of standard
     * bean deserializer.
     */
    public abstract Class<?> findPOJOBuilder();

    /**
     * Method for finding configuration for POJO Builder class.
     */
    public abstract JsonPOJOBuilder.Value findPOJOBuilderConfig();

    /**
     * Method called to create a "default instance" of the bean, currently
     * only needed for obtaining default field values which may be used for
     * suppressing serialization of fields that have "not changed".
     *
     * @param fixAccess If true, method is allowed to fix access to the
     *   default constructor (to be able to call non-public constructor);
     *   if false, has to use constructor as is.
     *
     * @return Instance of class represented by this descriptor, if
     *   suitable default constructor was found; null otherwise.
     */
    public abstract Object instantiateBean(boolean fixAccess);

    /**
     * Method for finding out if the POJO specifies default view(s) to
     * use for properties, considering both per-type annotations and
     * global default settings.
     */
    public abstract Class<?>[] findDefaultViews();

 
    /**
     * Base implementation for lazily-constructed suppliers for {@link BeanDescription} instances.
     */
    public static abstract class Supplier implements java.util.function.Supplier<BeanDescription>
    {
        private final JavaType _type;

        private transient BeanDescription _beanDesc;
        
        protected Supplier(JavaType type) {
            _type = type;
        }

        public JavaType getType() { return _type; }

        public Class<?> getBeanClass() { return _type.getRawClass(); }

        public boolean isRecordType() { return _type.isRecordType(); }

        public AnnotatedClass getClassInfo() {
            return get().getClassInfo();
        }

        @Override
        public BeanDescription get() {
            if (_beanDesc == null) {
                _beanDesc = _construct(_type);
            }
            return _beanDesc;
        }

        protected abstract BeanDescription _construct(JavaType forType);
    }
}
