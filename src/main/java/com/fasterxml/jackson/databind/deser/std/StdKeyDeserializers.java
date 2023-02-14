package com.fasterxml.jackson.databind.deser.std;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.KeyDeserializers;
import com.fasterxml.jackson.databind.introspect.AnnotatedAndMetadata;
import com.fasterxml.jackson.databind.introspect.AnnotatedConstructor;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.util.ClassUtil;
import com.fasterxml.jackson.databind.util.EnumResolver;

/**
 * Helper class used to contain simple/well-known key deserializers.
 * Following kinds of Objects can be handled currently:
 *<ul>
 * <li>Primitive wrappers (Boolean, Byte, Char, Short, Integer, Float, Long, Double)</li>
 * <li>Enums (usually not needed, since EnumMap doesn't call us)</li>
 * <li>{@link java.util.Date}</li>
 * <li>{@link java.util.Calendar}</li>
 * <li>{@link java.util.UUID}</li>
 * <li>{@link java.util.Locale}</li>
 * <li>Anything with constructor that takes a single String arg
 *   (if not explicitly @JsonIgnore'd)</li>
 * <li>Anything with {@code static T valueOf(String)} factory method
 *   (if not explicitly @JsonIgnore'd)</li>
 *</ul>
 */
public class StdKeyDeserializers
    implements KeyDeserializers, java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    public static KeyDeserializer constructEnumKeyDeserializer(EnumResolver enumResolver) {
        return new StdKeyDeserializer.EnumKD(enumResolver, null);
    }

    public static KeyDeserializer constructEnumKeyDeserializer(EnumResolver enumResolver,
            AnnotatedMethod factory) {
        return new StdKeyDeserializer.EnumKD(enumResolver, factory);
    }

    public static KeyDeserializer constructDelegatingKeyDeserializer(DeserializationConfig config,
            JavaType type, JsonDeserializer<?> deser)
    {
        return new StdKeyDeserializer.DelegatingKD(type.getRawClass(), deser);
    }

    public static KeyDeserializer findStringBasedKeyDeserializer(DeserializationConfig config,
            JavaType type)
        throws JsonMappingException
   {
        // 15-Jun-2021, tatu: As per [databind#3143], full introspection needs to consider
        //   as set of possibilities. Basically, precedence is:
        //
        //   1. Explicitly annotated 1-String-arg constructor, if one exists
        //   2. Explicitly annotated Factory method: just one allowed (exception if multiple)
        //   3. Implicit 1-String-arg constructor (no visibility checks for backwards
        //      compatibility reasons; should probably be checked in future, 3.0?)
        //   4. Implicit Factory method with name of "valueOf()" (primary) or
        //      "fromString()" (secondary). Likewise, no visibility check as of yet.

        // We don't need full deserialization information, just need to know creators.
        final BeanDescription beanDesc = config.introspectForCreation(type);
        // Ok, so: can we find T(String) constructor?
        final AnnotatedAndMetadata<AnnotatedConstructor, JsonCreator.Mode> ctorInfo = _findStringConstructor(beanDesc);
        // Explicit?
        if ((ctorInfo != null) && (ctorInfo.metadata != null)) {
            return _constructCreatorKeyDeserializer(config, ctorInfo.annotated);
        }
        // or if not, "static T valueOf(String)" (or equivalent marked
        // with @JsonCreator annotation?)
        final List<AnnotatedAndMetadata<AnnotatedMethod, JsonCreator.Mode>> factoryCandidates
            = beanDesc.getFactoryMethodsWithMode();

        // But must now filter out invalid candidates, both by signature (must take 1 and
        // only 1 arg; that arg must be of type `String`) and by annotations (we only
        // accept "delegating" style, so remove PROPERTIES)
        factoryCandidates.removeIf(m ->
            (m.annotated.getParameterCount() != 1)
                || (m.annotated.getRawParameterType(0) != String.class)
                || (m.metadata == JsonCreator.Mode.PROPERTIES)
                );

        // Any explicit?
        final AnnotatedMethod explicitFactory = _findExplicitStringFactoryMethod(factoryCandidates);
        if (explicitFactory != null) {
            return _constructCreatorKeyDeserializer(config, explicitFactory);
        }
        // If we had implicit Constructor, that'd work now
        if (ctorInfo != null) {
            return _constructCreatorKeyDeserializer(config, ctorInfo.annotated);
        }
        // And finally, if any implicit factory methods, acceptable now
        // nope, no such luck...
        if (!factoryCandidates.isEmpty()) {
            // 15-Jun-2021, tatu: Ideally we would provide stabler ordering, but for now
            //   let's simply pick the first one
            return _constructCreatorKeyDeserializer(config, factoryCandidates.get(0).annotated);
        }
        return null;
    }

    private static KeyDeserializer _constructCreatorKeyDeserializer(DeserializationConfig config,
            AnnotatedMember creator)
    {
        if (creator instanceof AnnotatedConstructor) {
            Constructor<?> rawCtor = ((AnnotatedConstructor) creator).getAnnotated();
            if (config.canOverrideAccessModifiers()) {
                ClassUtil.checkAndFixAccess(rawCtor, config.isEnabled(MapperFeature.OVERRIDE_PUBLIC_ACCESS_MODIFIERS));
            }
            return new StdKeyDeserializer.StringCtorKeyDeserializer(rawCtor);
        }
        Method m = ((AnnotatedMethod) creator).getAnnotated();
        if (config.canOverrideAccessModifiers()) {
            ClassUtil.checkAndFixAccess(m, config.isEnabled(MapperFeature.OVERRIDE_PUBLIC_ACCESS_MODIFIERS));
        }
        return new StdKeyDeserializer.StringFactoryKeyDeserializer(m);
    }

    // 13-Jun-2021, tatu: For now just look for constructor that takes one `String`
    //      argument (could look for CharSequence) and hence can have just one, no dups
    private static AnnotatedAndMetadata<AnnotatedConstructor, JsonCreator.Mode> _findStringConstructor(BeanDescription beanDesc)
    {
        for (AnnotatedAndMetadata<AnnotatedConstructor, JsonCreator.Mode> entry
                : beanDesc.getConstructorsWithMode())
        {
            // BeanDescription method does NOT filter out various types so check
            // it takes single argument.
            final AnnotatedConstructor ctor = entry.annotated;
            if ((ctor.getParameterCount() == 1)
                    && (String.class == ctor.getRawParameterType(0))) {
                return entry;
            }
        }
        return null;
    }

    private static AnnotatedMethod _findExplicitStringFactoryMethod(
            List<AnnotatedAndMetadata<AnnotatedMethod, JsonCreator.Mode>> candidates)
        throws JsonMappingException
    {
        AnnotatedMethod match = null;
        for (AnnotatedAndMetadata<AnnotatedMethod, JsonCreator.Mode> entry : candidates) {
            // Note: caller has filtered out invalid candidates; all we need to check are dups
            if (entry.metadata != null) {
                if (match == null) {
                    match = entry.annotated;
                } else {
                    // 15-Jun-2021, tatu: Not optimal type or information, but has to do for now
                    //    since we do not get DeserializationContext
                    Class<?> rawKeyType = entry.annotated.getDeclaringClass();
                    throw new IllegalArgumentException(
"Multiple suitable annotated Creator factory methods to be used as the Key deserializer for type "
                            +ClassUtil.nameOf(rawKeyType));
                }
            }
        }
        return match;
    }

    /*
    /**********************************************************
    /* KeyDeserializers implementation
    /**********************************************************
     */

    @Override
    public KeyDeserializer findKeyDeserializer(JavaType type,
            DeserializationConfig config, BeanDescription beanDesc) throws JsonMappingException
    {
        Class<?> raw = type.getRawClass();
        // 23-Apr-2013, tatu: Map primitive types, just in case one was given
        if (raw.isPrimitive()) {
            raw = ClassUtil.wrapperType(raw);
        }
        return StdKeyDeserializer.forType(raw);
    }
}
