package tools.jackson.databind;

import java.util.Collection;
import java.util.function.UnaryOperator;

import tools.jackson.core.*;
import tools.jackson.databind.cfg.MapperBuilder;
import tools.jackson.databind.cfg.MutableConfigOverride;
import tools.jackson.databind.deser.*;
import tools.jackson.databind.jsontype.NamedType;
import tools.jackson.databind.ser.Serializers;
import tools.jackson.databind.ser.ValueSerializerModifier;
import tools.jackson.databind.type.TypeFactory;
import tools.jackson.databind.type.TypeModifier;

import java.util.Collections;

/**
 * Simple interface for extensions that can be registered with {@link ObjectMapper}
 * to provide a well-defined set of extensions to default functionality; such as
 * support for new data types.
 *<p>
 * NOTE: was named just {@code Module} in Jackson 2.x but renamed due to naming
 * conflict with Java 9+ {@code java.lang.Module}
 */
public abstract class JacksonModule
    implements Versioned
{
    /*
    /**********************************************************************
    /* Simple accessors
    /**********************************************************************
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
     * Method that returns an id that may be used to determine if two {@link JacksonModule}
     * instances are considered to be of same type, for purpose of preventing
     * multiple registrations of "same" module,
     *<p>
     * Default implementation returns value of class name ({@link Class#getName}).
     *
     * @since 3.0
     */
    public Object getRegistrationId() {
        return getClass().getName();
    }

    /**
     * Returns the list of dependent modules this module has, if any.
     * It is called to let modules register other modules as dependencies.
     * Modules returned will be registered before this module is registered,
     * in iteration order.
     */
    public Iterable<? extends JacksonModule> getDependencies() {
        return Collections.emptyList();
    }

    /*
    /**********************************************************************
    /* Life-cycle: registration
    /**********************************************************************
     */

    /**
     * Method called by {@link ObjectMapper} when module is registered.
     * It is called to let module register functionality it provides,
     * using callback methods passed-in context object exposes.
     */
    public abstract void setupModule(SetupContext context);

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
        /******************************************************************
        /* Simple accessors
        /******************************************************************
         */

        /**
         * Method that returns version information about {@link ObjectMapper}
         * that implements this context. Modules can use this to choose
         * different settings or initialization order; or even decide to fail
         * set up completely if version is compatible with module.
         */
        public Version getMapperVersion();

        /**
         * @since 3.0
         */
        public String getFormatName();

        /**
         * Fallback access method that allows modules to refer to the
         * {@link MapperBuilder} that provided this context.
         * It should NOT be needed by most modules; and ideally should
         * not be used -- however, there may be cases where this may
         * be necessary due to various design constraints.
         *<p>
         * NOTE: use of this method is discouraged, as it allows access to
         * things Modules typically should not modify. It is included, however,
         * to allow access to new features in cases where Module API
         * has not yet been extended, or there are oversights.
         *<p>
         * Return value is chosen to force casting, to make caller aware that
         * this is a fallback accessor, used only when everything else fails:
         * type is, however, guaranteed to be {@link MapperBuilder} (and more
         * specifically format-specific subtype that mapper constructed, in case
         * format-specific access is needed).
         */
        public Object getOwner();

        /**
         * Accessor for finding {@link TypeFactory} that is currently configured
         * by the context.
         *<p>
         * NOTE: since it is possible that other modules might change or replace
         * TypeFactory, use of this method adds order-dependency for registrations.
         */
        public TypeFactory typeFactory();

        public TokenStreamFactory tokenStreamFactory();

        public boolean isEnabled(MapperFeature f);
        public boolean isEnabled(DeserializationFeature f);
        public boolean isEnabled(SerializationFeature f);
        public boolean isEnabled(TokenStreamFactory.Feature f);
        public boolean isEnabled(StreamReadFeature f);
        public boolean isEnabled(StreamWriteFeature f);

        /*
        /******************************************************************
        /* Mutant accessors
        /******************************************************************
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
         */
        public MutableConfigOverride configOverride(Class<?> type);

        /*
        /******************************************************************
        /* Handler registration; deserializers, related
        /******************************************************************
         */

        /**
         * Method that module can use to register additional deserializers to use for
         * handling types.
         *
         * @param d Object that can be called to find deserializer for types supported
         *   by module (null returned for non-supported types)
         * @see #addKeyDeserializers is used to register key deserializers (for Map keys)
         */
        public SetupContext addDeserializers(Deserializers d);

        /**
         * Method that module can use to register additional deserializers to use for
         * handling Map key values (which are separate from value deserializers because
         * they are always serialized from String values)
         */
        public SetupContext addKeyDeserializers(KeyDeserializers s);

        /**
         * Method that module can use to register additional modifier objects to
         * customize configuration and construction of bean deserializers.
         *
         * @param mod Modifier to register
         */
        public SetupContext addDeserializerModifier(ValueDeserializerModifier mod);

        /**
         * Method that module can use to register additional {@link tools.jackson.databind.deser.ValueInstantiator}s,
         * by adding {@link ValueInstantiators} object that gets called when
         * instantatiator is needed by a deserializer.
         *
         * @param instantiators Object that can provide {@link tools.jackson.databind.deser.ValueInstantiator}s for
         *    constructing POJO values during deserialization
         */
        public SetupContext addValueInstantiators(ValueInstantiators instantiators);

        /*
        /******************************************************************
        /* Handler registration; serializers, related
        /******************************************************************
         */

        /**
         * Method that module can use to register additional serializers to use for
         * handling types.
         *
         * @param s Object that can be called to find serializer for types supported
         *   by module (null returned for non-supported types)
         * @see #addKeySerializers is used to register key serializers (for Map keys)
         */
        public SetupContext addSerializers(Serializers s);

        /**
         * Method that module can use to register additional serializers to use for
         * handling Map key values (which are separate from value serializers because
         * they must write <code>JsonToken.FIELD_NAME</code> instead of String value).
         */
        public SetupContext addKeySerializers(Serializers s);

        /**
         * Method that module can use to register additional modifier objects to
         * customize configuration and construction of bean serializers.
         *
         * @param mod Modifier to register
         */
        public SetupContext addSerializerModifier(ValueSerializerModifier mod);

        /**
         * Method that module can use to override handler called to write JSON Object key
         * for {@link java.util.Map} values.
         *
         * @param ser Serializer called to write output for JSON Object key of which value
         *   on Java side is `null`
         */
        public SetupContext overrideDefaultNullKeySerializer(ValueSerializer<?> ser);

        /**
         * Method that module can use to override handler called to write Java `null` as
         * a value (Property or Map value, Collection/array element).
         *
         * @param ser Serializer called to write output for Java `null` as value (as
         *    distinct from key)
         */
        public SetupContext overrideDefaultNullValueSerializer(ValueSerializer<?> ser);

        /*
        /******************************************************************
        /* Handler registration, annotation introspectors
        /******************************************************************
         */

        /**
         * Method for registering specified {@link AnnotationIntrospector} as the highest
         * priority introspector (will be chained with existing introspector(s) which
         * will be used as fallbacks for cases this introspector does not handle)
         *
         * @param ai Annotation introspector to register.
         */
        public SetupContext insertAnnotationIntrospector(AnnotationIntrospector ai);

        /**
         * Method for registering specified {@link AnnotationIntrospector} as the lowest
         * priority introspector, chained with existing introspector(s) and called
         * as fallback for cases not otherwise handled.
         *
         * @param ai Annotation introspector to register.
         */
        public SetupContext appendAnnotationIntrospector(AnnotationIntrospector ai);

        /*
        /******************************************************************
        /* Type handling
        /******************************************************************
         */

        /**
         * Method that module can use to register additional
         * {@link AbstractTypeResolver} instance, to handle resolution of
         * abstract to concrete types (either by defaulting, or by materializing).
         *
         * @param resolver Resolver to add.
         */
        public SetupContext addAbstractTypeResolver(AbstractTypeResolver resolver);

        /**
         * Method that module can use to register additional
         * {@link TypeModifier} instance, which can augment {@link tools.jackson.databind.JavaType}
         * instances constructed by {@link tools.jackson.databind.type.TypeFactory}.
         *
         * @param modifier to add
         */
        public SetupContext addTypeModifier(TypeModifier modifier);

        /**
         * Method for registering specified classes as subtypes (of supertype(s)
         * they have)
         */
        public SetupContext registerSubtypes(Class<?>... subtypes);

        /**
         * Method for registering specified classes as subtypes (of supertype(s)
         * they have), using specified type names.
         */
        public SetupContext registerSubtypes(NamedType... subtypes);

        /**
         * Method for registering specified classes as subtypes (of supertype(s)
         * they have)
         */
        public SetupContext registerSubtypes(Collection<Class<?>> subtypes);

        /*
        /******************************************************************
        /* Handler registration, other
        /******************************************************************
         */

        /**
         * Add a deserialization problem handler
         *
         * @param handler The deserialization problem handler
         */
        public SetupContext addHandler(DeserializationProblemHandler handler);

        /**
         * Replace default {@link InjectableValues} that have been configured to be
         * used for mapper being built.
         *
         * @since 3.0
         */
        public SetupContext overrideInjectableValues(UnaryOperator<InjectableValues> v);

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
        public SetupContext setMixIn(Class<?> target, Class<?> mixinSource);
    }
}
