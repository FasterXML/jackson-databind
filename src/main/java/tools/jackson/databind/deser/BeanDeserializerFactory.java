package tools.jackson.databind.deser;

import java.lang.reflect.Modifier;
import java.util.*;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonPOJOBuilder;
import tools.jackson.databind.cfg.DeserializerFactoryConfig;
import tools.jackson.databind.deser.bean.BeanDeserializer;
import tools.jackson.databind.deser.impl.*;
import tools.jackson.databind.deser.jdk.ThrowableDeserializer;
import tools.jackson.databind.exc.InvalidDefinitionException;
import tools.jackson.databind.introspect.*;
import tools.jackson.databind.jsontype.TypeDeserializer;
import tools.jackson.databind.jsontype.impl.SubTypeValidator;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.util.BeanUtil;
import tools.jackson.databind.util.ClassUtil;
import tools.jackson.databind.util.IgnorePropertiesUtil;
import tools.jackson.databind.util.SimpleBeanPropertyDefinition;

/**
 * Concrete deserializer factory class that adds full Bean deserializer
 * construction logic using class introspection.
 * Note that factories specifically do not implement any form of caching:
 * aside from configuration they are stateless; caching is implemented
 * by other components.
 *<p>
 * Instances of this class are fully immutable as all configuration is
 * done by using "fluent factories" (methods that construct new factory
 * instances with different configuration, instead of modifying instance).
 */
public class BeanDeserializerFactory
    extends BasicDeserializerFactory
    implements java.io.Serializable // since 2.1
{
    private static final long serialVersionUID = 1;

    /**
     * Signature of <b>Throwable.initCause</b> method.
     */
    private final static Class<?>[] INIT_CAUSE_PARAMS = new Class<?>[] { Throwable.class };

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    /**
     * Globally shareable thread-safe instance which has no additional custom deserializers
     * registered
     */
    public final static BeanDeserializerFactory instance = new BeanDeserializerFactory(
            new DeserializerFactoryConfig());

    public BeanDeserializerFactory(DeserializerFactoryConfig config) {
        super(config);
    }

    /**
     * Method used by module registration functionality, to construct a new bean
     * deserializer factory
     * with different configuration settings.
     */
    @Override
    public DeserializerFactory withConfig(DeserializerFactoryConfig config)
    {
        if (_factoryConfig == config) {
            return this;
        }
        /* 22-Nov-2010, tatu: Handling of subtypes is tricky if we do immutable-with-copy-ctor;
         *    and we pretty much have to here either choose between losing subtype instance
         *    when registering additional deserializers, or losing deserializers.
         *    Instead, let's actually just throw an error if this method is called when subtype
         *    has not properly overridden this method; this to indicate problem as soon as possible.
         */
        ClassUtil.verifyMustOverride(BeanDeserializerFactory.class, this, "withConfig");
        return new BeanDeserializerFactory(config);
    }

    /*
    /**********************************************************
    /* DeserializerFactory API implementation
    /**********************************************************
     */

    /**
     * Method that called to create a new deserializer for types other than Collections,
     * Maps, arrays, referential types or enums, or "well-known" JDK scalar types.
     */
    @SuppressWarnings("unchecked")
    @Override
    public ValueDeserializer<Object> createBeanDeserializer(DeserializationContext ctxt,
            JavaType type, BeanDescription.Supplier beanDescRef)
    {
        final DeserializationConfig config = ctxt.getConfig();
        // First: we may also have custom overrides:
        ValueDeserializer<?> deser = _findCustomBeanDeserializer(type, config, beanDescRef);
        if (deser != null) {
            // [databind#2392]
            if (_factoryConfig.hasDeserializerModifiers()) {
                for (ValueDeserializerModifier mod : _factoryConfig.deserializerModifiers()) {
                    deser = mod.modifyDeserializer(ctxt.getConfig(), beanDescRef, deser);
                }
            }
            return (ValueDeserializer<Object>) deser;
        }
        // One more thing to check: do we have an exception type (Throwable or its
        // sub-classes)? If so, need slightly different handling.
        if (type.isThrowable()) {
            return buildThrowableDeserializer(ctxt, type, beanDescRef);
        }
        // Or, for abstract types, may have alternate means for resolution
        // (defaulting, materialization)

        // 29-Nov-2015, tatu: Also, filter out calls to primitive types, they are
        //    not something we could materialize anything for
        if (type.isAbstract() && !type.isPrimitive() && !type.isEnumType()) {
            // Let's make it possible to materialize abstract types.
            JavaType concreteType = materializeAbstractType(ctxt, type, beanDescRef);
            if (concreteType != null) {
                // important: introspect actual implementation (abstract class or
                // interface doesn't have constructors, for one)
                beanDescRef = ctxt.lazyIntrospectBeanDescription(concreteType);
                return buildBeanDeserializer(ctxt, concreteType, beanDescRef);
            }
        }
        // Otherwise, may want to check handlers for standard types, from superclass:
        deser = findStdDeserializer(ctxt, type, beanDescRef);
        if (deser != null) {
            return (ValueDeserializer<Object>)deser;
        }

        // Otherwise: could the class be a Bean class? If not, bail out
        if (!isPotentialBeanType(type.getRawClass())) {
            return null;
        }
        // For checks like [databind#1599]
        _validateSubType(ctxt, type, beanDescRef);

        // 05-May-2020, tatu: [databind#2683] Let's actually pre-emptively catch
        //   certain types (for now, java.time.*) to give better error messages
        deser = _findUnsupportedTypeDeserializer(ctxt, type, beanDescRef);
        if (deser != null) {
            return (ValueDeserializer<Object>)deser;
        }

        // Use generic bean introspection to build deserializer
        return buildBeanDeserializer(ctxt, type, beanDescRef);
    }

    @Override
    public ValueDeserializer<Object> createBuilderBasedDeserializer(
            DeserializationContext ctxt, JavaType valueType,
            BeanDescription.Supplier valueBeanDescRef,
            Class<?> builderClass)
    {
        // First: need a BeanDescription for builder class
        JavaType builderType;
        if (ctxt.isEnabled(MapperFeature.INFER_BUILDER_TYPE_BINDINGS)) {
            builderType = ctxt.getTypeFactory().constructParametricType(builderClass, valueType.getBindings());
        } else {
            builderType = ctxt.constructType(builderClass);
        }
        BeanDescription.Supplier builderDescRef = ctxt.lazyIntrospectBeanDescriptionForBuilder(builderType,
                valueBeanDescRef.get());
        // 20-Aug-2020, tatu: May want to change at some point to pass "valueBeanDesc"
        //    too; no urgent need at this point
        return buildBuilderBasedDeserializer(ctxt, valueType, builderDescRef);
    }

    /**
     * Method called by {@link BeanDeserializerFactory} to see if there might be a standard
     * deserializer registered for given type.
     */
    protected ValueDeserializer<?> findStdDeserializer(DeserializationContext ctxt,
            JavaType type, BeanDescription.Supplier beanDescRef)
    {
        // note: we do NOT check for custom deserializers here, caller has already
        // done that
        ValueDeserializer<?> deser = findDefaultDeserializer(ctxt, type, beanDescRef);
        // Also: better ensure these are post-processable?
        if (deser != null) {
            if (_factoryConfig.hasDeserializerModifiers()) {
                for (ValueDeserializerModifier mod : _factoryConfig.deserializerModifiers()) {
                    deser = mod.modifyDeserializer(ctxt.getConfig(), beanDescRef, deser);
                }
            }
        }
        return deser;
    }

    /**
     * Helper method called to see if given type, otherwise to be taken as POJO type,
     * is "known but not supported" JDK type, and if so, return alternate handler
     * (deserializer).
     * Initially added to support more meaningful error messages when "Java 8 date/time"
     * support module not registered.
     */
    protected ValueDeserializer<Object> _findUnsupportedTypeDeserializer(DeserializationContext ctxt,
            JavaType type, BeanDescription.Supplier beanDescRef)
    {
        // 05-May-2020, tatu: Should we check for possible Shape override to "POJO"?
        //   (to let users force 'serialize-as-POJO'? Or not?
        final String errorMsg = BeanUtil.checkUnsupportedType(ctxt.getConfig(), type);
        if (errorMsg != null) {
            // 30-Sep-2020, tatu: [databind#2867] Avoid checks if there is a mix-in
            //    which likely providers a handler...
            if (ctxt.getConfig().findMixInClassFor(type.getRawClass()) == null) {
                return new UnsupportedTypeDeserializer(type, errorMsg);
            }
        }
        return null;
    }

    protected JavaType materializeAbstractType(DeserializationContext ctxt,
            JavaType type, BeanDescription.Supplier beanDescRef)
    {
        final DeserializationConfig config = ctxt.getConfig();
        // May have multiple resolvers, call in precedence order until one returns non-null
        for (AbstractTypeResolver r : config.abstractTypeResolvers()) {
            JavaType concrete = r.resolveAbstractType(config, beanDescRef);
            if (concrete != null) {
                return concrete;
            }
        }
        return null;
    }

    /*
    /**********************************************************
    /* Public construction method beyond DeserializerFactory API:
    /* can be called from outside as well as overridden by
    /* sub-classes
    /**********************************************************
     */

    /**
     * Method that is to actually build a bean deserializer instance.
     * All basic sanity checks have been done to know that what we have
     * may be a valid bean type, and that there are no default simple
     * deserializers.
     */
    @SuppressWarnings("unchecked")
    public ValueDeserializer<Object> buildBeanDeserializer(DeserializationContext ctxt,
            JavaType type, BeanDescription.Supplier beanDescRef)
    {
        // First: check what creators we can use, if any
        ValueInstantiator valueInstantiator;
        /* 04-Jun-2015, tatu: To work around [databind#636], need to catch the
         *    issue, defer; this seems like a reasonable good place for now.
         *   Note, however, that for non-Bean types (Collections, Maps) this
         *   probably won't work and needs to be added elsewhere.
         */
        try {
            valueInstantiator = findValueInstantiator(ctxt, beanDescRef);
        } catch (NoClassDefFoundError error) {
            return new ErrorThrowingDeserializer(error);
        } catch (IllegalArgumentException e0) {
            // 05-Apr-2017, tatu: Although it might appear cleaner to require collector
            //   to throw proper exception, it doesn't actually have reference to this
            //   instance so...
            throw InvalidDefinitionException.from(ctxt.getParser(),
                    ClassUtil.exceptionMessage(e0),
                    beanDescRef, null)
                .withCause(e0);
        }
        BeanDeserializerBuilder builder = constructBeanDeserializerBuilder(ctxt, beanDescRef);
        builder.setValueInstantiator(valueInstantiator);
         // And then setters for deserializing from JSON Object
        addBeanProps(ctxt, beanDescRef, builder);
        addObjectIdReader(ctxt, beanDescRef, builder);

        // managed/back reference fields/setters need special handling... first part
        addBackReferenceProperties(ctxt, beanDescRef, builder);
        addInjectables(ctxt, beanDescRef, builder);

        final DeserializationConfig config = ctxt.getConfig();
        if (_factoryConfig.hasDeserializerModifiers()) {
            for (ValueDeserializerModifier mod : _factoryConfig.deserializerModifiers()) {
                builder = mod.updateBuilder(config, beanDescRef, builder);
            }
        }
        ValueDeserializer<?> deserializer;

        if (type.isAbstract() && !valueInstantiator.canInstantiate()) {
            deserializer = builder.buildAbstract();
        } else {
            deserializer = builder.build();
        }
        // may have modifier(s) that wants to modify or replace serializer we just built
        // (note that `resolve()` and `createContextual()` called later on)
        if (_factoryConfig.hasDeserializerModifiers()) {
            for (ValueDeserializerModifier mod : _factoryConfig.deserializerModifiers()) {
                deserializer = mod.modifyDeserializer(config, beanDescRef, deserializer);
            }
        }
        return (ValueDeserializer<Object>) deserializer;
    }

    /**
     * Method for constructing a bean deserializer that uses specified
     * intermediate Builder for binding data, and construction of the
     * value instance.
     * Note that implementation is mostly copied from the regular
     * BeanDeserializer build method.
     */
    @SuppressWarnings("unchecked")
    protected ValueDeserializer<Object> buildBuilderBasedDeserializer(
    		DeserializationContext ctxt, JavaType valueType,
    		BeanDescription.Supplier builderDescRef)
    {
        // Creators, anyone? (to create builder itself)
        ValueInstantiator valueInstantiator;
        try {
            valueInstantiator = findValueInstantiator(ctxt, builderDescRef);
        } catch (NoClassDefFoundError error) {
            return new ErrorThrowingDeserializer(error);
        } catch (IllegalArgumentException e) {
            // 05-Apr-2017, tatu: Although it might appear cleaner to require collector
            //   to throw proper exception, it doesn't actually have reference to this
            //   instance so...
            throw InvalidDefinitionException.from(ctxt.getParser(),
                    ClassUtil.exceptionMessage(e),
                    builderDescRef, null);
        }
        final DeserializationConfig config = ctxt.getConfig();
        BeanDeserializerBuilder builder = constructBeanDeserializerBuilder(ctxt, builderDescRef);
        builder.setValueInstantiator(valueInstantiator);
         // And then "with methods" for deserializing from JSON Object
        addBeanProps(ctxt, builderDescRef, builder);
        addObjectIdReader(ctxt, builderDescRef, builder);

        // managed/back reference fields/setters need special handling... first part
        addBackReferenceProperties(ctxt, builderDescRef, builder);
        addInjectables(ctxt, builderDescRef, builder);

        JsonPOJOBuilder.Value builderConfig = builderDescRef.get().findPOJOBuilderConfig();
        final String buildMethodName = (builderConfig == null) ?
                JsonPOJOBuilder.DEFAULT_BUILD_METHOD : builderConfig.buildMethodName;

        // and lastly, find build method to use:
        AnnotatedMethod buildMethod = builderDescRef.get().findMethod(buildMethodName, null);
        if (buildMethod != null) { // note: can't yet throw error; may be given build method
            if (config.canOverrideAccessModifiers()) {
            	ClassUtil.checkAndFixAccess(buildMethod.getMember(), config.isEnabled(MapperFeature.OVERRIDE_PUBLIC_ACCESS_MODIFIERS));
            }
        }
        builder.setPOJOBuilder(buildMethod, builderConfig);
        // this may give us more information...
        if (_factoryConfig.hasDeserializerModifiers()) {
            for (ValueDeserializerModifier mod : _factoryConfig.deserializerModifiers()) {
                builder = mod.updateBuilder(config, builderDescRef, builder);
            }
        }
        ValueDeserializer<?> deserializer = builder.buildBuilderBased(
        		valueType, buildMethodName);

        // [JACKSON-440]: may have modifier(s) that wants to modify or replace serializer we just built:
        if (_factoryConfig.hasDeserializerModifiers()) {
            for (ValueDeserializerModifier mod : _factoryConfig.deserializerModifiers()) {
                deserializer = mod.modifyDeserializer(config, builderDescRef, deserializer);
            }
        }
        return (ValueDeserializer<Object>) deserializer;
    }

    protected void addObjectIdReader(DeserializationContext ctxt,
            BeanDescription.Supplier beanDescRef, BeanDeserializerBuilder builder)
    {
        ObjectIdInfo objectIdInfo = beanDescRef.get().getObjectIdInfo();
        if (objectIdInfo == null) {
            return;
        }
        Class<?> implClass = objectIdInfo.getGeneratorType();
        JavaType idType;
        SettableBeanProperty idProp;
        ObjectIdGenerator<?> gen;

        ObjectIdResolver resolver = ctxt.objectIdResolverInstance(beanDescRef.getClassInfo(), objectIdInfo);

        // Just one special case: Property-based generator is trickier
        if (implClass == ObjectIdGenerators.PropertyGenerator.class) { // most special one, needs extra work
            PropertyName propName = objectIdInfo.getPropertyName();
            idProp = builder.findProperty(propName);
            if (idProp == null) {
                throw new IllegalArgumentException(String.format(
"Invalid Object Id definition for %s: cannot find property with name %s",
ClassUtil.getTypeDescription(beanDescRef.getType()),
ClassUtil.name(propName)));
            }
            idType = idProp.getType();
            gen = new PropertyBasedObjectIdGenerator(objectIdInfo.getScope());
        } else {
            JavaType type = ctxt.constructType(implClass);
            idType = ctxt.getTypeFactory().findTypeParameters(type, ObjectIdGenerator.class)[0];
            idProp = null;
            gen = ctxt.objectIdGeneratorInstance(beanDescRef.getClassInfo(), objectIdInfo);
        }
        // also: unlike with value deserializers, let's just resolve one we need here
        ValueDeserializer<?> deser = ctxt.findRootValueDeserializer(idType);
        builder.setObjectIdReader(ObjectIdReader.construct(idType,
                objectIdInfo.getPropertyName(), gen, deser, idProp, resolver));
    }

    @SuppressWarnings("unchecked")
    public ValueDeserializer<Object> buildThrowableDeserializer(DeserializationContext ctxt,
            JavaType type, BeanDescription.Supplier beanDescRef)
    {
        final DeserializationConfig config = ctxt.getConfig();
        // first: construct like a regular bean deserializer...
        BeanDeserializerBuilder builder = constructBeanDeserializerBuilder(ctxt, beanDescRef);
        builder.setValueInstantiator(findValueInstantiator(ctxt, beanDescRef));

        addBeanProps(ctxt, beanDescRef, builder);
        // (and assume there won't be any back references)

        // But then let's decorate things a bit
        // Need to add "initCause" as setter for exceptions (sub-classes of Throwable).
        // 26-May-2022, tatu: [databind#3275] Looks like JDK 12 added "setCause()"
        //    which can wreak havoc, at least with NamingStrategy
        Iterator<SettableBeanProperty> it = builder.getProperties();
        while (it.hasNext()) {
            SettableBeanProperty prop = it.next();
            if ("setCause".equals(prop.getMember().getName())) {
                // For now this is allowed as we are returned "live" Iterator...
                it.remove();
                break;
            }
        }
        AnnotatedMethod am = beanDescRef.get().findMethod("initCause", INIT_CAUSE_PARAMS);
        if (am != null) { // should never be null
            SettableBeanProperty causeCreatorProp = builder.findProperty(PropertyName.construct("cause"));
            // [databind#4827] : Consider case where sub-classed `Exception` has `JsonCreator` with `cause` parameter
            if (causeCreatorProp instanceof CreatorProperty) {
                // Set fallback-setter as null, so `fixAccess()` does not happen during build
                ((CreatorProperty) causeCreatorProp).setFallbackSetter(null);
            } else {
                // [databind#3497]: must consider possible PropertyNamingStrategy
                String name = "cause";
                PropertyNamingStrategy pts = config.getPropertyNamingStrategy();
                if (pts != null) {
                    name = pts.nameForSetterMethod(config, am, "cause");
                }
                SimpleBeanPropertyDefinition propDef = SimpleBeanPropertyDefinition.construct(ctxt.getConfig(), am,
                        new PropertyName(name));
                SettableBeanProperty prop = constructSettableProperty(ctxt, beanDescRef, propDef,
                        am.getParameterType(0));
                if (prop != null) {
                    // 21-Aug-2011, tatus: We may actually have found 'cause' property
                    //   to set... but let's replace it just in case, otherwise can end up with odd errors.
                    builder.addOrReplaceProperty(prop, true);
                }
            }
        }
        // update builder now that all information is in?
        if (_factoryConfig.hasDeserializerModifiers()) {
            for (ValueDeserializerModifier mod : _factoryConfig.deserializerModifiers()) {
                builder = mod.updateBuilder(config, beanDescRef, builder);
            }
        }
        ValueDeserializer<?> deserializer = builder.build();

        // At this point it ought to be a BeanDeserializer; if not, must assume
        // it's some other thing that can handle deserialization ok...
        if (deserializer instanceof BeanDeserializer) {
            deserializer = ThrowableDeserializer.construct(ctxt, (BeanDeserializer) deserializer);
        }

        // may have modifier(s) that wants to modify or replace serializer we just built:
        if (_factoryConfig.hasDeserializerModifiers()) {
            for (ValueDeserializerModifier mod : _factoryConfig.deserializerModifiers()) {
                deserializer = mod.modifyDeserializer(config, beanDescRef, deserializer);
            }
        }
        return (ValueDeserializer<Object>) deserializer;
    }

    /*
    /**********************************************************************
    /* Helper methods for Bean deserializer construction
    /**********************************************************************
     */

    /**
     * Overridable method that constructs a {@link BeanDeserializerBuilder}
     * which is used to accumulate information needed to create deserializer
     * instance.
     */
    protected BeanDeserializerBuilder constructBeanDeserializerBuilder(DeserializationContext ctxt,
            BeanDescription.Supplier beanDescRef) {
        return new BeanDeserializerBuilder(beanDescRef, ctxt);
    }

    /**
     * Method called to figure out settable properties for the
     * bean deserializer to use.
     *<p>
     * Note: designed to be overridable, and effort is made to keep interface
     * similar between versions.
     */
    protected void addBeanProps(DeserializationContext ctxt,
            BeanDescription.Supplier beanDescRef, BeanDeserializerBuilder builder)
    {
        final BeanDescription beanDesc = beanDescRef.get();
        final ValueInstantiator valueInstantiator = builder.getValueInstantiator();
        final SettableBeanProperty[] creatorProps = (valueInstantiator != null)
                ? valueInstantiator.getFromObjectArguments(ctxt.getConfig())
                : null;
        final boolean hasCreatorProps = (creatorProps != null);

        // 01-May-2016, tatu: Which base type to use here gets tricky, since
        //   it may often make most sense to use general type for overrides,
        //   but what we have here may be more specific impl type. But for now
        //   just use it as is.
        JsonIgnoreProperties.Value ignorals = ctxt.getConfig()
                .getDefaultPropertyIgnorals(beanDesc.getBeanClass(),
                        beanDesc.getClassInfo());
        Set<String> ignored;
        if (ignorals != null) {
            boolean ignoreAny = ignorals.getIgnoreUnknown();
            builder.setIgnoreUnknownProperties(ignoreAny);
            // Or explicit/implicit definitions?
            ignored = ignorals.findIgnoredForDeserialization();
            for (String propName : ignored) {
                builder.addIgnorable(propName);
            }
        } else {
            ignored = Collections.emptySet();
        }
        JsonIncludeProperties.Value inclusions = ctxt.getConfig()
                .getDefaultPropertyInclusions(beanDesc.getBeanClass(),
                        beanDesc.getClassInfo());
        Set<String> included = null;
        if (inclusions != null) {
            included = inclusions.getIncluded();
            if (included != null) {
                for(String propName : included) {
                    builder.addIncludable(propName);
                }
            }
        }

        // Also, do we have a fallback "any" setter?
        SettableAnyProperty anySetter = _resolveAnySetter(ctxt, beanDescRef, creatorProps);
        if (anySetter != null) {
            builder.setAnySetter(anySetter);
        } else {
            // 23-Jan-2018, tatu: although [databind#1805] would suggest we should block
            //   properties regardless, for now only consider unless there's any setter...
            Collection<String> ignored2 = beanDesc.getIgnoredPropertyNames();
            if (ignored2 != null) {
                for (String propName : ignored2) {
                    // allow ignoral of similarly named JSON property, but do not force;
                    // latter means NOT adding this to 'ignored':
                    builder.addIgnorable(propName);
                }
            }
        }
        final boolean useGettersAsSetters = ctxt.isEnabled(MapperFeature.USE_GETTERS_AS_SETTERS);
        // 24-Sep-2017, tatu: Legacy setting removed from 3.x, not sure if other visibility checks
        //    should be checked?
        // && ctxt.isEnabled(MapperFeature.AUTO_DETECT_GETTERS);

        // Ok: let's then filter out property definitions
        List<BeanPropertyDefinition> propDefs = filterBeanProps(ctxt,
                beanDescRef, builder, beanDesc.findProperties(), ignored, included);
        // After which we can let custom code change the set
        if (_factoryConfig.hasDeserializerModifiers()) {
            for (ValueDeserializerModifier mod : _factoryConfig.deserializerModifiers()) {
                propDefs = mod.updateProperties(ctxt.getConfig(), beanDescRef, propDefs);
            }
        }

        // At which point we still have all kinds of properties; not all with mutators:
        for (BeanPropertyDefinition propDef : propDefs) {
            SettableBeanProperty prop = null;

            // 18-Oct-2013, tatu: Although constructor parameters have highest precedence,
            //   we need to do linkage (as per [databind#318]), and so need to start with
            //   other types, and only then create constructor parameter, if any.
            if (propDef.hasSetter()) {
                AnnotatedMethod setter = propDef.getSetter();
                JavaType propertyType = setter.getParameterType(0);
                prop = constructSettableProperty(ctxt, beanDescRef, propDef, propertyType);
            } else if (propDef.hasField()) {
                AnnotatedField field = propDef.getField();
                JavaType propertyType = field.getType();
                prop = constructSettableProperty(ctxt, beanDescRef, propDef, propertyType);
            } else {
                // NOTE: specifically getter, since field was already checked above
                AnnotatedMethod getter = propDef.getGetter();
                if (getter != null) {
                    if (useGettersAsSetters && _isSetterlessType(getter.getRawType())) {
                        // 23-Jan-2018, tatu: As per [databind#1805], need to ensure we don't
                        //   accidentally sneak in getter-as-setter for `READ_ONLY` properties
                        if (builder.hasIgnorable(propDef.getName())) {
                            ; // skip
                        } else {
                            prop = constructSetterlessProperty(ctxt, beanDesc, propDef);
                        }
                    } else if (!propDef.hasConstructorParameter()) {
                        PropertyMetadata md = propDef.getMetadata();
                        // 25-Oct-2016, tatu: If merging enabled, might not need setter.
                        //   We cannot quite support this with creator parameters; in theory
                        //   possibly, but right not now due to complexities of routing, so
                        //   just prevent
                        if (md.getMergeInfo() != null) {
                            prop = constructSetterlessProperty(ctxt, beanDesc, propDef);
                        }
                    }
                }
            }

            // 25-Sep-2014, tatu: No point in finding constructor parameters for abstract types
            //   (since they are never used anyway)
            if (hasCreatorProps && propDef.hasConstructorParameter()) {
                /* If property is passed via constructor parameter, we must
                 * handle things in special way. Not sure what is the most optimal way...
                 * for now, let's just call a (new) method in builder, which does nothing.
                 */
                // but let's call a method just to allow custom builders to be aware...
                final String name = propDef.getName();
                CreatorProperty cprop = null;

                for (SettableBeanProperty cp : creatorProps) {
                    if (name.equals(cp.getName()) && (cp instanceof CreatorProperty)) {
                        cprop = (CreatorProperty) cp;
                        break;
                    }
                }
                if (cprop == null) {
                    List<String> n = new ArrayList<>();
                    for (SettableBeanProperty cp : creatorProps) {
                        n.add(cp.getName());
                    }
                    ctxt.reportBadPropertyDefinition(beanDesc, propDef,
"Could not find creator property with name %s (known Creator properties: %s)",
                            ClassUtil.name(name), n);
                    continue;
                }
                if (prop != null) {
                    cprop.setFallbackSetter(prop);
                }
                Class<?>[] views = propDef.findViews();
                if (views == null) {
                    views = beanDesc.findDefaultViews();
                }
                cprop.setViews(views);
                builder.addCreatorProperty(cprop);
                continue;
            }
            if (prop != null) {
                // one more thing before adding to builder: copy any metadata
                Class<?>[] views = propDef.findViews();
                if (views == null) {
                    views = beanDesc.findDefaultViews();
                }
                prop.setViews(views);
                builder.addProperty(prop);
            }
        }
    }

    // since 2.18
    private SettableAnyProperty _resolveAnySetter(DeserializationContext ctxt,
            BeanDescription.Supplier beanDescRef, SettableBeanProperty[] creatorProps)
    {
        // Look for any-setter via @JsonCreator
        if (creatorProps != null) {
            for (SettableBeanProperty prop : creatorProps) {
                AnnotatedMember member = prop.getMember();
                if (member != null && Boolean.TRUE.equals(ctxt.getAnnotationIntrospector().hasAnySetter(ctxt.getConfig(), member))) {
                    return constructAnySetter(ctxt, beanDescRef, member);
                }
            }
        }
        // else find the regular method/field level any-setter
        AnnotatedMember anySetter = beanDescRef.get().findAnySetterAccessor();
        if (anySetter != null) {
            return constructAnySetter(ctxt, beanDescRef, anySetter);
        }
        // not found, that's fine, too
        return null;
    }

    private boolean _isSetterlessType(Class<?> rawType) {
        // May also need to consider getters
        // for Map/Collection properties; but with lowest precedence
        // should only consider Collections and Maps, for now?
        return Collection.class.isAssignableFrom(rawType)
                || Map.class.isAssignableFrom(rawType);
    }

    /**
     * Helper method called to filter out explicit ignored properties,
     * as well as properties that have "ignorable types".
     * Note that this will not remove properties that have no setters.
     */
    protected List<BeanPropertyDefinition> filterBeanProps(DeserializationContext ctxt,
            BeanDescription.Supplier beanDescRef, BeanDeserializerBuilder builder,
            List<BeanPropertyDefinition> propDefsIn,
            Set<String> ignored,
            Set<String> included)
    {
        ArrayList<BeanPropertyDefinition> result = new ArrayList<BeanPropertyDefinition>(
                Math.max(4, propDefsIn.size()));
        HashMap<Class<?>,Boolean> ignoredTypes = new HashMap<Class<?>,Boolean>();
        // These are all valid setters, but we do need to introspect bit more
        for (BeanPropertyDefinition property : propDefsIn) {
            String name = property.getName();
            // explicit ignoral using @JsonIgnoreProperties of @JsonIncludeProperties needs to block entries
            if (IgnorePropertiesUtil.shouldIgnore(name, ignored, included)) {
                continue;
            }
            if (!property.hasConstructorParameter()) { // never skip constructor params
                Class<?> rawPropertyType = property.getRawPrimaryType();
                // Some types are declared as ignorable as well
                if ((rawPropertyType != null)
                        && isIgnorableType(ctxt, property, rawPropertyType, ignoredTypes)) {
                    // important: make ignorable, to avoid errors if value is actually seen
                    builder.addIgnorable(name);
                    continue;
                }
            }
            result.add(property);
        }
        return result;
    }

    /**
     * Method that will find if bean has any managed- or back-reference properties,
     * and if so add them to bean, to be linked during resolution phase.
     */
    protected void addBackReferenceProperties(DeserializationContext ctxt,
            BeanDescription.Supplier beanDescRef, BeanDeserializerBuilder builder)
    {
        // and then back references, not necessarily found as regular properties
        List<BeanPropertyDefinition> refProps = beanDescRef.get().findBackReferences();
        if (refProps != null) {
            for (BeanPropertyDefinition refProp : refProps) {
                /*
                AnnotatedMember m = refProp.getMutator();
                JavaType type;
                if (m instanceof AnnotatedMethod) {
                    type = ((AnnotatedMethod) m).getParameterType(0);
                } else {
                    type = m.getType();
                    // 30-Mar-2017, tatu: Unfortunately it is not yet possible to make back-refs
                    //    work through constructors; but let's at least indicate the issue for now
                    if (m instanceof AnnotatedParameter) {
                        ctxt.reportBadTypeDefinition(beanDesc,
"Cannot bind back reference using Creator parameter (reference %s, parameter index #%d)",
ClassUtil.name(name), ((AnnotatedParameter) m).getIndex());
                    }
                }
                */
                String refName = refProp.findReferenceName();
                builder.addBackReferenceProperty(refName, constructSettableProperty(ctxt,
                        beanDescRef, refProp, refProp.getPrimaryType()));
            }
        }
    }

    /**
     * Method called locate all members used for value injection (if any),
     * constructor {@link tools.jackson.databind.deser.impl.ValueInjector} instances, and add them to builder.
     */
    protected void addInjectables(DeserializationContext ctxt,
            BeanDescription.Supplier beanDescRef, BeanDeserializerBuilder builder)
    {
        Map<Object, AnnotatedMember> raw = beanDescRef.get().findInjectables();
        if (raw != null) {
            for (Map.Entry<Object, AnnotatedMember> entry : raw.entrySet()) {
                AnnotatedMember m = entry.getValue();
                builder.addInjectable(PropertyName.construct(m.getName()),
                        m.getType(),
                        beanDescRef.getClassAnnotations(), m, entry.getKey());
            }
        }
    }

    /**
     * Method called to construct fallback {@link SettableAnyProperty}
     * for handling unknown bean properties, given a method that
     * has been designated as such setter.
     *
     * @param mutator Either a 2-argument method (setter, with key and value),
     *    or a Field or (as of 2.18) Constructor Parameter of type Map or JsonNode/Object;
     *    either way accessor used for passing "any values"
     */
    @SuppressWarnings("unchecked")
    protected SettableAnyProperty constructAnySetter(DeserializationContext ctxt,
            BeanDescription.Supplier beanDescRef, AnnotatedMember mutator)
    {
        // find the java type based on the annotated setter method or setter field
        BeanProperty prop;
        JavaType keyType;
        JavaType valueType;
        final boolean isField = mutator instanceof AnnotatedField;
        // [databind#562] Allow @JsonAnySetter on Creator constructor
        final boolean isParameter = mutator instanceof AnnotatedParameter;
        int parameterIndex = -1;

        if (mutator instanceof AnnotatedMethod) {
            // we know it's a 2-arg method, second arg is the value
            AnnotatedMethod am = (AnnotatedMethod) mutator;
            keyType = am.getParameterType(0);
            valueType = am.getParameterType(1);
            // Need to resolve for possible generic types (like Maps, Collections)
            valueType = resolveMemberAndTypeAnnotations(ctxt, mutator, valueType);
            prop = new BeanProperty.Std(PropertyName.construct(mutator.getName()),
                    valueType, null, mutator,
                    PropertyMetadata.STD_OPTIONAL);

        } else if (isField) {
            AnnotatedField af = (AnnotatedField) mutator;
            // get the type from the content type of the map object
            JavaType fieldType = af.getType();
            // 31-Jul-2022, tatu: Not just Maps any more but also JsonNode, so:
            if (fieldType.isMapLikeType()) {
                fieldType = resolveMemberAndTypeAnnotations(ctxt, mutator, fieldType);
                keyType = fieldType.getKeyType();
                valueType = fieldType.getContentType();
                prop = new BeanProperty.Std(PropertyName.construct(mutator.getName()),
                        fieldType, null, mutator, PropertyMetadata.STD_OPTIONAL);
            } else if (fieldType.hasRawClass(JsonNode.class)
                    || fieldType.hasRawClass(ObjectNode.class)) {
                fieldType = resolveMemberAndTypeAnnotations(ctxt, mutator, fieldType);
                // Deserialize is individual values of ObjectNode, not full ObjectNode, so:
                valueType = ctxt.constructType(JsonNode.class);
                prop = new BeanProperty.Std(PropertyName.construct(mutator.getName()),
                        fieldType, null, mutator, PropertyMetadata.STD_OPTIONAL);

                // Unlike with more complicated types, here we do not allow any annotation
                // overrides etc but instead short-cut handling:
                return SettableAnyProperty.constructForJsonNodeField(ctxt,
                        prop, mutator, valueType,
                        ctxt.findRootValueDeserializer(valueType));
            } else {
                return ctxt.reportBadDefinition(beanDescRef.getType(), String.format(
                        "Unsupported type for any-setter: %s -- only support `Map`s, `JsonNode` and `ObjectNode` ",
                        ClassUtil.getTypeDescription(fieldType)));
            }
        } else if (isParameter) {
            AnnotatedParameter af = (AnnotatedParameter) mutator;
            JavaType paramType = af.getType();
            parameterIndex = af.getIndex();

            if (paramType.isMapLikeType()) {
                paramType = resolveMemberAndTypeAnnotations(ctxt, mutator, paramType);
                keyType = paramType.getKeyType();
                valueType = paramType.getContentType();
                prop = new BeanProperty.Std(PropertyName.construct(mutator.getName()),
                        paramType, null, mutator, PropertyMetadata.STD_OPTIONAL);
            } else if (paramType.hasRawClass(JsonNode.class) || paramType.hasRawClass(ObjectNode.class)) {
                paramType = resolveMemberAndTypeAnnotations(ctxt, mutator, paramType);
                // Deserialize is individual values of ObjectNode, not full ObjectNode, so:
                valueType = ctxt.constructType(JsonNode.class);
                prop = new BeanProperty.Std(PropertyName.construct(mutator.getName()),
                        paramType, null, mutator, PropertyMetadata.STD_OPTIONAL);

                // Unlike with more complicated types, here we do not allow any annotation
                // overrides etc but instead short-cut handling:
                return SettableAnyProperty.constructForJsonNodeParameter(ctxt, prop, mutator, valueType,
                        ctxt.findRootValueDeserializer(valueType), parameterIndex);
            } else {
                return ctxt.reportBadDefinition(beanDescRef.getType(), String.format(
                    "Unsupported type for any-setter: %s -- only support `Map`s, `JsonNode` and `ObjectNode` ",
                    ClassUtil.getTypeDescription(paramType)));
            }
        } else {
            return ctxt.reportBadDefinition(beanDescRef.getType(), String.format(
                    "Unrecognized mutator type for any-setter: %s",
                    ClassUtil.nameOf(mutator.getClass())));
        }

        // NOTE: code from now on is for `Map` valued Any properties (JsonNode/ObjectNode
        // already returned; unsupported types threw Exception), if we have Field/Ctor-Parameter
        // any-setter -- or, basically Any supported type (if Method)

        // First: see if there are explicitly specified
        // and then possible direct deserializer override on accessor
        KeyDeserializer keyDeser = findKeyDeserializerFromAnnotation(ctxt, mutator);
        if (keyDeser == null) {
            keyDeser = (KeyDeserializer) keyType.getValueHandler();
        }
        if (keyDeser == null) {
            keyDeser = ctxt.findKeyDeserializer(keyType, prop);
        } else {
            if (keyDeser instanceof ContextualKeyDeserializer) {
                keyDeser = ((ContextualKeyDeserializer) keyDeser)
                        .createContextual(ctxt, prop);
            }
        }
        ValueDeserializer<Object> deser = findContentDeserializerFromAnnotation(ctxt, mutator);
        if (deser == null) {
            deser = (ValueDeserializer<Object>) valueType.getValueHandler();
        }
        if (deser != null) {
            // As per [databind#462] need to ensure we contextualize deserializer before passing it on
            deser = (ValueDeserializer<Object>) ctxt.handlePrimaryContextualization(deser, prop, valueType);
        }
        TypeDeserializer typeDeser = (TypeDeserializer) valueType.getTypeHandler();
        if (isField) {
            return SettableAnyProperty.constructForMapField(ctxt,
                    prop, mutator, valueType, keyDeser, deser, typeDeser);
        }
        if (isParameter) {
            return SettableAnyProperty.constructForMapParameter(ctxt,
                    prop, mutator, valueType, keyDeser, deser, typeDeser, parameterIndex);
        }
        return SettableAnyProperty.constructForMethod(ctxt,
                prop, mutator, valueType, keyDeser, deser, typeDeser);
    }

    /**
     * Method that will construct a regular bean property setter using
     * the given setter method.
     *
     * @return Property constructed, if any; or null to indicate that
     *   there should be no property based on given definitions.
     */
    protected SettableBeanProperty constructSettableProperty(DeserializationContext ctxt,
            BeanDescription.Supplier beanDescRef, BeanPropertyDefinition propDef,
            JavaType propType0)
    {
        // need to ensure method is callable (for non-public)
        AnnotatedMember mutator = propDef.getNonConstructorMutator();
        // 08-Sep-2016, tatu: issues like [databind#1342] suggest something fishy
        //   going on; add sanity checks to try to pin down actual problem...
        //   Possibly passing creator parameter?
        if (mutator == null) {
            ctxt.reportBadPropertyDefinition(beanDescRef, propDef, "No non-constructor mutator available");
        }
        JavaType type = resolveMemberAndTypeAnnotations(ctxt, mutator, propType0);
        // Does the Method specify the deserializer to use? If so, let's use it.
        TypeDeserializer typeDeser = (TypeDeserializer) type.getTypeHandler();
        SettableBeanProperty prop;
        if (isFinalField(mutator)) {
            return null;
        }
        prop = new MethodProperty(propDef, type, typeDeser, beanDescRef.getClassAnnotations(), mutator);
        ValueDeserializer<?> deser = findDeserializerFromAnnotation(ctxt, mutator);
        if (deser == null) {
            deser = (ValueDeserializer<?>) type.getValueHandler();
        }
        if (deser != null) {
            deser = ctxt.handlePrimaryContextualization(deser, prop, type);
            prop = prop.withValueDeserializer(deser);
        }
        // need to retain name of managed forward references:
        AnnotationIntrospector.ReferenceProperty ref = propDef.findReferenceType();
        if (ref != null && ref.isManagedReference()) {
            prop.setManagedReferenceName(ref.getName());
        }
        ObjectIdInfo objectIdInfo = propDef.findObjectIdInfo();
        if (objectIdInfo != null){
            prop.setObjectIdInfo(objectIdInfo);
        }
        return prop;
    }

    private boolean isFinalField(AnnotatedMember am) {
        return am instanceof AnnotatedField
                && Modifier.isFinal(am.getMember().getModifiers());
    }

    /**
     * Method that will construct a regular bean property setter using
     * the given setter method.
     */
    protected SettableBeanProperty constructSetterlessProperty(DeserializationContext ctxt,
            BeanDescription beanDesc, BeanPropertyDefinition propDef)
    {
        final AnnotatedMethod getter = propDef.getGetter();
        JavaType type = resolveMemberAndTypeAnnotations(ctxt, getter, getter.getType());
        TypeDeserializer typeDeser = (TypeDeserializer) type.getTypeHandler();
        SettableBeanProperty prop = new SetterlessProperty(propDef, type, typeDeser,
                beanDesc.getClassAnnotations(), getter);
        ValueDeserializer<?> deser = findDeserializerFromAnnotation(ctxt, getter);
        if (deser == null) {
            deser = (ValueDeserializer<?>) type.getValueHandler();
        }
        if (deser != null) {
            deser = ctxt.handlePrimaryContextualization(deser, prop, type);
            prop = prop.withValueDeserializer(deser);
        }
        return prop;
    }

    /*
    /**********************************************************
    /* Helper methods for Bean deserializer, other
    /**********************************************************
     */

    /**
     * Helper method used to skip processing for types that we know
     * cannot be (i.e. are never consider to be) beans:
     * things like primitives, Arrays, Enums, and proxy types.
     *<p>
     * Note that usually we shouldn't really be getting these sort of
     * types anyway; but better safe than sorry.
     */
    protected boolean isPotentialBeanType(Class<?> type)
    {
        String typeStr = ClassUtil.canBeABeanType(type);
        if (typeStr != null) {
            throw new IllegalArgumentException("Cannot deserialize Class "+type.getName()+" (of type "+typeStr+") as a Bean");
        }
        if (ClassUtil.isProxyType(type)) {
            throw new IllegalArgumentException("Cannot deserialize Proxy class "+type.getName()+" as a Bean");
        }
        // also: can't deserialize some local classes: static are ok; in-method not;
        // other non-static inner classes are ok
        typeStr = ClassUtil.isLocalType(type, true);
        if (typeStr != null) {
            throw new IllegalArgumentException("Cannot deserialize Class "+type.getName()+" (of type "+typeStr+") as a Bean");
        }
        return true;
    }

    /**
     * Helper method that will check whether given raw type is marked as always ignorable
     * (for purpose of ignoring properties with type)
     */
    protected boolean isIgnorableType(DeserializationContext ctxt, BeanPropertyDefinition propDef,
            Class<?> type, Map<Class<?>,Boolean> ignoredTypes)
    {
        Boolean status = ignoredTypes.get(type);
        if (status != null) {
            return status.booleanValue();
        }
        // 22-Oct-2016, tatu: Slight check to skip primitives, String
        if ((type == String.class) || type.isPrimitive()) {
            status = Boolean.FALSE;
        } else {
            // 21-Apr-2016, tatu: For 2.8, can specify config overrides
            final DeserializationConfig config = ctxt.getConfig();
            status = config.getConfigOverride(type).getIsIgnoredType();
            if (status == null) {
                AnnotatedClass classAnnotations = ctxt.introspectClassAnnotations(type);
                status = ctxt.getAnnotationIntrospector().isIgnorableType(config, classAnnotations);
                // We default to 'false', i.e. not ignorable
                if (status == null) {
                    status = Boolean.FALSE;
                }
            }
        }
        ignoredTypes.put(type, status);
        return status.booleanValue();
    }

    // @since 2.8.11
    protected void _validateSubType(DeserializationContext ctxt, JavaType type,
            BeanDescription.Supplier beanDescRef)
    {
        SubTypeValidator.instance().validateSubType(ctxt, type, beanDescRef);
    }
}
