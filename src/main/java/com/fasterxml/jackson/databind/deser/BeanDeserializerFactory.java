package com.fasterxml.jackson.databind.deser;

import java.util.*;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.cfg.DeserializerFactoryConfig;
import com.fasterxml.jackson.databind.cfg.ConfigOverride;
import com.fasterxml.jackson.databind.deser.impl.*;
import com.fasterxml.jackson.databind.deser.std.ThrowableDeserializer;
import com.fasterxml.jackson.databind.introspect.*;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.util.ClassUtil;
import com.fasterxml.jackson.databind.util.SimpleBeanPropertyDefinition;

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

    private final static Class<?>[] NO_VIEWS = new Class<?>[0];

    /**
     * Set of well-known "nasty classes", deserialization of which is considered dangerous
     * and should (and is) prevented by default.
     *
     * @since 2.8.9
     */
    protected final static Set<String> DEFAULT_NO_DESER_CLASS_NAMES;
    static {
        Set<String> s = new HashSet<>();
        // Courtesy of [https://github.com/kantega/notsoserial]:
        // (and wrt [databind#1599]
        s.add("org.apache.commons.collections.functors.InvokerTransformer");
        s.add("org.apache.commons.collections.functors.InstantiateTransformer");
        s.add("org.apache.commons.collections4.functors.InvokerTransformer");
        s.add("org.apache.commons.collections4.functors.InstantiateTransformer");
        s.add("org.codehaus.groovy.runtime.ConvertedClosure");
        s.add("org.codehaus.groovy.runtime.MethodClosure");
        s.add("org.springframework.beans.factory.ObjectFactory");
        s.add("com.sun.org.apache.xalan.internal.xsltc.trax.TemplatesImpl");
        s.add("org.apache.xalan.xsltc.trax.TemplatesImpl");
        DEFAULT_NO_DESER_CLASS_NAMES = Collections.unmodifiableSet(s);
    }

    /**
     * Set of class names of types that are never to be deserialized.
     *
     * @since 2.8.9
     */
    protected Set<String> _cfgIllegalClassNames = DEFAULT_NO_DESER_CLASS_NAMES;

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
        if (getClass() != BeanDeserializerFactory.class) {
            throw new IllegalStateException("Subtype of BeanDeserializerFactory ("+getClass().getName()
                    +") has not properly overridden method 'withAdditionalDeserializers': can not instantiate subtype with "
                    +"additional deserializer definitions");
        }
        return new BeanDeserializerFactory(config);
    }
    
    /*
    /**********************************************************
    /* DeserializerFactory API implementation
    /**********************************************************
     */

    /**
     * Method that {@link DeserializerCache}s call to create a new
     * deserializer for types other than Collections, Maps, arrays and
     * enums.
     */
    @Override
    public JsonDeserializer<Object> createBeanDeserializer(DeserializationContext ctxt,
            JavaType type, BeanDescription beanDesc)
        throws JsonMappingException
    {
        final DeserializationConfig config = ctxt.getConfig();
        // We may also have custom overrides:
        JsonDeserializer<Object> custom = _findCustomBeanDeserializer(type, config, beanDesc);
        if (custom != null) {
            return custom;
        }
        /* One more thing to check: do we have an exception type
         * (Throwable or its sub-classes)? If so, need slightly
         * different handling.
         */
        if (type.isThrowable()) {
            return buildThrowableDeserializer(ctxt, type, beanDesc);
        }
        /* Or, for abstract types, may have alternate means for resolution
         * (defaulting, materialization)
         */
        // 29-Nov-2015, tatu: Also, filter out calls to primitive types, they are
        //    not something we could materialize anything for
        if (type.isAbstract() && !type.isPrimitive() && !type.isEnumType()) {
            // Let's make it possible to materialize abstract types.
            JavaType concreteType = materializeAbstractType(ctxt, type, beanDesc);
            if (concreteType != null) {
                /* important: introspect actual implementation (abstract class or
                 * interface doesn't have constructors, for one)
                 */
                beanDesc = config.introspect(concreteType);
                return buildBeanDeserializer(ctxt, concreteType, beanDesc);
            }
        }
        // Otherwise, may want to check handlers for standard types, from superclass:
        @SuppressWarnings("unchecked")
        JsonDeserializer<Object> deser = (JsonDeserializer<Object>) findStdDeserializer(ctxt, type, beanDesc);
        if (deser != null) {
            return deser;
        }

        // Otherwise: could the class be a Bean class? If not, bail out
        if (!isPotentialBeanType(type.getRawClass())) {
            return null;
        }
        // For checks like [databind#1599]
        checkIllegalTypes(ctxt, type, beanDesc);
        // Use generic bean introspection to build deserializer
        return buildBeanDeserializer(ctxt, type, beanDesc);
    }

    @Override
    public JsonDeserializer<Object> createBuilderBasedDeserializer(
    		DeserializationContext ctxt, JavaType valueType, BeanDescription beanDesc,
    		Class<?> builderClass)
        throws JsonMappingException
    {
        // First: need a BeanDescription for builder class
        JavaType builderType = ctxt.constructType(builderClass);
        BeanDescription builderDesc = ctxt.getConfig().introspectForBuilder(builderType);
        return buildBuilderBasedDeserializer(ctxt, valueType, builderDesc);
    }
    
    /**
     * Method called by {@link BeanDeserializerFactory} to see if there might be a standard
     * deserializer registered for given type.
     */
    protected JsonDeserializer<?> findStdDeserializer(DeserializationContext ctxt,
            JavaType type, BeanDescription beanDesc)
        throws JsonMappingException
    {
        // note: we do NOT check for custom deserializers here, caller has already
        // done that
        JsonDeserializer<?> deser = findDefaultDeserializer(ctxt, type, beanDesc);
        // Also: better ensure these are post-processable?
        if (deser != null) {
            if (_factoryConfig.hasDeserializerModifiers()) {
                for (BeanDeserializerModifier mod : _factoryConfig.deserializerModifiers()) {
                    deser = mod.modifyDeserializer(ctxt.getConfig(), beanDesc, deser);
                }
            }
        }
        return deser;
    }
    
    protected JavaType materializeAbstractType(DeserializationContext ctxt,
            JavaType type, BeanDescription beanDesc)
        throws JsonMappingException
    {
        // May have multiple resolvers, call in precedence order until one returns non-null
        for (AbstractTypeResolver r : _factoryConfig.abstractTypeResolvers()) {
            JavaType concrete = r.resolveAbstractType(ctxt.getConfig(), beanDesc);
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
    public JsonDeserializer<Object> buildBeanDeserializer(DeserializationContext ctxt,
            JavaType type, BeanDescription beanDesc)
        throws JsonMappingException
    {
        // First: check what creators we can use, if any
        ValueInstantiator valueInstantiator;
        /* 04-Jun-2015, tatu: To work around [databind#636], need to catch the
         *    issue, defer; this seems like a reasonable good place for now.
         *   Note, however, that for non-Bean types (Collections, Maps) this
         *   probably won't work and needs to be added elsewhere.
         */
        try {
            valueInstantiator = findValueInstantiator(ctxt, beanDesc);
        } catch (NoClassDefFoundError error) {
            return new ErrorThrowingDeserializer(error);
        }
        BeanDeserializerBuilder builder = constructBeanDeserializerBuilder(ctxt, beanDesc);
        builder.setValueInstantiator(valueInstantiator);
         // And then setters for deserializing from JSON Object
        addBeanProps(ctxt, beanDesc, builder);
        addObjectIdReader(ctxt, beanDesc, builder);

        // managed/back reference fields/setters need special handling... first part
        addReferenceProperties(ctxt, beanDesc, builder);
        addInjectables(ctxt, beanDesc, builder);
        
        final DeserializationConfig config = ctxt.getConfig();
        if (_factoryConfig.hasDeserializerModifiers()) {
            for (BeanDeserializerModifier mod : _factoryConfig.deserializerModifiers()) {
                builder = mod.updateBuilder(config, beanDesc, builder);
            }
        }
        JsonDeserializer<?> deserializer;

        /* 19-Mar-2012, tatu: This check used to be done earlier; but we have to defer
         *   it a bit to collect information on ObjectIdReader, for example.
         */
        if (type.isAbstract() && !valueInstantiator.canInstantiate()) {
            deserializer = builder.buildAbstract();
        } else {
            deserializer = builder.build();
        }

        // [JACKSON-440]: may have modifier(s) that wants to modify or replace serializer we just built:
        if (_factoryConfig.hasDeserializerModifiers()) {
            for (BeanDeserializerModifier mod : _factoryConfig.deserializerModifiers()) {
                deserializer = mod.modifyDeserializer(config, beanDesc, deserializer);
            }
        }
        return (JsonDeserializer<Object>) deserializer;
    }
    
    /**
     * Method for constructing a bean deserializer that uses specified
     * intermediate Builder for binding data, and construction of the
     * value instance.
     * Note that implementation is mostly copied from the regular
     * BeanDeserializer build method.
     */
    @SuppressWarnings("unchecked")
    protected JsonDeserializer<Object> buildBuilderBasedDeserializer(
    		DeserializationContext ctxt, JavaType valueType, BeanDescription builderDesc)
        throws JsonMappingException
    {
    	// Creators, anyone? (to create builder itself)
        ValueInstantiator valueInstantiator = findValueInstantiator(ctxt, builderDesc);
        final DeserializationConfig config = ctxt.getConfig();
        BeanDeserializerBuilder builder = constructBeanDeserializerBuilder(ctxt, builderDesc);
        builder.setValueInstantiator(valueInstantiator);
         // And then "with methods" for deserializing from JSON Object
        addBeanProps(ctxt, builderDesc, builder);
        addObjectIdReader(ctxt, builderDesc, builder);
        
        // managed/back reference fields/setters need special handling... first part
        addReferenceProperties(ctxt, builderDesc, builder);
        addInjectables(ctxt, builderDesc, builder);

        JsonPOJOBuilder.Value builderConfig = builderDesc.findPOJOBuilderConfig();
        final String buildMethodName = (builderConfig == null) ?
                "build" : builderConfig.buildMethodName;
        
        // and lastly, find build method to use:
        AnnotatedMethod buildMethod = builderDesc.findMethod(buildMethodName, null);
        if (buildMethod != null) { // note: can't yet throw error; may be given build method
            if (config.canOverrideAccessModifiers()) {
            	ClassUtil.checkAndFixAccess(buildMethod.getMember(), config.isEnabled(MapperFeature.OVERRIDE_PUBLIC_ACCESS_MODIFIERS));
            }
        }
        builder.setPOJOBuilder(buildMethod, builderConfig);
        // this may give us more information...
        if (_factoryConfig.hasDeserializerModifiers()) {
            for (BeanDeserializerModifier mod : _factoryConfig.deserializerModifiers()) {
                builder = mod.updateBuilder(config, builderDesc, builder);
            }
        }
        JsonDeserializer<?> deserializer = builder.buildBuilderBased(
        		valueType, buildMethodName);

        // [JACKSON-440]: may have modifier(s) that wants to modify or replace serializer we just built:
        if (_factoryConfig.hasDeserializerModifiers()) {
            for (BeanDeserializerModifier mod : _factoryConfig.deserializerModifiers()) {
                deserializer = mod.modifyDeserializer(config, builderDesc, deserializer);
            }
        }
        return (JsonDeserializer<Object>) deserializer;
    }
    
    protected void addObjectIdReader(DeserializationContext ctxt,
            BeanDescription beanDesc, BeanDeserializerBuilder builder)
        throws JsonMappingException
    {
        ObjectIdInfo objectIdInfo = beanDesc.getObjectIdInfo();
        if (objectIdInfo == null) {
            return;
        }
        Class<?> implClass = objectIdInfo.getGeneratorType();
        JavaType idType;
        SettableBeanProperty idProp;
        ObjectIdGenerator<?> gen;

        ObjectIdResolver resolver = ctxt.objectIdResolverInstance(beanDesc.getClassInfo(), objectIdInfo);

        // Just one special case: Property-based generator is trickier
        if (implClass == ObjectIdGenerators.PropertyGenerator.class) { // most special one, needs extra work
            PropertyName propName = objectIdInfo.getPropertyName();
            idProp = builder.findProperty(propName);
            if (idProp == null) {
                throw new IllegalArgumentException("Invalid Object Id definition for "
                        +beanDesc.getBeanClass().getName()+": can not find property with name '"+propName+"'");
            }
            idType = idProp.getType();
            gen = new PropertyBasedObjectIdGenerator(objectIdInfo.getScope());
        } else {
            JavaType type = ctxt.constructType(implClass);
            idType = ctxt.getTypeFactory().findTypeParameters(type, ObjectIdGenerator.class)[0];
            idProp = null;
            gen = ctxt.objectIdGeneratorInstance(beanDesc.getClassInfo(), objectIdInfo);
        }
        // also: unlike with value deserializers, let's just resolve one we need here
        JsonDeserializer<?> deser = ctxt.findRootValueDeserializer(idType);
        builder.setObjectIdReader(ObjectIdReader.construct(idType,
                objectIdInfo.getPropertyName(), gen, deser, idProp, resolver));
    }
    
    @SuppressWarnings("unchecked")
    public JsonDeserializer<Object> buildThrowableDeserializer(DeserializationContext ctxt,
            JavaType type, BeanDescription beanDesc)
        throws JsonMappingException
    {
        final DeserializationConfig config = ctxt.getConfig();
        // first: construct like a regular bean deserializer...
        BeanDeserializerBuilder builder = constructBeanDeserializerBuilder(ctxt, beanDesc);
        builder.setValueInstantiator(findValueInstantiator(ctxt, beanDesc));

        addBeanProps(ctxt, beanDesc, builder);
        // (and assume there won't be any back references)

        // But then let's decorate things a bit
        /* To resolve [JACKSON-95], need to add "initCause" as setter
         * for exceptions (sub-classes of Throwable).
         */
        AnnotatedMethod am = beanDesc.findMethod("initCause", INIT_CAUSE_PARAMS);
        if (am != null) { // should never be null
            SimpleBeanPropertyDefinition propDef = SimpleBeanPropertyDefinition.construct(ctxt.getConfig(), am,
                    new PropertyName("cause"));
            SettableBeanProperty prop = constructSettableProperty(ctxt, beanDesc, propDef,
                    am.getParameterType(0));
            if (prop != null) {
                /* 21-Aug-2011, tatus: We may actually have found 'cause' property
                 *   to set... but let's replace it just in case,
                 *   otherwise can end up with odd errors.
                 */
                builder.addOrReplaceProperty(prop, true);
            }
        }

        // And also need to ignore "localizedMessage"
        builder.addIgnorable("localizedMessage");
        // Java 7 also added "getSuppressed", skip if we have such data:
        builder.addIgnorable("suppressed");
        /* As well as "message": it will be passed via constructor,
         * as there's no 'setMessage()' method
        */
        builder.addIgnorable("message");

        // update builder now that all information is in?
        if (_factoryConfig.hasDeserializerModifiers()) {
            for (BeanDeserializerModifier mod : _factoryConfig.deserializerModifiers()) {
                builder = mod.updateBuilder(config, beanDesc, builder);
            }
        }
        JsonDeserializer<?> deserializer = builder.build();
        
        /* At this point it ought to be a BeanDeserializer; if not, must assume
         * it's some other thing that can handle deserialization ok...
         */
        if (deserializer instanceof BeanDeserializer) {
            deserializer = new ThrowableDeserializer((BeanDeserializer) deserializer);
        }

        // may have modifier(s) that wants to modify or replace serializer we just built:
        if (_factoryConfig.hasDeserializerModifiers()) {
            for (BeanDeserializerModifier mod : _factoryConfig.deserializerModifiers()) {
                deserializer = mod.modifyDeserializer(config, beanDesc, deserializer);
            }
        }
        return (JsonDeserializer<Object>) deserializer;
    }

    /*
    /**********************************************************
    /* Helper methods for Bean deserializer construction,
    /* overridable by sub-classes
    /**********************************************************
     */

    /**
     * Overridable method that constructs a {@link BeanDeserializerBuilder}
     * which is used to accumulate information needed to create deserializer
     * instance.
     */
    protected BeanDeserializerBuilder constructBeanDeserializerBuilder(DeserializationContext ctxt,
            BeanDescription beanDesc) {
        return new BeanDeserializerBuilder(beanDesc, ctxt.getConfig());
    }
    
    /**
     * Method called to figure out settable properties for the
     * bean deserializer to use.
     *<p>
     * Note: designed to be overridable, and effort is made to keep interface
     * similar between versions.
     */
    protected void addBeanProps(DeserializationContext ctxt,
            BeanDescription beanDesc, BeanDeserializerBuilder builder)
        throws JsonMappingException
    {
        final boolean isConcrete = !beanDesc.getType().isAbstract();
        final SettableBeanProperty[] creatorProps = isConcrete
                ? builder.getValueInstantiator().getFromObjectArguments(ctxt.getConfig())
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
            ignored = ignorals.getIgnored();
            for (String propName : ignored) {
                builder.addIgnorable(propName);
            }
        } else {
            ignored = Collections.emptySet();
        }

        // Also, do we have a fallback "any" setter?
        AnnotatedMethod anySetterMethod = beanDesc.findAnySetter();
        AnnotatedMember anySetterField = null;
        if (anySetterMethod != null) {
            builder.setAnySetter(constructAnySetter(ctxt, beanDesc, anySetterMethod));
        }
        else {
        	anySetterField = beanDesc.findAnySetterField();
        	if(anySetterField != null) {
        		builder.setAnySetter(constructAnySetter(ctxt, beanDesc, anySetterField));
        	}
        }
        // NOTE: we do NOT add @JsonIgnore'd properties into blocked ones if there's any-setter
        // Implicit ones via @JsonIgnore and equivalent?
        if (anySetterMethod == null && anySetterField == null) {
            Collection<String> ignored2 = beanDesc.getIgnoredPropertyNames();
            if (ignored2 != null) {
                for (String propName : ignored2) {
                    // allow ignoral of similarly named JSON property, but do not force;
                    // latter means NOT adding this to 'ignored':
                    builder.addIgnorable(propName);
                }
            }
        }
        final boolean useGettersAsSetters = ctxt.isEnabled(MapperFeature.USE_GETTERS_AS_SETTERS)
                && ctxt.isEnabled(MapperFeature.AUTO_DETECT_GETTERS);

        // Ok: let's then filter out property definitions
        List<BeanPropertyDefinition> propDefs = filterBeanProps(ctxt,
                beanDesc, builder, beanDesc.findProperties(), ignored);

        // After which we can let custom code change the set
        if (_factoryConfig.hasDeserializerModifiers()) {
            for (BeanDeserializerModifier mod : _factoryConfig.deserializerModifiers()) {
                propDefs = mod.updateProperties(ctxt.getConfig(), beanDesc, propDefs);
            }
        }
        
        // At which point we still have all kinds of properties; not all with mutators:
        for (BeanPropertyDefinition propDef : propDefs) {
            SettableBeanProperty prop = null;
            /* 18-Oct-2013, tatu: Although constructor parameters have highest precedence,
             *   we need to do linkage (as per [databind#318]), and so need to start with
             *   other types, and only then create constructor parameter, if any.
             */
            if (propDef.hasSetter()) {
                JavaType propertyType = propDef.getSetter().getParameterType(0);
                prop = constructSettableProperty(ctxt, beanDesc, propDef, propertyType);
            } else if (propDef.hasField()) {
                JavaType propertyType = propDef.getField().getType();
                prop = constructSettableProperty(ctxt, beanDesc, propDef, propertyType);
            } else if (useGettersAsSetters && propDef.hasGetter()) {
                /* May also need to consider getters
                 * for Map/Collection properties; but with lowest precedence
                 */
                AnnotatedMethod getter = propDef.getGetter();
                // should only consider Collections and Maps, for now?
                Class<?> rawPropertyType = getter.getRawType();
                if (Collection.class.isAssignableFrom(rawPropertyType)
                        || Map.class.isAssignableFrom(rawPropertyType)) {
                    prop = constructSetterlessProperty(ctxt, beanDesc, propDef);
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
                if (creatorProps != null) {
                    for (SettableBeanProperty cp : creatorProps) {
                        if (name.equals(cp.getName()) && (cp instanceof CreatorProperty)) {
                            cprop = (CreatorProperty) cp;
                            break;
                        }
                    }
                }
                if (cprop == null) {
                    List<String> n = new ArrayList<>();
                    for (SettableBeanProperty cp : creatorProps) {
                        n.add(cp.getName());
                    }
                    ctxt.reportBadPropertyDefinition(beanDesc, propDef,
                            "Could not find creator property with name '%s' (known Creator properties: %s)",
                            name, n);
                    continue;
                }
                if (prop != null) {
                    cprop.setFallbackSetter(prop);
                }
                prop = cprop;
                builder.addCreatorProperty(cprop);
                continue;
            }

            if (prop != null) {
                Class<?>[] views = propDef.findViews();
                if (views == null) {
                    // one more twist: if default inclusion disabled, need to force empty set of views
                    if (!ctxt.isEnabled(MapperFeature.DEFAULT_VIEW_INCLUSION)) {
                        views = NO_VIEWS;
                    }
                }
                // one more thing before adding to builder: copy any metadata
                prop.setViews(views);
                builder.addProperty(prop);
            }
        }
    }
    
    /**
     * Helper method called to filter out explicit ignored properties,
     * as well as properties that have "ignorable types".
     * Note that this will not remove properties that have no
     * setters.
     */
    protected List<BeanPropertyDefinition> filterBeanProps(DeserializationContext ctxt,
            BeanDescription beanDesc, BeanDeserializerBuilder builder,
            List<BeanPropertyDefinition> propDefsIn,
            Set<String> ignored)
        throws JsonMappingException
    {
        ArrayList<BeanPropertyDefinition> result = new ArrayList<BeanPropertyDefinition>(
                Math.max(4, propDefsIn.size()));
        HashMap<Class<?>,Boolean> ignoredTypes = new HashMap<Class<?>,Boolean>();
        // These are all valid setters, but we do need to introspect bit more
        for (BeanPropertyDefinition property : propDefsIn) {
            String name = property.getName();
            if (ignored.contains(name)) { // explicit ignoral using @JsonIgnoreProperties needs to block entries
                continue;
            }
            if (!property.hasConstructorParameter()) { // never skip constructor params
                Class<?> rawPropertyType = null;
                if (property.hasSetter()) {
                    rawPropertyType = property.getSetter().getRawParameterType(0);
                } else if (property.hasField()) {
                    rawPropertyType = property.getField().getRawType();
                }

                // Some types are declared as ignorable as well
                if ((rawPropertyType != null)
                        && isIgnorableType(ctxt.getConfig(), beanDesc, rawPropertyType, ignoredTypes)) {
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
    protected void addReferenceProperties(DeserializationContext ctxt,
            BeanDescription beanDesc, BeanDeserializerBuilder builder)
        throws JsonMappingException
    {
        // and then back references, not necessarily found as regular properties
        Map<String,AnnotatedMember> refs = beanDesc.findBackReferenceProperties();
        if (refs != null) {
            for (Map.Entry<String, AnnotatedMember> en : refs.entrySet()) {
                String name = en.getKey();
                AnnotatedMember m = en.getValue();
                JavaType type;
                if (m instanceof AnnotatedMethod) {
                    type = ((AnnotatedMethod) m).getParameterType(0);
                } else {
                    type = m.getType();
                    // 30-Mar-2017, tatu: Unfortunately it is not yet possible to make back-refs
                    //    work through constructors; but let's at least indicate the issue for now
                    if (m instanceof AnnotatedParameter) {
                        ctxt.reportBadTypeDefinition(beanDesc,
"Can not bind back references as Creator parameters: type %s (reference '%s', parameter index #%d)",
beanDesc.getBeanClass().getName(), name, ((AnnotatedParameter) m).getIndex());
                    }
                }
                SimpleBeanPropertyDefinition propDef = SimpleBeanPropertyDefinition.construct(
                        ctxt.getConfig(), m, PropertyName.construct(name));
                builder.addBackReferenceProperty(name, constructSettableProperty(ctxt,
                        beanDesc, propDef, type));
            }
        }
    }

    /**
     * Method called locate all members used for value injection (if any),
     * constructor {@link com.fasterxml.jackson.databind.deser.impl.ValueInjector} instances, and add them to builder.
     */
    protected void addInjectables(DeserializationContext ctxt,
            BeanDescription beanDesc, BeanDeserializerBuilder builder)
        throws JsonMappingException
    {
        Map<Object, AnnotatedMember> raw = beanDesc.findInjectables();
        if (raw != null) {
            for (Map.Entry<Object, AnnotatedMember> entry : raw.entrySet()) {
                AnnotatedMember m = entry.getValue();
                builder.addInjectable(PropertyName.construct(m.getName()),
                        m.getType(),
                        beanDesc.getClassAnnotations(), m, entry.getKey());
            }
        }
    }

    /**
     * Method called to construct fallback {@link SettableAnyProperty}
     * for handling unknown bean properties, given a method that
     * has been designated as such setter.
     * 
     * @param mutator Either 2-argument method (setter, with key and value), or Field
     *     that contains Map; either way accessor used for passing "any values"
     */
    @SuppressWarnings("unchecked")
    protected SettableAnyProperty constructAnySetter(DeserializationContext ctxt,
            BeanDescription beanDesc, AnnotatedMember mutator)
        throws JsonMappingException
    {
        //find the java type based on the annotated setter method or setter field 
        JavaType type = null;
        if (mutator instanceof AnnotatedMethod) {
            // we know it's a 2-arg method, second arg is the value
            type = ((AnnotatedMethod) mutator).getParameterType(1);
        } else if (mutator instanceof AnnotatedField) {
            // get the type from the content type of the map object
            type = ((AnnotatedField) mutator).getType().getContentType();
        }
        // First: various annotations on type itself, as well as type-overrides
        // on accessor need to be resolved
        type = resolveMemberAndTypeAnnotations(ctxt, mutator, type);
        BeanProperty.Std prop = new BeanProperty.Std(PropertyName.construct(mutator.getName()),
                type, null, beanDesc.getClassAnnotations(), mutator,
                PropertyMetadata.STD_OPTIONAL);
        // and then possible direct deserializer override on accessor
        JsonDeserializer<Object> deser = findDeserializerFromAnnotation(ctxt, mutator);
        if (deser == null) {
            deser = type.getValueHandler();
        }
        if (deser != null) {
            // As per [databind#462] need to ensure we contextualize deserializer before passing it on
            deser = (JsonDeserializer<Object>) ctxt.handlePrimaryContextualization(deser, prop, type);
        }
        TypeDeserializer typeDeser = type.getTypeHandler();
        return new SettableAnyProperty(prop, mutator, type, deser, typeDeser);
    }

    /**
     * Method that will construct a regular bean property setter using
     * the given setter method.
     *
     * @return Property constructed, if any; or null to indicate that
     *   there should be no property based on given definitions.
     */
    protected SettableBeanProperty constructSettableProperty(DeserializationContext ctxt,
            BeanDescription beanDesc, BeanPropertyDefinition propDef,
            JavaType propType0)
        throws JsonMappingException
    {
        // need to ensure method is callable (for non-public)
        AnnotatedMember mutator = propDef.getNonConstructorMutator();
        // 08-Sep-2016, tatu: issues like [databind#1342] suggest something fishy
        //   going on; add sanity checks to try to pin down actual problem...
        //   Possibly passing creator parameter?
        if (mutator == null) {
            ctxt.reportBadPropertyDefinition(beanDesc, propDef, "No non-constructor mutator available");
        }
        JavaType type = resolveMemberAndTypeAnnotations(ctxt, mutator, propType0);
        // Does the Method specify the deserializer to use? If so, let's use it.
        TypeDeserializer typeDeser = type.getTypeHandler();
        SettableBeanProperty prop;
        if (mutator instanceof AnnotatedMethod) {
            prop = new MethodProperty(propDef, type, typeDeser,
                    beanDesc.getClassAnnotations(), (AnnotatedMethod) mutator);
        } else {
            // 08-Sep-2016, tatu: wonder if we should verify it is `AnnotatedField` to be safe?
            prop = new FieldProperty(propDef, type, typeDeser,
                    beanDesc.getClassAnnotations(), (AnnotatedField) mutator);
        }
        JsonDeserializer<?> deser = findDeserializerFromAnnotation(ctxt, mutator);
        if (deser == null) {
            deser = type.getValueHandler();
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

    /**
     * Method that will construct a regular bean property setter using
     * the given setter method.
     */
    protected SettableBeanProperty constructSetterlessProperty(DeserializationContext ctxt,
            BeanDescription beanDesc, BeanPropertyDefinition propDef)
        throws JsonMappingException
    {
        final AnnotatedMethod getter = propDef.getGetter();
        JavaType type = resolveMemberAndTypeAnnotations(ctxt, getter, getter.getType());
        TypeDeserializer typeDeser = type.getTypeHandler();
        SettableBeanProperty prop = new SetterlessProperty(propDef, type, typeDeser,
                beanDesc.getClassAnnotations(), getter);
        JsonDeserializer<?> deser = findDeserializerFromAnnotation(ctxt, getter);
        if (deser == null) {
            deser = type.getValueHandler();
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
     * can not be (i.e. are never consider to be) beans: 
     * things like primitives, Arrays, Enums, and proxy types.
     *<p>
     * Note that usually we shouldn't really be getting these sort of
     * types anyway; but better safe than sorry.
     */
    protected boolean isPotentialBeanType(Class<?> type)
    {
        String typeStr = ClassUtil.canBeABeanType(type);
        if (typeStr != null) {
            throw new IllegalArgumentException("Can not deserialize Class "+type.getName()+" (of type "+typeStr+") as a Bean");
        }
        if (ClassUtil.isProxyType(type)) {
            throw new IllegalArgumentException("Can not deserialize Proxy class "+type.getName()+" as a Bean");
        }
        /* also: can't deserialize some local classes: static are ok; in-method not;
         * other non-static inner classes are ok
         */
        typeStr = ClassUtil.isLocalType(type, true);
        if (typeStr != null) {
            throw new IllegalArgumentException("Can not deserialize Class "+type.getName()+" (of type "+typeStr+") as a Bean");
        }
        return true;
    }

    /**
     * Helper method that will check whether given raw type is marked as always ignorable
     * (for purpose of ignoring properties with type)
     */
    protected boolean isIgnorableType(DeserializationConfig config, BeanDescription beanDesc,
            Class<?> type, Map<Class<?>,Boolean> ignoredTypes)
    {
        Boolean status = ignoredTypes.get(type);
        if (status != null) {
            return status.booleanValue();
        }
        // 21-Apr-2016, tatu: For 2.8, can specify config overrides
        ConfigOverride override = config.findConfigOverride(type);
        if (override != null) {
            status = override.getIsIgnoredType();
        }
        if (status == null) {
            BeanDescription desc = config.introspectClassAnnotations(type);
            status = config.getAnnotationIntrospector().isIgnorableType(desc.getClassInfo());
            // We default to 'false', i.e. not ignorable
            if (status == null) {
                status = Boolean.FALSE;
            }
        }
        ignoredTypes.put(type, status);
        return status.booleanValue();
    }

    /**
     * @since 2.8.9
     */
    protected void checkIllegalTypes(DeserializationContext ctxt, JavaType type,
            BeanDescription beanDesc)
        throws JsonMappingException
    {
        // There are certain nasty classes that could cause problems, mostly
        // via default typing -- catch them here.
        String full = type.getRawClass().getName();

        if (_cfgIllegalClassNames.contains(full)) {
            ctxt.reportBadTypeDefinition(beanDesc,
                    "Illegal type (%s) to deserialize: prevented for security reasons", full);
        }
    }
}
