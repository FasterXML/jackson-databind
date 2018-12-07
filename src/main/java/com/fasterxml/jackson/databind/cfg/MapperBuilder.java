package com.fasterxml.jackson.databind.cfg;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.DateFormat;
import java.util.*;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.*;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.jsontype.SubtypeResolver;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.ser.*;
import com.fasterxml.jackson.databind.type.TypeFactory;

/**
 * Jackson 3 will introduce fully immutable, builder-based system for constructing
 * {@link ObjectMapper}s. Same can not be done with 2.10 for backwards-compatibility
 * reasons; but we can offer sort of "fake" builder, which simply encapsulates
 * configuration calls. The main (and only) point is to allow gradual upgrade.
 *
 * @since 2.10
 */
public abstract class MapperBuilder<M extends ObjectMapper,
    B extends MapperBuilder<M,B>>
{
    protected final M _mapper;

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    protected MapperBuilder(M mapper)
    {
        _mapper = mapper;
    }

    /**
     * Method to call to create actual mapper instance, usually passing {@code this}
     * builder to its constructor.
     */
    public M build() {
        return _mapper;
    }

    /*
    /**********************************************************************
    /* Accessors, features
    /**********************************************************************
     */

    public boolean isEnabled(MapperFeature f) {
        return _mapper.isEnabled(f);
    }
    public boolean isEnabled(DeserializationFeature f) {
        return _mapper.isEnabled(f);
    }
    public boolean isEnabled(SerializationFeature f) {
        return _mapper.isEnabled(f);
    }

    public boolean isEnabled(JsonParser.Feature f) {
        return _mapper.isEnabled(f);
    }
    public boolean isEnabled(JsonGenerator.Feature f) {
        return _mapper.isEnabled(f);
    }

    /*
    /**********************************************************************
    /* Accessors, other
    /**********************************************************************
     */

    public TokenStreamFactory streamFactory() {
        return _mapper.tokenStreamFactory();
    }

    /*
    /**********************************************************************
    /* Changing features: mapper, ser, deser
    /**********************************************************************
     */

    @SuppressWarnings("deprecation")
    public B enable(MapperFeature... features) {
        _mapper.enable(features);
        return _this();
    }

    @SuppressWarnings("deprecation")
    public B disable(MapperFeature... features) {
        _mapper.disable(features);
        return _this();
    }

    @SuppressWarnings("deprecation")
    public B configure(MapperFeature feature, boolean state) {
        _mapper.configure(feature, state);
        return _this();
    }

    public B enable(SerializationFeature... features) {
        for (SerializationFeature f : features) {
            _mapper.enable(f);
        }
        return _this();
    }

    public B disable(SerializationFeature... features) {
        for (SerializationFeature f : features) {
            _mapper.disable(f);
        }
        return _this();
    }

    public B configure(SerializationFeature feature, boolean state) {
        _mapper.configure(feature, state);
        return _this();
    }

    public B enable(DeserializationFeature... features) {
        for (DeserializationFeature f : features) {
            _mapper.enable(f);
        }
        return _this();
    }

    public B disable(DeserializationFeature... features) {
        for (DeserializationFeature f : features) {
            _mapper.disable(f);
        }
        return _this();
    }

    public B configure(DeserializationFeature feature, boolean state) {
        _mapper.configure(feature, state);
        return _this();
    }

    /*
    /**********************************************************************
    /* Changing features: parser, generator, pre-2.10
    /**********************************************************************
     */

    public B enable(JsonParser.Feature... features) {
        _mapper.enable(features);
        return _this();
    }

    public B disable(JsonParser.Feature... features) {
        _mapper.disable(features);
        return _this();
    }

    public B configure(JsonParser.Feature feature, boolean state) {
        _mapper.configure(feature, state);
        return _this();
    }

    public B enable(JsonGenerator.Feature... features) {
        _mapper.enable(features);
        return _this();
    }

    public B disable(JsonGenerator.Feature... features) {
        _mapper.disable(features);
        return _this();
    }

    public B configure(JsonGenerator.Feature feature, boolean state) {
        _mapper.configure(feature, state);
        return _this();
    }

    /*
    /**********************************************************************
    /* Changing features: parser, generator, 2.10+
    /**********************************************************************
     */

    public B enable(StreamReadFeature... features) {
        for (StreamReadFeature f : features) {
            _mapper.enable(f.mappedFeature());
        }
        return _this();
    }

    public B disable(StreamReadFeature... features) {
        for (StreamReadFeature f : features) {
            _mapper.disable(f.mappedFeature());
        }
        return _this();
    }

    public B configure(StreamReadFeature feature, boolean state) {
        _mapper.configure(feature.mappedFeature(), state);
        return _this();
    }

    public B enable(StreamWriteFeature... features) {
        for (StreamWriteFeature f : features) {
            _mapper.enable(f.mappedFeature());
        }
        return _this();
    }

    public B disable(StreamWriteFeature... features) {
        for (StreamWriteFeature f : features) {
            _mapper.disable(f.mappedFeature());
        }
        return _this();
    }

    public B configure(StreamWriteFeature feature, boolean state) {
        _mapper.configure(feature.mappedFeature(), state);
        return _this();
    }

    /*
    /**********************************************************************
    /* Module registration, discovery, access
    /**********************************************************************
     */

    public B addModule(com.fasterxml.jackson.databind.Module module)
    {
        _mapper.registerModule(module);
        return _this();
    }

    public B addModules(com.fasterxml.jackson.databind.Module... modules)
    {
        for (com.fasterxml.jackson.databind.Module module : modules) {
            addModule(module);
        }
        return _this();
    }

    public B addModules(Iterable<? extends com.fasterxml.jackson.databind.Module> modules)
    {
        for (com.fasterxml.jackson.databind.Module module : modules) {
            addModule(module);
        }
        return _this();
    }

    /**
     * Method for locating available methods, using JDK {@link ServiceLoader}
     * facility, along with module-provided SPI.
     *<p>
     * Note that method does not do any caching, so calls should be considered
     * potentially expensive.
     */
    public static List<com.fasterxml.jackson.databind.Module> findModules() {
        return findModules(null);
    }

    /**
     * Method for locating available methods, using JDK {@link ServiceLoader}
     * facility, along with module-provided SPI.
     *<p>
     * Note that method does not do any caching, so calls should be considered
     * potentially expensive.
     */
    public static List<com.fasterxml.jackson.databind.Module> findModules(ClassLoader classLoader)
    {
        ArrayList<com.fasterxml.jackson.databind.Module> modules = new ArrayList<>();
        ServiceLoader<com.fasterxml.jackson.databind.Module> loader = secureGetServiceLoader(com.fasterxml.jackson.databind.Module.class, classLoader);
        for (com.fasterxml.jackson.databind.Module module : loader) {
            modules.add(module);
        }
        return modules;
    }

    private static <T> ServiceLoader<T> secureGetServiceLoader(final Class<T> clazz, final ClassLoader classLoader) {
        final SecurityManager sm = System.getSecurityManager();
        if (sm == null) {
            return (classLoader == null) ?
                    ServiceLoader.load(clazz) : ServiceLoader.load(clazz, classLoader);
        }
        return AccessController.doPrivileged(new PrivilegedAction<ServiceLoader<T>>() {
            @Override
            public ServiceLoader<T> run() {
                return (classLoader == null) ?
                        ServiceLoader.load(clazz) : ServiceLoader.load(clazz, classLoader);
            }
        });
    }

    /**
     * Convenience method that is functionally equivalent to:
     *<code>
     *   addModules(builder.findModules());
     *</code>
     *<p>
     * As with {@link #findModules()}, no caching is done for modules, so care
     * needs to be taken to either create and share a single mapper instance;
     * or to cache introspected set of modules.
     */
    public B findAndAddModules() {
        return addModules(findModules());
    }

    /*
    /**********************************************************************
    /* Changing base settings
    /**********************************************************************
     */

    /**
     * Method for replacing {@link AnnotationIntrospector} used by the
     * mapper instance to be built.
     * Note that doing this will replace the current introspector, which
     * may lead to unavailability of core Jackson annotations.
     * If you want to combine handling of multiple introspectors,
     * have a look at {@link com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair}.
     *
     * @see com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair
     */
    public B annotationIntrospector(AnnotationIntrospector intr) {
        _mapper.setAnnotationIntrospector(intr);
        return _this();
    }

    public B nodeFactory(JsonNodeFactory f) {
        _mapper.setNodeFactory(f);
        return _this();
    }

    /*
    /**********************************************************************
    /* Changing introspection helpers
    /**********************************************************************
     */

    public B typeFactory(TypeFactory f) {
        _mapper.setTypeFactory(f);
        return _this();
    }

    public B subtypeResolver(SubtypeResolver r) {
        _mapper.setSubtypeResolver(r);
        return _this();
    }

    public B visibility(VisibilityChecker<?> vc) {
        _mapper.setVisibility(vc);
        return _this();
    }

    public B visibility(PropertyAccessor forMethod, JsonAutoDetect.Visibility visibility) {
        _mapper.setVisibility(forMethod, visibility);
        return _this();
    }

    /**
     * Method for configuring {@link HandlerInstantiator} to use for creating
     * instances of handlers (such as serializers, deserializers, type and type
     * id resolvers), given a class.
     *
     * @param hi Instantiator to use; if null, use the default implementation
     */
    public B handlerInstantiator(HandlerInstantiator hi) {
        _mapper.setHandlerInstantiator(hi);
        return _this();
    }

    public B propertyNamingStrategy(PropertyNamingStrategy s) {
        _mapper.setPropertyNamingStrategy(s);
        return _this();
    }

    /*
    /**********************************************************************
    /* Changing factories, serialization
    /**********************************************************************
     */

    public B serializerFactory(SerializerFactory f) {
        _mapper.setSerializerFactory(f);
        return _this();
    }

    /**
     * Method for configuring this mapper to use specified {@link FilterProvider} for
     * mapping Filter Ids to actual filter instances.
     *<p>
     * Note that usually it is better to use method in {@link ObjectWriter}, but sometimes
     * this method is more convenient. For example, some frameworks only allow configuring
     * of ObjectMapper instances and not {@link ObjectWriter}s.
     */
    public B filterProvider(FilterProvider prov) {
        _mapper.setFilterProvider(prov);
        return _this();
    }

    public B defaultPrettyPrinter(PrettyPrinter pp) {
        _mapper.setDefaultPrettyPrinter(pp);
        return _this();
    }

    /*
    /**********************************************************************
    /* Changing factories, related, deserialization
    /**********************************************************************
     */

    public B injectableValues(InjectableValues v) {
        _mapper.setInjectableValues(v);
        return _this();
    }

    /**
     * Method used for adding a {@link DeserializationProblemHandler} for this
     * builder, at the head of the list (meaning it has priority over handler
     * registered earlier).
     */
    public B addHandler(DeserializationProblemHandler h) {
        _mapper.addHandler(h);
        return _this();
    }

    /**
     * Method that may be used to remove all {@link DeserializationProblemHandler}s added
     * to this builder (if any).
     */
    public B clearProblemHandlers() {
        _mapper.clearProblemHandlers();
        return _this();
    }

    /*
    /**********************************************************************
    /* Changing settings, date/time
    /**********************************************************************
     */

    /**
     * Method for configuring the default {@link DateFormat} to use when serializing time
     * values as Strings, and deserializing from JSON Strings.
     * If you need per-request configuration, factory methods in
     * {@link ObjectReader} and {@link ObjectWriter} instead.
     */
    public B defaultDateFormat(DateFormat df) {
        _mapper.setDateFormat(df);
        return _this();
    }

    /**
     * Method for overriding default TimeZone to use for formatting.
     * Default value used is UTC (NOT default TimeZone of JVM).
     */
    public B defaultTimeZone(TimeZone tz) {
        _mapper.setTimeZone(tz);
        return _this();
    }

    /**
     * Method for overriding default locale to use for formatting.
     * Default value used is {@link Locale#getDefault()}.
     */
    public B defaultLocale(Locale locale) {
        _mapper.setLocale(locale);
        return _this();
    }

    /*
    /**********************************************************************
    /* Changing settings, formatting
    /**********************************************************************
     */

    /**
     * Method that will configure default {@link Base64Variant} that
     * <code>byte[]</code> serializers and deserializers will use.
     * 
     * @param v Base64 variant to use
     * 
     * @return This mapper, for convenience to allow chaining
     */
    public B defaultBase64Variant(Base64Variant v) {
        _mapper.setBase64Variant(v);
        return _this();
    }

    public B serializationInclusion(JsonInclude.Include incl) {
        _mapper.setSerializationInclusion(incl);
        return _this();
    }

    /*
    /**********************************************************************
    /* Adding Mix-ins
    /**********************************************************************
     */

    /**
     * Method to use for defining mix-in annotations to use for augmenting
     * annotations that classes have, for purpose of configuration serialization
     * and/or deserialization processing.
     * Mixing in is done when introspecting class annotations and properties.
     * Annotations from "mixin" class (and its supertypes)
     * will <b>override</b>
     * annotations that target classes (and their super-types) have.
     *<p>
     * Note that standard mixin handler implementations will only allow a single mix-in
     * source class per target, so if there was a previous mix-in defined target it will
     * be cleared. This also means that you can remove mix-in definition by specifying
     * {@code mixinSource} of {@code null}
     */
    public B addMixIn(Class<?> target, Class<?> mixinSource)
    {
        _mapper.addMixIn(target, mixinSource);
        return _this();
    }

    /*
    /**********************************************************************
    /* Subtype registration
    /**********************************************************************
     */

    public B registerSubtypes(Class<?>... subtypes) {
        _mapper.registerSubtypes(subtypes);
        return _this();
    }

    public B registerSubtypes(NamedType... subtypes) {
        _mapper.registerSubtypes(subtypes);
        return _this();
    }

    public B registerSubtypes(Collection<Class<?>> subtypes) {
        _mapper.registerSubtypes(subtypes);
        return _this();
    }

    /*
    /**********************************************************************
    /* Other helper methods
    /**********************************************************************
     */

    // silly convenience cast method we need
    @SuppressWarnings("unchecked")
    protected final B _this() { return (B) this; }
}
