package com.fasterxml.jackson.databind;

import java.util.Collection;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.cfg.MutableConfigOverride;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import com.fasterxml.jackson.databind.deser.Deserializers;
import com.fasterxml.jackson.databind.deser.KeyDeserializers;
import com.fasterxml.jackson.databind.deser.ValueInstantiators;
import com.fasterxml.jackson.databind.introspect.ClassIntrospector;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.Serializers;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.type.TypeModifier;
import java.util.Collections;

/**
 * Simple interface for extensions that can be registered with {@link ObjectMapper}
 * to provide a well-defined set of extensions to default functionality; such as
 * support for new data types.
 */
public abstract class Module
    implements Versioned
{
    /*
    /**********************************************************
    /* Simple accessors
    /**********************************************************
     */

    /**
     * Method that returns a display that can be used by Jackson
     * for informational purposes, as well as in associating extensions with
     * module that provides them.
     */
    public abstract String getModuleName();

    /**
     * Method that returns version of this module. Can be used by Jackson for
     * informational purposes.
     */
    @Override
    public abstract Version version();

    /**
     * Method that returns an id that may be used to determine if two {@link Module}
     * instances are considered to be of same type, for purpose of preventing
     * multiple registrations of "same type of" module
     * (see {@link com.fasterxml.jackson.databind.MapperFeature#IGNORE_DUPLICATE_MODULE_REGISTRATIONS})
     * If `null` is returned, every instance is considered unique.
     * If non-null value is returned, equality of id Objects is used to check whether
     * modules should be considered to be "of same type"
     *<p>
     * Default implementation returns value of class name ({@link Class#getName}).
     *
     * @since 2.5
     */
    public Object getTypeId() {
        return getClass().getName();
    }

    /*
    /**********************************************************
    /* Life-cycle: registration
    /**********************************************************
     */

    /**
     * Method called by {@link ObjectMapper} when module is registered.
     * It is called to let module register functionality it provides,
     * using callback methods passed-in context object exposes.
     */
    public abstract void setupModule(SetupContext context);

    /**
     * Returns the list of dependent modules this module has, if any.
     * It is called to let modules register other modules as dependencies.
     * Modules returned will be registered before this module is registered,
     * in iteration order.
     *
     * @since 2.10
     */
    public Iterable<? extends Module> getDependencies() {
        return Collections.emptyList();
    }

    /*
    /**********************************************************
    /* Helper types
    /**********************************************************
     */

    /**
     * Interface Jackson exposes to modules for purpose of registering
     * extended functionality.
     * Usually implemented by {@link ObjectMapper}, but modules should
     * NOT rely on this -- if they do require access to mapper instance,
     * they need to call {@link SetupContext#getOwner} method.
     */
    public static interface SetupContext
    {
        /*
        /**********************************************************
        /* Simple accessors
        /**********************************************************
         */

        /**
         * Method that returns version information about {@link ObjectMapper}
         * that implements this context. Modules can use this to choose
         * different settings or initialization order; or even decide to fail
         * set up completely if version is compatible with module.
         */
        public Version getMapperVersion();

        /**
         * Fallback access method that allows modules to refer to the
         * {@link ObjectMapper} that provided this context.
         * It should NOT be needed by most modules; and ideally should
         * not be used -- however, there may be cases where this may
         * be necessary due to various design constraints.
         *<p>
         * NOTE: use of this method is discouraged, as it allows access to
         * things Modules typically should not modify. It is included, however,
         * to allow access to new features in cases where Module API
         * has not yet been extended, or there are oversights.
         *<p>
         * Return value is chosen to not leak dependency to {@link ObjectMapper};
         * however, instance will always be of that type.
         * This is why return value is declared generic, to allow caller to
         * specify context to often avoid casting.
         *
         * @since 2.0
         */
        public <C extends ObjectCodec> C getOwner();

        /**
         * Accessor for finding {@link TypeFactory} that is currently configured
         * by the context.
         *<p>
         * NOTE: since it is possible that other modules might change or replace
         * TypeFactory, use of this method adds order-dependency for registrations.
         *
         * @since 2.0
         */
        public TypeFactory getTypeFactory();

        public boolean isEnabled(MapperFeature f);

        public boolean isEnabled(DeserializationFeature f);

        public boolean isEnabled(SerializationFeature f);

        public boolean isEnabled(JsonFactory.Feature f);

        public boolean isEnabled(JsonParser.Feature f);

        public boolean isEnabled(JsonGenerator.Feature f);

        /*
        /**********************************************************
        /* Mutant accessors
        /**********************************************************
         */

        /**
         * "Mutant accessor" for getting a mutable configuration override object for
         * given type, needed to add or change per-type overrides applied
         * to properties of given type.
         * Usage is through returned object by colling "setter" methods, which
         * directly modify override object and take effect directly.
         * For example you can do
         *<pre>
         *   mapper.configOverride(java.util.Date.class)
         *       .setFormat(JsonFormat.Value.forPattern("yyyy-MM-dd"));
         *</pre>
         * to change the default format to use for properties of type
         * {@link java.util.Date} (possibly further overridden by per-property
         * annotations)
         *
         * @since 2.8
         */
        public MutableConfigOverride configOverride(Class<?> type);

        /*
        /**********************************************************
        /* Handler registration; serializers/deserializers
        /**********************************************************
         */

        /**
         * Method that module can use to register additional deserializers to use for
         * handling types.
         *
         * @param d Object that can be called to find deserializer for types supported
         *   by module (null returned for non-supported types)
         */
        public void addDeserializers(Deserializers d);

        /**
         * Method that module can use to register additional deserializers to use for
         * handling Map key values (which are separate from value deserializers because
         * they are always serialized from String values)
         */
        public void addKeyDeserializers(KeyDeserializers s);

        /**
         * Method that module can use to register additional serializers to use for
         * handling types.
         *
         * @param s Object that can be called to find serializer for types supported
         *   by module (null returned for non-supported types)
         */
        public void addSerializers(Serializers s);

        /**
         * Method that module can use to register additional serializers to use for
         * handling Map key values (which are separate from value serializers because
         * they must write <code>JsonToken.FIELD_NAME</code> instead of String value).
         */
        public void addKeySerializers(Serializers s);

        /*
        /**********************************************************
        /* Handler registration; other
        /**********************************************************
         */

        /**
         * Method that module can use to register additional modifier objects to
         * customize configuration and construction of bean deserializers.
         *
         * @param mod Modifier to register
         */
        public void addBeanDeserializerModifier(BeanDeserializerModifier mod);

        /**
         * Method that module can use to register additional modifier objects to
         * customize configuration and construction of bean serializers.
         *
         * @param mod Modifier to register
         */
        public void addBeanSerializerModifier(BeanSerializerModifier mod);

        /**
         * Method that module can use to register additional
         * {@link AbstractTypeResolver} instance, to handle resolution of
         * abstract to concrete types (either by defaulting, or by materializing).
         *
         * @param resolver Resolver to add.
         */
        public void addAbstractTypeResolver(AbstractTypeResolver resolver);

        /**
         * Method that module can use to register additional
         * {@link TypeModifier} instance, which can augment {@link com.fasterxml.jackson.databind.JavaType}
         * instances constructed by {@link com.fasterxml.jackson.databind.type.TypeFactory}.
         *
         * @param modifier to add
         */
        public void addTypeModifier(TypeModifier modifier);

        /**
         * Method that module can use to register additional {@link com.fasterxml.jackson.databind.deser.ValueInstantiator}s,
         * by adding {@link ValueInstantiators} object that gets called when
         * instantatiator is needed by a deserializer.
         *
         * @param instantiators Object that can provide {@link com.fasterxml.jackson.databind.deser.ValueInstantiator}s for
         *    constructing POJO values during deserialization
         */
        public void addValueInstantiators(ValueInstantiators instantiators);

        /**
         * Method for replacing the default class introspector with a derived class that
         * overrides specific behavior.
         *
         * @param ci Derived class of ClassIntrospector with overriden behavior
         *
         * @since 2.2
         */
        public void setClassIntrospector(ClassIntrospector ci);

        /**
         * Method for registering specified {@link AnnotationIntrospector} as the highest
         * priority introspector (will be chained with existing introspector(s) which
         * will be used as fallbacks for cases this introspector does not handle)
         *
         * @param ai Annotation introspector to register.
         */
        public void insertAnnotationIntrospector(AnnotationIntrospector ai);

        /**
         * Method for registering specified {@link AnnotationIntrospector} as the lowest
         * priority introspector, chained with existing introspector(s) and called
         * as fallback for cases not otherwise handled.
         *
         * @param ai Annotation introspector to register.
         */
        public void appendAnnotationIntrospector(AnnotationIntrospector ai);

        /**
         * Method for registering specified classes as subtypes (of supertype(s)
         * they have)
         */
        public void registerSubtypes(Class<?>... subtypes);

        /**
         * Method for registering specified classes as subtypes (of supertype(s)
         * they have), using specified type names.
         */
        public void registerSubtypes(NamedType... subtypes);

        /**
         * Method for registering specified classes as subtypes (of supertype(s)
         * they have)
         *
         * @since 2.9
         */
        public void registerSubtypes(Collection<Class<?>> subtypes);

        /**
         * Method used for defining mix-in annotations to use for augmenting
         * specified class or interface.
         * All annotations from
         * <code>mixinSource</code> are taken to override annotations
         * that <code>target</code> (or its supertypes) has.
         *<p>
         * Note: mix-ins are registered both for serialization and deserialization
         * (which can be different internally).
         *<p>
         * Note: currently only one set of mix-in annotations can be defined for
         * a single class; so if multiple modules register mix-ins, highest
         * priority one (last one registered) will have priority over other modules.
         *
         * @param target Class (or interface) whose annotations to effectively override
         * @param mixinSource Class (or interface) whose annotations are to
         *   be "added" to target's annotations, overriding as necessary
         */
        public void setMixInAnnotations(Class<?> target, Class<?> mixinSource);

        /**
         * Add a deserialization problem handler
         *
         * @param handler The deserialization problem handler
         */
        public void addDeserializationProblemHandler(DeserializationProblemHandler handler);

        /**
         * Method that may be used to override naming strategy that is used
         * by {@link ObjectMapper}.
         *
         * @since 2.3
         */
        public void setNamingStrategy(PropertyNamingStrategy naming);
    }
}
