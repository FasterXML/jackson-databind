package tools.jackson.databind.deser;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.*;
import tools.jackson.databind.cfg.*;
import tools.jackson.databind.deser.bean.CreatorCandidate;
import tools.jackson.databind.deser.bean.CreatorCollector;
import tools.jackson.databind.deser.impl.UnwrappedPropertyHandler;
import tools.jackson.databind.deser.jackson.JsonNodeDeserializer;
import tools.jackson.databind.deser.jackson.TokenBufferDeserializer;
import tools.jackson.databind.deser.jdk.*;
import tools.jackson.databind.ext.OptionalHandlerFactory;
import tools.jackson.databind.ext.jdk8.Jdk8OptionalDeserializer;
import tools.jackson.databind.ext.jdk8.OptionalDoubleDeserializer;
import tools.jackson.databind.ext.jdk8.OptionalIntDeserializer;
import tools.jackson.databind.ext.jdk8.OptionalLongDeserializer;
import tools.jackson.databind.introspect.*;
import tools.jackson.databind.jsontype.TypeDeserializer;
import tools.jackson.databind.jsontype.impl.NoOpTypeDeserializer;
import tools.jackson.databind.type.*;
import tools.jackson.databind.util.*;

/**
 * Abstract factory base class that can provide deserializers for standard
 * JDK classes, including collection classes and simple heuristics for
 * "up-casting" common collection interface types
 * (such as {@link java.util.Collection}).
 *<p>
 * Since all simple deserializers are eagerly instantiated, and there is
 * no additional introspection or customizability of these types,
 * this factory is stateless.
 */
@SuppressWarnings("serial")
public abstract class BasicDeserializerFactory
    extends DeserializerFactory
    implements java.io.Serializable
{
    private final static Class<?> CLASS_OBJECT = Object.class;
    private final static Class<?> CLASS_STRING = String.class;
    private final static Class<?> CLASS_CHAR_SEQUENCE = CharSequence.class;
    private final static Class<?> CLASS_ITERABLE = Iterable.class;
    private final static Class<?> CLASS_MAP_ENTRY = Map.Entry.class;
    private final static Class<?> CLASS_SERIALIZABLE = Serializable.class;

    /*
    /**********************************************************************
    /* Config
    /**********************************************************************
     */

    /**
     * Configuration settings for this factory; immutable instance (just like this
     * factory), new version created via copy-constructor (fluent-style)
     */
    protected final DeserializerFactoryConfig _factoryConfig;

    /*
    /**********************************************************************
    /* Life cycle
    /**********************************************************************
     */

    protected BasicDeserializerFactory(DeserializerFactoryConfig config) {
        _factoryConfig = config;
    }

    /**
     * Method for getting current {@link DeserializerFactoryConfig}.
      *<p>
     * Note that since instances are immutable, you CANNOT change settings
     * by accessing an instance and calling methods: this will simply create
     * new instance of config object.
     */
    public DeserializerFactoryConfig getFactoryConfig() {
        return _factoryConfig;
    }

    protected abstract DeserializerFactory withConfig(DeserializerFactoryConfig config);

    /*
    /**********************************************************************
    /* Configuration handling: fluent factories
    /**********************************************************************
     */

    /**
     * Convenience method for creating a new factory instance with additional deserializer
     * provider.
     */
    @Override
    public final DeserializerFactory withAdditionalDeserializers(Deserializers additional) {
        return withConfig(_factoryConfig.withAdditionalDeserializers(additional));
    }

    /**
     * Convenience method for creating a new factory instance with additional
     * {@link KeyDeserializers}.
     */
    @Override
    public final DeserializerFactory withAdditionalKeyDeserializers(KeyDeserializers additional) {
        return withConfig(_factoryConfig.withAdditionalKeyDeserializers(additional));
    }

    /**
     * Convenience method for creating a new factory instance with additional
     * {@link ValueDeserializerModifier}.
     */
    @Override
    public final DeserializerFactory withDeserializerModifier(ValueDeserializerModifier modifier) {
        return withConfig(_factoryConfig.withDeserializerModifier(modifier));
    }

    /**
     * Convenience method for creating a new factory instance with additional
     * {@link ValueInstantiators}.
     */
    @Override
    public final DeserializerFactory withValueInstantiators(ValueInstantiators instantiators) {
        return withConfig(_factoryConfig.withValueInstantiators(instantiators));
    }

    /*
    /**********************************************************************
    /* DeserializerFactory impl (partial): ValueInstantiators
    /**********************************************************************
     */

    /**
     * Value instantiator is created both based on creator annotations,
     * and on optional externally provided instantiators (registered through
     * module interface).
     */
    @Override
    public ValueInstantiator findValueInstantiator(DeserializationContext ctxt,
            BeanDescription.Supplier beanDescRef)
    {
        final DeserializationConfig config = ctxt.getConfig();
        final boolean hasCustom = _factoryConfig.hasValueInstantiators();

        ValueInstantiator instantiator = null;
        // Check @JsonValueInstantiator before anything else
        AnnotatedClass ac = beanDescRef.getClassInfo();
        Object instDef = config.getAnnotationIntrospector().findValueInstantiator(ctxt.getConfig(), ac);
        if (instDef != null) {
            instantiator = _valueInstantiatorInstance(config, ac, instDef);
        }
        if (instantiator == null) {
            // Second: see if some of standard Jackson/JDK types might provide value
            // instantiators.
            instantiator = JDKValueInstantiators.findStdValueInstantiator(config, beanDescRef.getBeanClass());

            // Third: custom value instantiators via provider?
            if (instantiator == null) {
                if (hasCustom) {
                    for (ValueInstantiators insts : _factoryConfig.valueInstantiators()) {
                        instantiator = insts.findValueInstantiator(config, beanDescRef);
                        if (instantiator != null) {
                            break;
                        }
                    }
                }
                // Fourth: create default one, if no custom
                if (instantiator == null) {
                    instantiator = _constructDefaultValueInstantiator(ctxt, beanDescRef);
                }
            }
        }

        // finally: anyone want to modify ValueInstantiator?
        if (hasCustom) {
            for (ValueInstantiators insts : _factoryConfig.valueInstantiators()) {
                instantiator = insts.modifyValueInstantiator(config, beanDescRef, instantiator);
                // let's do sanity check; easier to spot buggy handlers
                if (instantiator == null) {
                    ctxt.reportBadTypeDefinition(beanDescRef,
						"Broken registered ValueInstantiators (of type %s): returned null ValueInstantiator",
						insts.getClass().getName());
                }
            }
        }
        if (instantiator != null) {
            instantiator = instantiator.createContextual(ctxt, beanDescRef);
        }

        return instantiator;
    }

    /**
     * Method that will construct standard default {@link ValueInstantiator}
     * using annotations (like @JsonCreator) and visibility rules
     */
    protected ValueInstantiator _constructDefaultValueInstantiator(DeserializationContext ctxt,
            BeanDescription.Supplier beanDescRef)
    {
        final MapperConfig<?> config = ctxt.getConfig();
        final PotentialCreators potentialCreators = beanDescRef.get().getPotentialCreators();
        final ConstructorDetector ctorDetector = config.getConstructorDetector();
        // need to construct suitable visibility checker:
        final VisibilityChecker vchecker = config.getDefaultVisibilityChecker(beanDescRef.getBeanClass(),
                beanDescRef.getClassInfo());

        final CreatorCollector creators = new CreatorCollector(config, beanDescRef.getType());

        // 21-May-2024, tatu: [databind#4515] Rewritten to use PotentialCreators
        if (potentialCreators.hasPropertiesBased()) {
            PotentialCreator primaryPropsBased = potentialCreators.propertiesBased;

            // 12-Nov-2024, tatu: [databind#4777] We may have collected a 0-args Factory
            //   method; and if so, may need to "pull it out" as default creator
            if (primaryPropsBased.paramCount() == 0) {
                creators.setDefaultCreator(primaryPropsBased.creator());
            } else {
                // Start by assigning the primary (and only) properties-based creator
                _addSelectedPropertiesBasedCreator(ctxt, beanDescRef, creators,
                        CreatorCandidate.construct(config,
                                primaryPropsBased.creator(), primaryPropsBased.propertyDefs()));
            }
        }

        // Continue with explicitly annotated delegating Creators
        boolean hasExplicitDelegating = _addExplicitDelegatingCreators(ctxt,
                beanDescRef, creators,
                potentialCreators.getExplicitDelegating());

        // constructors only usable on concrete types:
        if (beanDescRef.getType().isConcrete()) {
            // 25-Jan-2017, tatu: As per [databind#1501], [databind#1502], [databind#1503], best
            //     for now to skip attempts at using anything but no-args constructor (see
            //     `InnerClassProperty` construction for that)
            final boolean isNonStaticInnerClass = beanDescRef.get().isNonStaticInnerClass();
            if (isNonStaticInnerClass) {
                // TODO: look for `@JsonCreator` annotated ones, throw explicit exception?
            } else {
                // First things first: the "default constructor" (zero-arg
                // constructor; whether implicit or explicit) is NOT included
                // in list of constructors, so needs to be handled separately.
                // However, we may have added one for 0-args Factory method earlier, so:
                if (!creators.hasDefaultCreator()) {
                    AnnotatedConstructor defaultCtor = beanDescRef.get().findDefaultConstructor();
                    if (defaultCtor != null) {
                        creators.setDefaultCreator(defaultCtor);
                    }
                }

                // 18-Sep-2020, tatu: Although by default implicit introspection is allowed, 2.12
                //   has settings to prevent that either generally, or at least for JDK types
                final boolean findImplicit = ctorDetector.shouldIntrospectImplicitConstructors(beanDescRef.getBeanClass());
                if (findImplicit) {
                    _addImplicitDelegatingConstructors(ctxt, beanDescRef, vchecker,
                            creators,
                            potentialCreators.getImplicitDelegatingConstructors());
                }
            }
        }
        if (!hasExplicitDelegating) {
            _addImplicitDelegatingFactories(ctxt, vchecker,
                    creators,
                    potentialCreators.getImplicitDelegatingFactories());
        }

        return creators.constructValueInstantiator(ctxt);
    }

    public ValueInstantiator _valueInstantiatorInstance(DeserializationConfig config,
            Annotated annotated, Object instDef)
    {
        if (instDef == null) {
            return null;
        }

        ValueInstantiator inst;

        if (instDef instanceof ValueInstantiator valueInstantiator) {
            return valueInstantiator;
        }
        if (!(instDef instanceof Class)) {
            throw new IllegalStateException("AnnotationIntrospector returned key deserializer definition of type "
                    +instDef.getClass().getName()
                    +"; expected type KeyDeserializer or Class<KeyDeserializer> instead");
        }
        Class<?> instClass = (Class<?>)instDef;
        if (ClassUtil.isBogusClass(instClass)) {
            return null;
        }
        if (!ValueInstantiator.class.isAssignableFrom(instClass)) {
            throw new IllegalStateException("AnnotationIntrospector returned Class "+instClass.getName()
                    +"; expected Class<ValueInstantiator>");
        }
        HandlerInstantiator hi = config.getHandlerInstantiator();
        if (hi != null) {
            inst = hi.valueInstantiatorInstance(config, annotated, instClass);
            if (inst != null) {
                return inst;
            }
        }
        return (ValueInstantiator) ClassUtil.createInstance(instClass,
                config.canOverrideAccessModifiers());
    }

    /*
    /**********************************************************************
    /* Creator introspection: new (2.18) helper methods
    /**********************************************************************
     */

    private boolean _addExplicitDelegatingCreators(DeserializationContext ctxt,
            BeanDescription.Supplier beanDescRef,
            CreatorCollector creators,
            List<PotentialCreator> potentials)
    {
        final MapperConfig<?> config = ctxt.getConfig();
        boolean added = false;

        for (PotentialCreator ctor : potentials) {
            added |= _addExplicitDelegatingCreator(ctxt, beanDescRef, creators,
                    CreatorCandidate.construct(config, ctor.creator(), null));
        }
        return added;
    }

    private void _addImplicitDelegatingConstructors(DeserializationContext ctxt,
            BeanDescription.Supplier beanDescRef, VisibilityChecker vchecker,
            CreatorCollector creators,
            List<PotentialCreator> potentials)
    {
        final MapperConfig<?> config = ctxt.getConfig();
        final AnnotationIntrospector intr = config.getAnnotationIntrospector();

        for (PotentialCreator candidate : potentials) {
            final int argCount = candidate.paramCount();
            final AnnotatedWithParams ctor = candidate.creator();
            // some single-arg Constructors (String, number) are auto-detected
            if (argCount == 1) {
                // 29-May-2024, tatu: Last arg is "true" for 3.0 (for some reason)
                /*boolean added = */ _handleSingleArgumentCreator(creators,
                        ctor, false,
                        true); // not-annotated, yes, visible
//                        vchecker.isCreatorVisible(ctor));
                // regardless, fully handled
                continue;
            }

            // 2 or more args; all params must have names or be injectable
            // 14-Mar-2015, tatu (2.6): Or, as per [#725], implicit names will also
            //   do, with some constraints. But that will require bit post processing...

            SettableBeanProperty[] properties = new SettableBeanProperty[argCount];
            int injectCount = 0;

            for (int i = 0; i < argCount; ++i) {
                final AnnotatedParameter param = ctor.getParameter(i);
                JacksonInject.Value injectable = intr.findInjectableValue(config, param);

                if (injectable != null) {
                    ++injectCount;
                    properties[i] = constructCreatorProperty(ctxt, beanDescRef, null, i, param, injectable);
                    continue;
                }
                NameTransformer unwrapper = intr.findUnwrappingNameTransformer(config, param);
                if (unwrapper != null) {
                    properties[i] = constructCreatorProperty(ctxt, beanDescRef,
                            UnwrappedPropertyHandler.creatorParamName(i), i, param, null);
                }
            }

            if ((injectCount + 1) == argCount) {
                // Secondary: all but one injectable, one un-annotated (un-named)
                creators.addDelegatingCreator(ctor, false, properties, 0);
                continue;
            }
            // otherwise, fail? Or no?
            /*
            ctxt.reportBadTypeDefinition(beanDesc,
    "Delegating constructor has %d parameters (with %d Injectables): must have one and only one non-Injectable parameter",
    argCount, injectCount);
*/
        }
    }

    private void _addImplicitDelegatingFactories(DeserializationContext ctxt,
            VisibilityChecker vchecker,
            CreatorCollector creators,
            List<PotentialCreator> potentials)
    {
        for (PotentialCreator candidate : potentials) {
            final int argCount = candidate.paramCount();
            AnnotatedWithParams factory = candidate.creator();
            // some single-arg Factory methods (String, number) are auto-detected
            if (argCount == 1) {
                /*boolean added=*/ _handleSingleArgumentCreator(creators,
                        factory, false, vchecker.isCreatorVisible(factory));
            }
            // 2 and more args? Must be explicit, handled earlier
        }
    }

    /*
    /**********************************************************************
    /* Creator introspection: older (pre-2.18) helper methods
    /**********************************************************************
     */

    /**
     * Helper method called when there is the explicit "is-creator" with mode of "delegating"
     */
    private boolean _addExplicitDelegatingCreator(DeserializationContext ctxt,
            BeanDescription.Supplier beanDescRef, CreatorCollector creators,
            CreatorCandidate candidate)
    {
        // Somewhat simple: find injectable values, if any, ensure there is one
        // and just one delegated argument; report violations if any

        int ix = -1;
        final int argCount = candidate.paramCount();
        SettableBeanProperty[] properties = new SettableBeanProperty[argCount];
        // [databind#4688]: Should still accept 0-arg (explicitly delegated) creator
        //   for backwards-compatibility (worked in 2.17 and before)
        if (argCount == 0) {
            // "Convert" to property-based since that works well
            creators.addPropertyCreator(candidate.creator(), true, properties);
            return true;
        }
        for (int i = 0; i < argCount; ++i) {
            AnnotatedParameter param = candidate.parameter(i);
            JacksonInject.Value injectId = candidate.injection(i);
            if (injectId != null) {
                properties[i] = constructCreatorProperty(ctxt, beanDescRef, null, i, param, injectId);
                continue;
            }
            if (ix < 0) {
                ix = i;
                continue;
            }
            // Illegal to have more than one value to delegate to
            ctxt.reportBadTypeDefinition(beanDescRef,
                    "More than one argument (#%d and #%d) left as delegating for Creator %s: only one allowed",
                    ix, i, candidate);
        }
        // Also, let's require that one Delegating argument does exist
        if (ix < 0) {
            ctxt.reportBadTypeDefinition(beanDescRef,
                    "No argument left as delegating for Creator %s: exactly one required", candidate);
        }
        // 17-Jan-2018, tatu: as per [databind#1853] need to ensure we will distinguish
        //   "well-known" single-arg variants (String, int/long, boolean) from "generic" delegating...
        if (argCount == 1) {
            return _handleSingleArgumentCreator(creators, candidate.creator(), true, true);
        }
        creators.addDelegatingCreator(candidate.creator(), true, properties, ix);
        return true;
    }

    /**
     * Helper method called to add the single chosen "properties-based" Creator (if any).
     */
    private void _addSelectedPropertiesBasedCreator(DeserializationContext ctxt,
            BeanDescription.Supplier beanDescRef, CreatorCollector creators,
            CreatorCandidate candidate)
    {
        final DeserializationConfig config = ctxt.getConfig();
        final AnnotationIntrospector intr = config.getAnnotationIntrospector();
        final int paramCount = candidate.paramCount();
        SettableBeanProperty[] properties = new SettableBeanProperty[paramCount];
        int anySetterIx = -1;

        for (int i = 0; i < paramCount; ++i) {
            JacksonInject.Value injectId = candidate.injection(i);
            AnnotatedParameter param = candidate.parameter(i);
            PropertyName name = candidate.paramName(i);
            boolean isAnySetter = Boolean.TRUE.equals(ctxt.getAnnotationIntrospector().hasAnySetter(config, param));
            if (isAnySetter) {
                if (anySetterIx >= 0) {
                    ctxt.reportBadTypeDefinition(beanDescRef,
                            "More than one 'any-setter' specified (parameter #%d vs #%d)",
                            anySetterIx, i);
                } else {
                    anySetterIx = i;
                }
            } else if (name == null) {
                // 21-Sep-2017, tatu: Looks like we want to block accidental use of Unwrapped,
                //   as that will not work with Creators well at all
                NameTransformer unwrapper = intr.findUnwrappingNameTransformer(config, param);
                if (unwrapper != null) {
                    properties[i] = constructCreatorProperty(ctxt, beanDescRef,
                            UnwrappedPropertyHandler.creatorParamName(i), i, param, null);
                }
                // Must be injectable or have name; without either won't work
                if ((name == null) && (injectId == null)) {
                    ctxt.reportBadTypeDefinition(beanDescRef,
"Argument #%d of Creator %s has no property name (and is not Injectable): cannot use as property-based Creator",
                        i, candidate);
                }
            }
            properties[i] = constructCreatorProperty(ctxt, beanDescRef, name, i, param, injectId);
        }
        creators.addPropertyCreator(candidate.creator(), true, properties);
    }

    private boolean _handleSingleArgumentCreator(CreatorCollector creators,
            AnnotatedWithParams ctor, boolean isCreator, boolean isVisible)
    {
        // otherwise either 'simple' number, String, or general delegate:
        Class<?> type = ctor.getRawParameterType(0);
        if (type == String.class || type == CLASS_CHAR_SEQUENCE) {
            if (isCreator || isVisible) {
                creators.addStringCreator(ctor, isCreator);
            }
            return true;
        }
        if (type == int.class || type == Integer.class) {
            if (isCreator || isVisible) {
                creators.addIntCreator(ctor, isCreator);
            }
            return true;
        }
        if (type == long.class || type == Long.class) {
            if (isCreator || isVisible) {
                creators.addLongCreator(ctor, isCreator);
            }
            return true;
        }
        if (type == double.class || type == Double.class) {
            if (isCreator || isVisible) {
                creators.addDoubleCreator(ctor, isCreator);
            }
            return true;
        }
        if (type == boolean.class || type == Boolean.class) {
            if (isCreator || isVisible) {
                creators.addBooleanCreator(ctor, isCreator);
            }
            return true;
        }
        if (type == BigInteger.class) {
            if (isCreator || isVisible) {
                creators.addBigIntegerCreator(ctor, isCreator);
            }
        }
        if (type == BigDecimal.class) {
            if (isCreator || isVisible) {
                creators.addBigDecimalCreator(ctor, isCreator);
            }
        }
        // Delegating Creator ok iff it has @JsonCreator (etc)
        if (isCreator) {
            creators.addDelegatingCreator(ctor, isCreator, null, 0);
            return true;
        }
        return false;
    }

    /**
     * Method that will construct a property object that represents
     * a logical property passed via Creator (constructor or static
     * factory method)
     */
    protected SettableBeanProperty constructCreatorProperty(DeserializationContext ctxt,
            BeanDescription.Supplier beanDescRef, PropertyName name, int index,
            AnnotatedParameter param,
            JacksonInject.Value injectable)
    {
        final DeserializationConfig config = ctxt.getConfig();
        final AnnotationIntrospector intr = config.getAnnotationIntrospector();
        PropertyMetadata metadata;
        final PropertyName wrapperName;
        {
            if (intr == null) {
                metadata = PropertyMetadata.STD_REQUIRED_OR_OPTIONAL;
                wrapperName = null;
            } else {
                Boolean b = intr.hasRequiredMarker(config, param);
                String desc = intr.findPropertyDescription(config, param);
                Integer idx = intr.findPropertyIndex(config, param);
                String def = intr.findPropertyDefaultValue(config, param);
                metadata = PropertyMetadata.construct(b, desc, idx, def);
                wrapperName = intr.findWrapperName(config, param);
            }
        }
        JavaType type = resolveMemberAndTypeAnnotations(ctxt, param, param.getType());
        BeanProperty.Std property = new BeanProperty.Std(name, type,
                wrapperName, param, metadata);
        // Type deserializer: either comes from property (and already resolved)
        TypeDeserializer typeDeser = (TypeDeserializer) type.getTypeHandler();
        // or if not, based on type being referenced:
        if (typeDeser == null) {
            typeDeser = ctxt.findTypeDeserializer(type);
        }

        // 22-Sep-2019, tatu: for [databind#2458] need more work on getting metadata
        //   about SetterInfo, mergeability
        metadata = _getSetterInfo(config, property, metadata);

        // Note: contextualization of typeDeser _should_ occur in constructor of CreatorProperty
        // so it is not called directly here
        SettableBeanProperty prop = CreatorProperty.construct(name, type, property.getWrapperName(),
                typeDeser, beanDescRef.getClassAnnotations(), param, index, injectable,
                metadata);
        ValueDeserializer<?> deser = findDeserializerFromAnnotation(ctxt, param);
        if (deser == null) {
            deser = (ValueDeserializer<?>) type.getValueHandler();
        }
        if (deser != null) {
            // As per [databind#462] need to ensure we contextualize deserializer before passing it on
            deser = ctxt.handlePrimaryContextualization(deser, prop, type);
            prop = prop.withValueDeserializer(deser);
        }
        return prop;
    }

    /**
     * Helper method copied from {@code POJOPropertyBuilder} since that won't be
     * applied to creator parameters.
     */
    private PropertyMetadata _getSetterInfo(MapperConfig<?> config,
            BeanProperty prop, PropertyMetadata metadata)
    {
        final AnnotationIntrospector intr = config.getAnnotationIntrospector();

        boolean needMerge = true;
        Nulls valueNulls = null;
        Nulls contentNulls = null;

        // NOTE: compared to `POJOPropertyBuilder`, we only have access to creator
        // parameter, not other accessors, so code bit simpler
        AnnotatedMember prim = prop.getMember();

        if (prim != null) {
            // Ok, first: does property itself have something to say?
            if (intr != null) {
                JsonSetter.Value setterInfo = intr.findSetterInfo(config, prim);
                if (setterInfo != null) {
                    valueNulls = setterInfo.nonDefaultValueNulls();
                    contentNulls = setterInfo.nonDefaultContentNulls();
                }
            }
            // If not, config override?
            // 25-Oct-2016, tatu: Either this, or type of accessor...
            if (needMerge || (valueNulls == null) || (contentNulls == null)) {
                ConfigOverride co = config.getConfigOverride(prop.getType().getRawClass());
                JsonSetter.Value setterInfo = co.getNullHandling();
                if (setterInfo != null) {
                    if (valueNulls == null) {
                        valueNulls = setterInfo.nonDefaultValueNulls();
                    }
                    if (contentNulls == null) {
                        contentNulls = setterInfo.nonDefaultContentNulls();
                    }
                }
            }
        }
        if (needMerge || (valueNulls == null) || (contentNulls == null)) {
            JsonSetter.Value setterInfo = config.getDefaultNullHandling();
            if (valueNulls == null) {
                valueNulls = setterInfo.nonDefaultValueNulls();
            }
            if (contentNulls == null) {
                contentNulls = setterInfo.nonDefaultContentNulls();
            }
        }
        if ((valueNulls != null) || (contentNulls != null)) {
            metadata = metadata.withNulls(valueNulls, contentNulls);
        }
        return metadata;
    }

    /*
    protected PropertyName _findImplicitParamName(AnnotatedParameter param, AnnotationIntrospector intr)
    {
        String str = intr.findImplicitPropertyName(param);
        if (str != null && !str.isEmpty()) {
            return PropertyName.construct(str);
        }
        return null;
    }

    protected boolean _checkIfCreatorPropertyBased(AnnotationIntrospector intr,
            AnnotatedWithParams creator, BeanPropertyDefinition propDef)
    {
        // If explicit name, or inject id, property-based
        if (((propDef != null) && propDef.isExplicitlyNamed())
                || (intr.findInjectableValue(creator.getParameter(0)) != null)) {
            return true;
        }
        if (propDef != null) {
            // One more thing: if implicit name matches property with a getter
            // or field, we'll consider it property-based as well
            String implName = propDef.getName();
            if (implName != null && !implName.isEmpty()) {
                if (propDef.couldSerialize()) {
                    return true;
                }
            }
        }
        // in absence of everything else, default to delegating
        return false;
    }
*/

    /*
    /**********************************************************************
    /* DeserializerFactory impl: array deserializers
    /**********************************************************************
     */

    @Override
    public ValueDeserializer<?> createArrayDeserializer(DeserializationContext ctxt,
            ArrayType type, BeanDescription.Supplier beanDescRef)
    {
        final DeserializationConfig config = ctxt.getConfig();
        final JavaType elemType = type.getContentType();

        // Very first thing: is deserializer hard-coded for elements?
        @SuppressWarnings("unchecked")
        final ValueDeserializer<Object> contentDeser = (ValueDeserializer<Object>) elemType.getValueHandler();
        // Then optional type info: if type has been resolved, we may already know type deserializer:
        final TypeDeserializer elemTypeDeser = _findContentTypeDeserializer(ctxt, elemType);

        // 23-Nov-2010, tatu: Custom array deserializer?
        ValueDeserializer<?>  deser = _findCustomArrayDeserializer(type,
                config, beanDescRef, elemTypeDeser, contentDeser);
        if (deser == null) {
            if (contentDeser == null) {
                if (elemType.isPrimitive()) {
                    deser = PrimitiveArrayDeserializers.forType(elemType.getRawClass());
                } else if (elemType.hasRawClass(String.class)) {
                    deser = StringArrayDeserializer.instance;
                }
            }
            if (deser == null) {
                deser = new ObjectArrayDeserializer(type, contentDeser, elemTypeDeser);
            }
        }
        // and then new with 2.2: ability to post-process it too (databind#120)
        if (_factoryConfig.hasDeserializerModifiers()) {
            for (ValueDeserializerModifier mod : _factoryConfig.deserializerModifiers()) {
                deser = mod.modifyArrayDeserializer(config, type, beanDescRef, deser);
            }
        }
        return deser;
    }

    /*
    /**********************************************************************
    /* DeserializerFactory impl: Collection(-like) deserializers
    /**********************************************************************
     */

    @SuppressWarnings("unchecked")
    @Override
    public ValueDeserializer<?> createCollectionDeserializer(DeserializationContext ctxt,
            CollectionType type, BeanDescription.Supplier beanDescRef)
    {
        final JavaType contentType = type.getContentType();
        final ValueDeserializer<Object> contentDeser = (ValueDeserializer<Object>) contentType.getValueHandler();
        final TypeDeserializer contentTypeDeser = _findContentTypeDeserializer(ctxt, contentType);
        final DeserializationConfig config = ctxt.getConfig();

        // 23-Nov-2010, tatu: Custom deserializer?
        ValueDeserializer<?> deser = _findCustomCollectionDeserializer(type,
                config, beanDescRef, contentTypeDeser, contentDeser);
        if (deser == null) {
            Class<?> collectionClass = type.getRawClass();
            if (contentDeser == null) { // not defined by annotation
                // [databind#1853]: Map `Set<ENUM>` to `EnumSet<ENUM>`
                if (contentType.isEnumType() && (collectionClass == Set.class)) {
                    collectionClass = EnumSet.class;
                    type = (CollectionType) config.getTypeFactory().constructSpecializedType(type, collectionClass);
                }
                // One special type: EnumSet:
                if (EnumSet.class.isAssignableFrom(collectionClass)) {
                    deser = new EnumSetDeserializer(contentType, null);
                }
            }
        }

        /* One twist: if we are being asked to instantiate an interface or
         * abstract Collection, we need to either find something that implements
         * the thing, or give up.
         *
         * Note that we do NOT try to guess based on secondary interfaces
         * here; that would probably not work correctly since casts would
         * fail later on (as the primary type is not the interface we'd
         * be implementing)
         */
        if (deser == null) {
            if (type.isInterface() || type.isAbstract()) {
                CollectionType implType = _mapAbstractCollectionType(type, config);
                if (implType != null) {
                    type = implType;
                    // But if so, also need to re-check creators...
                    beanDescRef = ctxt.lazyIntrospectBeanDescriptionForCreation(type);
                }
            }
            if (deser == null) {
                ValueInstantiator inst = findValueInstantiator(ctxt, beanDescRef);
                if (!inst.canCreateUsingDefault()) {
                    // [databind#161]: No default constructor for ArrayBlockingQueue...
                    if (type.hasRawClass(ArrayBlockingQueue.class)) {
                        return new ArrayBlockingQueueDeserializer(type, contentDeser, contentTypeDeser, inst);
                    }
                    // 10-Jan-2017, tatu: `java.util.Collections` types need help:
                    deser = JavaUtilCollectionsDeserializers.findForCollection(ctxt, type);
                    if (deser != null) {
                        return deser;
                    }
                }
                // Can use more optimal deserializer if content type is String, so:
                if (contentType.hasRawClass(String.class)) {
                    // no value type deserializer because Strings are one of natural/native types:
                    deser = new StringCollectionDeserializer(type, contentDeser, inst);
                } else {
                    deser = new CollectionDeserializer(type, contentDeser, contentTypeDeser, inst);
                }
            }
        }
        // allow post-processing it too
        if (_factoryConfig.hasDeserializerModifiers()) {
            for (ValueDeserializerModifier mod : _factoryConfig.deserializerModifiers()) {
                deser = mod.modifyCollectionDeserializer(config, type, beanDescRef, deser);
            }
        }
        return deser;
    }

    protected CollectionType _mapAbstractCollectionType(JavaType type, DeserializationConfig config)
    {
        final Class<?> collectionClass = ContainerDefaultMappings.findCollectionFallback(type);
        if (collectionClass != null) {
            return (CollectionType) config.getTypeFactory()
                    .constructSpecializedType(type, collectionClass, true);
        }
        return null;
    }

    // Copied almost verbatim from "createCollectionDeserializer" -- should try to share more code
    @Override
    public ValueDeserializer<?> createCollectionLikeDeserializer(DeserializationContext ctxt,
            CollectionLikeType type, BeanDescription.Supplier beanDescRef)
    {
        final JavaType contentType = type.getContentType();
        // Very first thing: is deserializer hard-coded for elements?
        @SuppressWarnings("unchecked")
        ValueDeserializer<Object> contentDeser = (ValueDeserializer<Object>) contentType.getValueHandler();
        final DeserializationConfig config = ctxt.getConfig();
        final TypeDeserializer contentTypeDeser = _findContentTypeDeserializer(ctxt, contentType);

        ValueDeserializer<?> deser = _findCustomCollectionLikeDeserializer(type, config, beanDescRef,
                contentTypeDeser, contentDeser);
        if (deser != null) {
            // ability to post-process it too (databind#120)
            if (_factoryConfig.hasDeserializerModifiers()) {
                for (ValueDeserializerModifier mod : _factoryConfig.deserializerModifiers()) {
                    deser = mod.modifyCollectionLikeDeserializer(config, type, beanDescRef, deser);
                }
            }
        }
        return deser;
    }

    /*
    /**********************************************************************
    /* DeserializerFactory impl: Map(-like) deserializers
    /**********************************************************************
     */

    @Override
    public ValueDeserializer<?> createMapDeserializer(DeserializationContext ctxt,
            MapType type, BeanDescription.Supplier beanDescRef)
    {
        final DeserializationConfig config = ctxt.getConfig();
        final JavaType keyType = type.getKeyType();
        final JavaType contentType = type.getContentType();

        // First: is there annotation-specified deserializer for values?
        @SuppressWarnings("unchecked")
        ValueDeserializer<Object> contentDeser = (ValueDeserializer<Object>) contentType.getValueHandler();

        // Ok: need a key deserializer (null indicates 'default' here)
        KeyDeserializer keyDes = (KeyDeserializer) keyType.getValueHandler();
        // Then optional type info; either attached to type, or resolved separately:
        final TypeDeserializer contentTypeDeser = _findContentTypeDeserializer(ctxt, contentType);

        // 23-Nov-2010, tatu: Custom deserializer?
        ValueDeserializer<?> deser = _findCustomMapDeserializer(type, config, beanDescRef,
                keyDes, contentTypeDeser, contentDeser);

        if (deser == null) {
            // Value handling is identical for all, but EnumMap requires special handling for keys
            Class<?> mapClass = type.getRawClass();
            // [databind#1853]: Map `Map<ENUM,x>` to `EnumMap<ENUM,x>`
            if ((mapClass == Map.class) && keyType.isEnumType()) {
                mapClass = EnumMap.class;
                type = (MapType) config.getTypeFactory().constructSpecializedType(type, mapClass);
//                type = (MapType) config.getTypeFactory().constructMapType(mapClass, keyType, contentType);
            }
            if (EnumMap.class.isAssignableFrom(mapClass)) {
                ValueInstantiator inst;

                // 06-Mar-2017, tatu: Should only need to check ValueInstantiator for
                //    custom sub-classes, see [databind#1544]
                if (mapClass == EnumMap.class) {
                    inst = null;
                } else {
                    inst = findValueInstantiator(ctxt, beanDescRef);
                }
                if (!keyType.isEnumImplType()) {
                    throw new IllegalArgumentException("Cannot construct EnumMap; generic (key) type not available");
                }
                deser = new EnumMapDeserializer(type, inst, null,
                        contentDeser, contentTypeDeser, null);
            }

            // Otherwise, generic handler works ok.

            /* But there is one more twist: if we are being asked to instantiate
             * an interface or abstract Map, we need to either find something
             * that implements the thing, or give up.
             *
             * Note that we do NOT try to guess based on secondary interfaces
             * here; that would probably not work correctly since casts would
             * fail later on (as the primary type is not the interface we'd
             * be implementing)
             */
            if (deser == null) {
                if (type.isInterface() || type.isAbstract()) {
                    MapType implType = _mapAbstractMapType(type, config);
                    if (implType != null) {
                        type = (MapType) implType;
                        mapClass = type.getRawClass();
                        // But if so, also need to re-check creators...
                        beanDescRef = ctxt.lazyIntrospectBeanDescriptionForCreation(type);
                    }
                    // 11-Nov-2024, tatu: Related to [databind#4783] let's not fail on
                    //    abstract Maps so they can work with merge (or factory methods)
                } else {
                    // 10-Jan-2017, tatu: `java.util.Collections` types need help:
                    deser = JavaUtilCollectionsDeserializers.findForMap(ctxt, type);
                    if (deser != null) {
                        return deser;
                    }
                }
                if (deser == null) {
                    ValueInstantiator inst = findValueInstantiator(ctxt, beanDescRef);
                    // 01-May-2016, tatu: Which base type to use here gets tricky, since
                    //   most often it ought to be `Map` or `EnumMap`, but due to abstract
                    //   mapping it will more likely be concrete type like `HashMap`.
                    //   So, for time being, just pass `Map.class`
                    MapDeserializer md = new MapDeserializer(type, inst, keyDes, contentDeser, contentTypeDeser);
                    JsonIgnoreProperties.Value ignorals = config.getDefaultPropertyIgnorals(Map.class,
                            beanDescRef.getClassInfo());
                    Set<String> ignored = (ignorals == null) ? null
                            : ignorals.findIgnoredForDeserialization();
                    md.setIgnorableProperties(ignored);
                    JsonIncludeProperties.Value inclusions = config.getDefaultPropertyInclusions(Map.class,
                            beanDescRef.getClassInfo());
                    Set<String> included = inclusions == null ? null : inclusions.getIncluded();
                    md.setIncludableProperties(included);
                    deser = md;
                }
            }
        }
        if (_factoryConfig.hasDeserializerModifiers()) {
            for (ValueDeserializerModifier mod : _factoryConfig.deserializerModifiers()) {
                deser = mod.modifyMapDeserializer(config, type, beanDescRef, deser);
            }
        }
        return deser;
    }

    protected MapType _mapAbstractMapType(JavaType type, DeserializationConfig config)
    {
        final Class<?> mapClass = ContainerDefaultMappings.findMapFallback(type);
        if (mapClass != null) {
            return (MapType) config.getTypeFactory()
                    .constructSpecializedType(type, mapClass, true);
        }
        return null;
    }

    // Copied almost verbatim from "createMapDeserializer" -- should try to share more code
    @Override
    public ValueDeserializer<?> createMapLikeDeserializer(DeserializationContext ctxt,
            MapLikeType type, BeanDescription.Supplier beanDescRef)
    {
        final JavaType keyType = type.getKeyType();
        final JavaType contentType = type.getContentType();
        final DeserializationConfig config = ctxt.getConfig();

        // First: is there annotation-specified deserializer for values?
        @SuppressWarnings("unchecked")
        ValueDeserializer<Object> contentDeser = (ValueDeserializer<Object>) contentType.getValueHandler();

        // Ok: need a key deserializer (null indicates 'default' here)
        KeyDeserializer keyDes = (KeyDeserializer) keyType.getValueHandler();
        /* !!! 24-Jan-2012, tatu: NOTE: impls MUST use resolve() to find key deserializer!
        if (keyDes == null) {
            keyDes = p.findKeyDeserializer(config, keyType, property);
        }
        */
        // Then optional type info; either attached to type, or resolve separately:
        final TypeDeserializer contentTypeDeser = _findContentTypeDeserializer(ctxt, contentType);
        ValueDeserializer<?> deser = _findCustomMapLikeDeserializer(type, config,
                beanDescRef, keyDes, contentTypeDeser, contentDeser);
        if (deser != null) {
            // ability to post-process it too (Issue#120)
            if (_factoryConfig.hasDeserializerModifiers()) {
                for (ValueDeserializerModifier mod : _factoryConfig.deserializerModifiers()) {
                    deser = mod.modifyMapLikeDeserializer(config, type, beanDescRef, deser);
                }
            }
        }
        return deser;
    }

    /*
    /**********************************************************************
    /* DeserializerFactory impl: other types
    /**********************************************************************
     */

    /**
     * Factory method for constructing deserializers of {@link Enum} types.
     */
    @Override
    public ValueDeserializer<?> createEnumDeserializer(DeserializationContext ctxt,
            JavaType type, BeanDescription.Supplier beanDescRef)
    {
        final DeserializationConfig config = ctxt.getConfig();
        // 23-Nov-2010, tatu: Custom deserializer?
        ValueDeserializer<?> deser = _findCustomEnumDeserializer(type, config, beanDescRef);

        if (deser == null) {
            // 12-Feb-2020, tatu: while we can't really create real deserializer for `Enum.class`,
            //    it is necessary to allow it in one specific case: see [databind#2605] for details
            //    but basically it can be used as polymorphic base.
            //    We could check `type.getTypeHandler()` to look for that case but seems like we
            //    may as well simply create placeholder (AbstractDeserializer) regardless
            if (type.hasRawClass(Enum.class)) {
                return AbstractDeserializer.constructForNonPOJO(beanDescRef);
            }

            final Class<?> enumClass = type.getRawClass();
            ValueInstantiator valueInstantiator = _constructDefaultValueInstantiator(ctxt, beanDescRef);
            SettableBeanProperty[] creatorProps = (valueInstantiator == null) ? null
                    : valueInstantiator.getFromObjectArguments(config);
            // May have @JsonCreator for static factory method:
            for (AnnotatedMethod factory : beanDescRef.get().getFactoryMethods()) {
                if (_hasCreatorAnnotation(config, factory)) {
                    if (factory.getParameterCount() == 0) { // [databind#960]
                        deser = EnumDeserializer.deserializerForNoArgsCreator(config, enumClass, factory);
                        break;
                    }
                    Class<?> returnType = factory.getRawReturnType();
                    // usually should be class, but may be just plain Enum<?> (for Enum.valueOf()?)
                    if (!returnType.isAssignableFrom(enumClass)) {
                        ctxt.reportBadDefinition(type, String.format(
"Invalid `@JsonCreator` annotated Enum factory method [%s]: needs to return compatible type",
factory.toString()));
                    }
                    deser = EnumDeserializer.deserializerForCreator(
                        config, enumClass, factory, valueInstantiator, creatorProps,
                        constructEnumResolver(ctxt, enumClass, beanDescRef));
                    break;
                }
            }

            // Need to consider @JsonValue if one found
            if (deser == null) {
                deser = new EnumDeserializer(constructEnumResolver(ctxt, enumClass, beanDescRef),
                        config.isEnabled(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS),
                        constructEnumNamingStrategyResolver(config, beanDescRef.getClassInfo()),
                        // since 2.16
                        EnumResolver.constructUsingToString(config, beanDescRef.getClassInfo())
                );
            }
        }

        // and then post-process it too
        if (_factoryConfig.hasDeserializerModifiers()) {
            for (ValueDeserializerModifier mod : _factoryConfig.deserializerModifiers()) {
                deser = mod.modifyEnumDeserializer(config, type, beanDescRef, deser);
            }
        }
        return deser;
    }

    @Override
    public ValueDeserializer<?> createTreeDeserializer(DeserializationConfig config,
            JavaType nodeType, BeanDescription.Supplier beanDescRef)
    {
        // 23-Nov-2010, tatu: Custom deserializer?
        ValueDeserializer<?> custom = _findCustomTreeNodeDeserializer(nodeType, config,
                beanDescRef);
        if (custom != null) {
            return custom;
        }
        if (nodeType.isTypeOrSubTypeOf(JsonNode.class)) {
            return JsonNodeDeserializer.getDeserializer(nodeType.getRawClass());
        }
        // 20-May-2025, tatu: Is this ok... ?
        return null;
    }

    @Override
    public ValueDeserializer<?> createReferenceDeserializer(DeserializationContext ctxt,
            ReferenceType type, BeanDescription.Supplier beanDescRef)
    {
        JavaType contentType = type.getContentType();
        // Very first thing: is deserializer hard-coded for elements?
        @SuppressWarnings("unchecked")
        ValueDeserializer<Object> contentDeser = (ValueDeserializer<Object>) contentType.getValueHandler();
        final DeserializationConfig config = ctxt.getConfig();
        final TypeDeserializer contentTypeDeser = _findContentTypeDeserializer(ctxt, contentType);
        ValueDeserializer<?> deser = _findCustomReferenceDeserializer(type, config, beanDescRef,
                contentTypeDeser, contentDeser);

        if (deser == null) {
            // 19-Sep-2017, tatu: Java 8 Optional directly supported in 3.x:
            if (type.isTypeOrSubTypeOf(Optional.class)) {
                // Not sure this can really work but let's try:
                ValueInstantiator inst = type.hasRawClass(Optional.class) ? null
                        : findValueInstantiator(ctxt, beanDescRef);
                return new Jdk8OptionalDeserializer(type, inst, contentTypeDeser, contentDeser);
            }
            if (type.isTypeOrSubTypeOf(AtomicReference.class)) {
                // 23-Oct-2016, tatu: Note that subtypes are probably not supportable
                //    without either forcing merging (to avoid having to create instance)
                //    or something else...
                ValueInstantiator inst = type.hasRawClass(AtomicReference.class) ? null
                        : findValueInstantiator(ctxt, beanDescRef);
                return new AtomicReferenceDeserializer(type, inst, contentTypeDeser, contentDeser);
            }
            if (type.hasRawClass(OptionalInt.class)) {
                return new OptionalIntDeserializer();
            }
            if (type.hasRawClass(OptionalLong.class)) {
                return new OptionalLongDeserializer();
            }
            if (type.hasRawClass(OptionalDouble.class)) {
                return new OptionalDoubleDeserializer();
            }
        }
        if (deser != null) {
            // and then post-process
            if (_factoryConfig.hasDeserializerModifiers()) {
                for (ValueDeserializerModifier mod : _factoryConfig.deserializerModifiers()) {
                    deser = mod.modifyReferenceDeserializer(config, type, beanDescRef, deser);
                }
            }
        }
        return deser;
    }

    /*
    /**********************************************************************
    /* DeserializerFactory impl (partial): other deserializers
    /**********************************************************************
     */

    /**
     * Overridable method called after checking all other types.
     */
    protected ValueDeserializer<?> findOptionalStdDeserializer(DeserializationContext ctxt,
            JavaType type, BeanDescription.Supplier beanDescRef)
    {
        return OptionalHandlerFactory.instance.findDeserializer(ctxt.getConfig(), type);
    }

    /*
    /**********************************************************************
    /* DeserializerFactory impl (partial): key deserializers
    /**********************************************************************
     */

    @Override
    public KeyDeserializer createKeyDeserializer(DeserializationContext ctxt,
            JavaType type)
    {
        final DeserializationConfig config = ctxt.getConfig();
        final BeanDescription.Supplier beanDescRef = ctxt.lazyIntrospectBeanDescription(type);

        // [databind#2452]: Support `@JsonDeserialize(keyUsing = ...)`
        KeyDeserializer deser = findKeyDeserializerFromAnnotation(ctxt, beanDescRef.getClassInfo());

        if ((deser == null) && _factoryConfig.hasKeyDeserializers()) {
            for (KeyDeserializers d  : _factoryConfig.keyDeserializers()) {
                deser = d.findKeyDeserializer(type, config, beanDescRef);
                if (deser != null) {
                    break;
                }
            }
        }

        // the only non-standard thing is this:
        if (deser == null) {
            if (type.isEnumType()) {
                deser = _createEnumKeyDeserializer(ctxt, type);
            } else {
                deser = JDKKeyDeserializers.findStringBasedKeyDeserializer(ctxt, type);
            }
        }
        // and then post-processing
        if (deser != null) {
            if (_factoryConfig.hasDeserializerModifiers()) {
                for (ValueDeserializerModifier mod : _factoryConfig.deserializerModifiers()) {
                    deser = mod.modifyKeyDeserializer(config, type, deser);
                }
            }
        }
        return deser;
    }

    private KeyDeserializer _createEnumKeyDeserializer(DeserializationContext ctxt,
            JavaType type)
    {
        final DeserializationConfig config = ctxt.getConfig();
        Class<?> enumClass = type.getRawClass();

        BeanDescription.Supplier beanDescRef = ctxt.lazyIntrospectBeanDescription(type);
        final AnnotatedClass classInfo = beanDescRef.getClassInfo();

        // 24-Sep-2015, bim: a key deserializer is the preferred thing.
        KeyDeserializer des = findKeyDeserializerFromAnnotation(ctxt, beanDescRef.getClassInfo());
        if (des != null) {
            return des;
        } else {
            // 24-Sep-2015, bim: if no key deser, look for enum deserializer first, then a plain deser.
            ValueDeserializer<?> custom = _findCustomEnumDeserializer(type, config, beanDescRef);
            if (custom != null) {
                return JDKKeyDeserializers.constructDelegatingKeyDeserializer(config, type, custom);
            }
            ValueDeserializer<?> valueDesForKey = findDeserializerFromAnnotation(ctxt, classInfo);
            if (valueDesForKey != null) {
                return JDKKeyDeserializers.constructDelegatingKeyDeserializer(config, type, valueDesForKey);
            }
        }
        EnumResolver enumRes = constructEnumResolver(ctxt, enumClass, beanDescRef);
        EnumResolver byEnumNamingResolver = constructEnumNamingStrategyResolver(config, classInfo);
        EnumResolver byToStringResolver = EnumResolver.constructUsingToString(config, classInfo);
        EnumResolver byIndexResolver = EnumResolver.constructUsingIndex(config, classInfo);

        // May have @JsonCreator for static factory method
        for (AnnotatedMethod factory : beanDescRef.get().getFactoryMethods()) {
            if (_hasCreatorAnnotation(config, factory)) {
                int argCount = factory.getParameterCount();
                if (argCount == 1) {
                    Class<?> returnType = factory.getRawReturnType();
                    // usually should be class, but may be just plain Enum<?> (for Enum.valueOf()?)
                    if (returnType.isAssignableFrom(enumClass)) {
                        // note: mostly copied from 'EnumDeserializer.deserializerForCreator(...)'
                        if (factory.getRawParameterType(0) != String.class) {
                            // [databind#2725]: Should not error out because (1) there may be good creator
                            //   method and (2) this method may be valid for "regular" enum value deserialization
                            // (leaving aside potential for multiple conflicting creators)
//                            throw new IllegalArgumentException("Parameter #0 type for factory method ("+factory+") not suitable, must be java.lang.String");
                            continue;
                        }
                        if (config.canOverrideAccessModifiers()) {
                            ClassUtil.checkAndFixAccess(factory.getMember(),
                                    ctxt.isEnabled(MapperFeature.OVERRIDE_PUBLIC_ACCESS_MODIFIERS));
                        }
                        return JDKKeyDeserializers.constructEnumKeyDeserializer(enumRes, factory, byEnumNamingResolver, byToStringResolver, byIndexResolver);
                    }
                }
                throw new IllegalArgumentException("Unsuitable method ("+factory+") decorated with @JsonCreator (for Enum type "
                        +enumClass.getName()+")");
            }
        }
        // Also, need to consider @JsonValue, if one found
        return JDKKeyDeserializers.constructEnumKeyDeserializer(enumRes, byEnumNamingResolver, byToStringResolver, byIndexResolver);
    }

    /*
    /**********************************************************************
    /* DeserializerFactory impl: find explicitly supported types
    /**********************************************************************
     */

    /**
     * Method that can be used to check if databind module has deserializer
     * for given (likely JDK) type: explicit meaning that it is not automatically
     * generated for POJO.
     *<p>
     * This matches {@link Deserializers#hasDeserializerFor} method.
     *
     * @since 3.0
     */
    @Override
    public boolean hasExplicitDeserializerFor(DatabindContext ctxt,
            Class<?> valueType)
    {
        // First things first: unpeel Array types as the element type is
        // what we are interested in -- this because we will always support array
        // types via composition, and since array types are JDK provided (and hence
        // cannot be custom or customized).
        if (valueType.isArray()) {
            do {
                valueType = valueType.getComponentType();
            } while (valueType.isArray());
            // one special case: allow `Object[]`, but not `Object`; former
            // will not automatically cause issues with contents as they
            // must have type separately
            if (valueType == CLASS_OBJECT) {
                return true;
            }
        }

        // Yes, we handle all Enum types
        if (Enum.class.isAssignableFrom(valueType)) {
            return true;
        }
        // Numbers?
        final String clsName = valueType.getName();
        if (clsName.startsWith("java.")) {
            if (Collection.class.isAssignableFrom(valueType)) {
                return true;
            }
            if (Map.class.isAssignableFrom(valueType)) {
                return true;
            }
            if (Number.class.isAssignableFrom(valueType)) {
                return NumberDeserializers.find(valueType) != null;
            }
            if (JDKMiscDeserializers.hasDeserializerFor(valueType)
                    || (valueType == CLASS_STRING)
                    // note: number wrappers dealt with above
                    || (valueType == Boolean.class)
                    || (valueType == EnumMap.class)
                    || (valueType == AtomicReference.class)
                    ) {
                return true;
            }
            if (JDKDateDeserializers.hasDeserializerFor(valueType)) {
                return true;
            }
        } else if (clsName.startsWith("com.fasterxml.")) {
            return JsonNode.class.isAssignableFrom(valueType)
                   || (valueType == TokenBuffer.class);
        } else {
            return OptionalHandlerFactory.instance.hasDeserializerFor(valueType);
        }
        return false;
    }

    /*
    /**********************************************************************
    /* Extended API
    /**********************************************************************
     */

    /**
     * Helper method called to find one of default deserializers for "well-known"
     * platform types: JDK-provided types, and small number of public Jackson
     * API types.
     */
    public ValueDeserializer<?> findDefaultDeserializer(DeserializationContext ctxt,
            JavaType type, BeanDescription.Supplier beanDescRef)
    {
        Class<?> rawType = type.getRawClass();
        // Object ("untyped"), and as of 2.10 (see [databind#2115]), `java.io.Serializable`
        if ((rawType == CLASS_OBJECT) || (rawType == CLASS_SERIALIZABLE)) {
            // 11-Feb-2015, tatu: As per [databind#700] need to be careful wrt non-default Map, List.
            DeserializationConfig config = ctxt.getConfig();
            JavaType lt, mt;

            if (ctxt.getConfig().hasAbstractTypeResolvers()) {
                lt = _findRemappedType(config, List.class);
                mt = _findRemappedType(config, Map.class);
            } else {
                lt = mt = null;
            }
            return new UntypedObjectDeserializer(lt, mt);
        }
        // String and equivalents
        if (rawType == CLASS_STRING || rawType == CLASS_CHAR_SEQUENCE) {
            return StringDeserializer.instance;
        }
        if (rawType == CLASS_ITERABLE) {
            // [databind#199]: Can and should 'upgrade' to a Collection type:
            TypeFactory tf = ctxt.getTypeFactory();
            JavaType[] tps = tf.findTypeParameters(type, CLASS_ITERABLE);
            JavaType elemType = (tps == null || tps.length != 1) ? TypeFactory.unknownType() : tps[0];
            CollectionType ct = tf.constructCollectionType(Collection.class, elemType);
            // Should we re-introspect beanDesc? For now let's not...
            return createCollectionDeserializer(ctxt, ct, beanDescRef);
        }
        if (rawType == CLASS_MAP_ENTRY) {
            // 28-Apr-2015, tatu: TypeFactory does it all for us already so
            JavaType kt = type.containedTypeOrUnknown(0);
            JavaType vt = type.containedTypeOrUnknown(1);
            TypeDeserializer vts = (TypeDeserializer) vt.getTypeHandler();
            if (vts == null) {
                vts = ctxt.findTypeDeserializer(vt);
            }
            @SuppressWarnings("unchecked")
            ValueDeserializer<Object> valueDeser = (ValueDeserializer<Object>) vt.getValueHandler();
            KeyDeserializer keyDes = (KeyDeserializer) kt.getValueHandler();
            return new MapEntryDeserializer(type, keyDes, valueDeser, vts);
        }
        String clsName = rawType.getName();
        if (rawType.isPrimitive() || clsName.startsWith("java.")) {
            // Primitives/wrappers, other Numbers:
            ValueDeserializer<?> deser = NumberDeserializers.find(rawType);
            if (deser == null) {
                deser = JDKDateDeserializers.find(rawType, clsName);
            }
            if (deser != null) {
                return deser;
            }
        }
        // and a few Jackson types as well:
        if (rawType == TokenBuffer.class) {
            return new TokenBufferDeserializer();
        }
        ValueDeserializer<?> deser = findOptionalStdDeserializer(ctxt, type, beanDescRef);
        if (deser != null) {
            return deser;
        }
        return JDKMiscDeserializers.find(ctxt, rawType, clsName);
    }

    private JavaType _findRemappedType(DeserializationConfig config, Class<?> rawType)
    {
        JavaType type = config.mapAbstractType(config.constructType(rawType));
        return (type == null || type.hasRawClass(rawType)) ? null : type;
    }

    /*
    /**********************************************************************
    /* Helper methods, finding custom deserializers
    /**********************************************************************
     */

    protected ValueDeserializer<?> _findCustomTreeNodeDeserializer(JavaType type,
            DeserializationConfig config, BeanDescription.Supplier beanDescRef)
    {
        for (Deserializers d  : _factoryConfig.deserializers()) {
            ValueDeserializer<?> deser = d.findTreeNodeDeserializer(type, config, beanDescRef);
            if (deser != null) {
                return deser;
            }
        }
        return null;
    }

    protected ValueDeserializer<?> _findCustomReferenceDeserializer(ReferenceType type,
            DeserializationConfig config, BeanDescription.Supplier beanDescRef,
            TypeDeserializer contentTypeDeserializer, ValueDeserializer<?> contentDeserializer)
    {
        for (Deserializers d  : _factoryConfig.deserializers()) {
            ValueDeserializer<?> deser = d.findReferenceDeserializer(type, config, beanDescRef,
                    contentTypeDeserializer, contentDeserializer);
            if (deser != null) {
                return deser;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    protected ValueDeserializer<Object> _findCustomBeanDeserializer(JavaType type,
            DeserializationConfig config, BeanDescription.Supplier beanDescRef)
    {
        for (Deserializers d  : _factoryConfig.deserializers()) {
            ValueDeserializer<?> deser = d.findBeanDeserializer(type, config, beanDescRef);
            if (deser != null) {
                return (ValueDeserializer<Object>) deser;
            }
        }
        return null;
    }

    protected ValueDeserializer<?> _findCustomArrayDeserializer(ArrayType type,
            DeserializationConfig config, BeanDescription.Supplier beanDescRef,
            TypeDeserializer elementTypeDeserializer, ValueDeserializer<?> elementDeserializer)
    {
        for (Deserializers d  : _factoryConfig.deserializers()) {
            ValueDeserializer<?> deser = d.findArrayDeserializer(type, config,
                    beanDescRef, elementTypeDeserializer, elementDeserializer);
            if (deser != null) {
                return deser;
            }
        }
        return null;
    }

    protected ValueDeserializer<?> _findCustomCollectionDeserializer(CollectionType type,
            DeserializationConfig config, BeanDescription.Supplier beanDescRef,
            TypeDeserializer elementTypeDeserializer, ValueDeserializer<?> elementDeserializer)
    {
        for (Deserializers d  : _factoryConfig.deserializers()) {
            ValueDeserializer<?> deser = d.findCollectionDeserializer(type, config,
                    beanDescRef, elementTypeDeserializer, elementDeserializer);
            if (deser != null) {
                return deser;
            }
        }
        return null;
    }

    protected ValueDeserializer<?> _findCustomCollectionLikeDeserializer(CollectionLikeType type,
            DeserializationConfig config, BeanDescription.Supplier beanDescRef,
            TypeDeserializer elementTypeDeserializer, ValueDeserializer<?> elementDeserializer)
    {
        for (Deserializers d  : _factoryConfig.deserializers()) {
            ValueDeserializer<?> deser = d.findCollectionLikeDeserializer(type, config,
                    beanDescRef, elementTypeDeserializer, elementDeserializer);
            if (deser != null) {
                return deser;
            }
        }
        return null;
    }

    protected ValueDeserializer<?> _findCustomEnumDeserializer(JavaType type,
            DeserializationConfig config, BeanDescription.Supplier beanDescRef)
    {
        for (Deserializers d  : _factoryConfig.deserializers()) {
            ValueDeserializer<?> deser = d.findEnumDeserializer(type, config, beanDescRef);
            if (deser != null) {
                return deser;
            }
        }
        return null;
    }

    protected ValueDeserializer<?> _findCustomMapDeserializer(MapType type,
            DeserializationConfig config, BeanDescription.Supplier beanDescRef,
            KeyDeserializer keyDeserializer,
            TypeDeserializer elementTypeDeserializer, ValueDeserializer<?> elementDeserializer)
    {
        for (Deserializers d  : _factoryConfig.deserializers()) {
            ValueDeserializer<?> deser = d.findMapDeserializer(type, config, beanDescRef,
                    keyDeserializer, elementTypeDeserializer, elementDeserializer);
            if (deser != null) {
                return deser;
            }
        }
        return null;
    }

    protected ValueDeserializer<?> _findCustomMapLikeDeserializer(MapLikeType type,
            DeserializationConfig config, BeanDescription.Supplier beanDescRef,
            KeyDeserializer keyDeserializer,
            TypeDeserializer elementTypeDeserializer, ValueDeserializer<?> elementDeserializer)
    {
        for (Deserializers d  : _factoryConfig.deserializers()) {
            ValueDeserializer<?> deser = d.findMapLikeDeserializer(type, config, beanDescRef,
                    keyDeserializer, elementTypeDeserializer, elementDeserializer);
            if (deser != null) {
                return deser;
            }
        }
        return null;
    }

    /*
    /**********************************************************************
    /* Helper methods, value/content/key type introspection
    /**********************************************************************
     */

    /**
     * Helper method called to check if a class or method
     * has annotation that tells which class to use for deserialization; and if
     * so, to instantiate, that deserializer to use.
     * Note that deserializer will NOT yet be contextualized so caller needs to
     * take care to call contextualization appropriately.
     * Returns null if no such annotation found.
     */
    protected ValueDeserializer<Object> findDeserializerFromAnnotation(DeserializationContext ctxt,
            Annotated ann)
    {
        AnnotationIntrospector intr = ctxt.getAnnotationIntrospector();
        if (intr != null) {
            Object deserDef = intr.findDeserializer(ctxt.getConfig(), ann);
            if (deserDef != null) {
                return ctxt.deserializerInstance(ann, deserDef);
            }
        }
        return null;
    }

    /**
     * Helper method called to check if a class or method
     * has annotation that tells which class to use for deserialization of {@link java.util.Map} keys.
     * Returns null if no such annotation found.
     */
    protected KeyDeserializer findKeyDeserializerFromAnnotation(DeserializationContext ctxt,
            Annotated ann)
    {
        AnnotationIntrospector intr = ctxt.getAnnotationIntrospector();
        if (intr != null) {
            Object deserDef = intr.findKeyDeserializer(ctxt.getConfig(), ann);
            if (deserDef != null) {
                return ctxt.keyDeserializerInstance(ann, deserDef);
            }
        }
        return null;
    }

    protected ValueDeserializer<Object> findContentDeserializerFromAnnotation(DeserializationContext ctxt,
            Annotated ann)
    {
        AnnotationIntrospector intr = ctxt.getAnnotationIntrospector();
        if (intr != null) {
            Object deserDef = intr.findContentDeserializer(ctxt.getConfig(), ann);
            if (deserDef != null) {
                return ctxt.deserializerInstance(ann, deserDef);
            }
        }
        return null;
    }

    /**
     * Helper method used to resolve additional type-related annotation information
     * like type overrides, or handler (serializer, deserializer) overrides,
     * so that from declared field, property or constructor parameter type
     * is used as the base and modified based on annotations, if any.
     */
    protected JavaType resolveMemberAndTypeAnnotations(DeserializationContext ctxt,
            AnnotatedMember member, JavaType type)
    {
        AnnotationIntrospector intr = ctxt.getAnnotationIntrospector();
        if (intr == null) {
            return type;
        }

        // First things first: see if we can find annotations on declared
        // type

        if (type.isMapLikeType()) {
            JavaType keyType = type.getKeyType();
            if (keyType != null) {
                Object kdDef = intr.findKeyDeserializer(ctxt.getConfig(), member);
                KeyDeserializer kd = ctxt.keyDeserializerInstance(member, kdDef);
                if (kd != null) {
                    type = ((MapLikeType) type).withKeyValueHandler(kd);
                    keyType = type.getKeyType(); // just in case it's used below
                }
            }
        }

        if (type.hasContentType()) { // that is, is either container- or reference-type
            Object cdDef = intr.findContentDeserializer(ctxt.getConfig(), member);
            ValueDeserializer<?> cd = ctxt.deserializerInstance(member, cdDef);
            if (cd != null) {
                type = type.withContentValueHandler(cd);
            }
            TypeDeserializer contentTypeDeser = ctxt.findPropertyContentTypeDeserializer(type,
                    (AnnotatedMember) member);
            if (contentTypeDeser != null) {
                type = type.withContentTypeHandler(contentTypeDeser);
            }
        }
        TypeDeserializer valueTypeDeser = ctxt.findPropertyTypeDeserializer(type, (AnnotatedMember) member);
        if (valueTypeDeser != null) {
            type = type.withTypeHandler(valueTypeDeser);
        }

        // Second part: find actual type-override annotations on member, if any

        // 18-Jun-2016, tatu: Should we re-do checks for annotations on refined
        //   subtypes as well? Code pre-2.8 did not do this, but if we get bug
        //   reports may need to consider
        type = intr.refineDeserializationType(ctxt.getConfig(), member, type);
        return type;
    }

    protected EnumResolver constructEnumResolver(DeserializationContext ctxt,
            Class<?> enumClass, BeanDescription.Supplier beanDescRef)
    {
        AnnotatedMember jvAcc = beanDescRef.get().findJsonValueAccessor();
        if (jvAcc != null) {
            if (ctxt.canOverrideAccessModifiers()) {
                ClassUtil.checkAndFixAccess(jvAcc.getMember(),
                        ctxt.isEnabled(MapperFeature.OVERRIDE_PUBLIC_ACCESS_MODIFIERS));
            }
            return EnumResolver.constructUsingMethod(ctxt.getConfig(), beanDescRef.getClassInfo(), jvAcc);
        }
        return EnumResolver.constructFor(ctxt.getConfig(), beanDescRef.getClassInfo());
    }

    /**
     * Factory method used to resolve an instance of {@link CompactStringObjectMap}
     * with {@link EnumNamingStrategy} applied for the target class.
     */
    protected EnumResolver constructEnumNamingStrategyResolver(DeserializationConfig config,
            AnnotatedClass enumClass)
    {
        Object namingDef = config.getAnnotationIntrospector().findEnumNamingStrategy(config, enumClass);
        EnumNamingStrategy enumNamingStrategy = EnumNamingStrategyFactory.createEnumNamingStrategyInstance(
            namingDef, config.canOverrideAccessModifiers(), config.getEnumNamingStrategy());
        return enumNamingStrategy == null ? null
            : EnumResolver.constructUsingEnumNamingStrategy(config, enumClass, enumNamingStrategy);
    }

    protected boolean _hasCreatorAnnotation(MapperConfig<?> config,
            Annotated ann) {
        AnnotationIntrospector intr = config.getAnnotationIntrospector();
        if (intr != null) {
            JsonCreator.Mode mode = intr.findCreatorAnnotation(config, ann);
            return (mode != null) && (mode != JsonCreator.Mode.DISABLED);
        }
        return false;
    }

    // @since 3.1
    protected TypeDeserializer _findContentTypeDeserializer(DeserializationContext ctxt,
            JavaType contentType)
    {
        // Then optional type info: if type has been resolved, we may already know type deserializer:
        TypeDeserializer contentTypeDeser = (TypeDeserializer) contentType.getTypeHandler();
        // [databind#1654]: @JsonTypeInfo(use = Id.NONE) should not apply type deserializer
        // when custom content deserializer is specified via @JsonDeserialize(contentUsing = ...)
        if (contentTypeDeser instanceof NoOpTypeDeserializer) {
            return null;
        }
        if (contentTypeDeser == null) {
            // but if not, may still be possible to find:
            contentTypeDeser = ctxt.findTypeDeserializer(contentType);
        }
        return contentTypeDeser;
    }
    
    /*
    /**********************************************************************
    /* Helper classes
    /**********************************************************************
     */

    /**
     * Helper class to contain default mappings for abstract JDK {@link java.util.Collection}
     * and {@link java.util.Map} types. Separated out here to defer cost of creating lookups
     * until mappings are actually needed.
     */
    @SuppressWarnings("rawtypes")
    protected static class ContainerDefaultMappings {
        // We do some defaulting for abstract Collection classes and
        // interfaces, to avoid having to use exact types or annotations in
        // cases where the most common concrete Collection will do.
        final static HashMap<String, Class<? extends Collection>> _collectionFallbacks;
        static {
            HashMap<String, Class<? extends Collection>> fallbacks = new HashMap<>();

            final Class<? extends Collection> DEFAULT_LIST = ArrayList.class;
            final Class<? extends Collection> DEFAULT_SET = HashSet.class;

            fallbacks.put(Collection.class.getName(), DEFAULT_LIST);
            fallbacks.put(List.class.getName(), DEFAULT_LIST);
            fallbacks.put(Set.class.getName(), DEFAULT_SET);
            fallbacks.put(SortedSet.class.getName(), TreeSet.class);
            fallbacks.put(Queue.class.getName(), LinkedList.class);

            // 09-Feb-2019, tatu: How did we miss these? Related in [databind#2251] problem
            fallbacks.put(AbstractList.class.getName(), DEFAULT_LIST);
            fallbacks.put(AbstractSet.class.getName(), DEFAULT_SET);

            // 09-Feb-2019, tatu: And more esoteric types added in JDK6
            fallbacks.put(Deque.class.getName(), LinkedList.class);
            fallbacks.put(NavigableSet.class.getName(), TreeSet.class);

            // Sequenced types added in JDK21
            fallbacks.put("java.util.SequencedCollection", DEFAULT_LIST);
            fallbacks.put("java.util.SequencedSet", LinkedHashSet.class);

            _collectionFallbacks = fallbacks;
        }

        // We do some defaulting for abstract Map classes and
        // interfaces, to avoid having to use exact types or annotations in
        // cases where the most common concrete Maps will do.
        final static HashMap<String, Class<? extends Map>> _mapFallbacks;
        static {
            HashMap<String, Class<? extends Map>> fallbacks = new HashMap<>();

            final Class<? extends Map> DEFAULT_MAP = LinkedHashMap.class;
            fallbacks.put(Map.class.getName(), DEFAULT_MAP);
            fallbacks.put(AbstractMap.class.getName(), DEFAULT_MAP);
            fallbacks.put(ConcurrentMap.class.getName(), ConcurrentHashMap.class);
            fallbacks.put(SortedMap.class.getName(), TreeMap.class);

            fallbacks.put(java.util.NavigableMap.class.getName(), TreeMap.class);
            fallbacks.put(java.util.concurrent.ConcurrentNavigableMap.class.getName(),
                    java.util.concurrent.ConcurrentSkipListMap.class);

            // Sequenced types added in JDK21
            fallbacks.put("java.util.SequencedMap", LinkedHashMap.class);

            _mapFallbacks = fallbacks;
        }

        public static Class<?> findCollectionFallback(JavaType type) {
            return _collectionFallbacks.get(type.getRawClass().getName());
        }

        public static Class<?> findMapFallback(JavaType type) {
            return _mapFallbacks.get(type.getRawClass().getName());
        }
    }
}
