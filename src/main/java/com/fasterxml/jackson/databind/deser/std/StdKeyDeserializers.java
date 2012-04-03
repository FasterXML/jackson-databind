package com.fasterxml.jackson.databind.deser.std;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.type.*;
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
{
    protected final HashMap<JavaType, KeyDeserializer> _keyDeserializers = new HashMap<JavaType, KeyDeserializer>();

    protected StdKeyDeserializers()
    {
        add(new StdKeyDeserializer.BoolKD());
        add(new StdKeyDeserializer.ByteKD());
        add(new StdKeyDeserializer.CharKD());
        add(new StdKeyDeserializer.ShortKD());
        add(new StdKeyDeserializer.IntKD());
        add(new StdKeyDeserializer.LongKD());
        add(new StdKeyDeserializer.FloatKD());
        add(new StdKeyDeserializer.DoubleKD());
        add(new StdKeyDeserializer.DateKD());
        add(new StdKeyDeserializer.CalendarKD());
        add(new StdKeyDeserializer.UuidKD());
        add(new StdKeyDeserializer.LocaleKD());
    }

    private void add(StdKeyDeserializer kdeser)
    {
        Class<?> keyClass = kdeser.getKeyClass();
        /* As with other cases involving primitive types, we can use
         * default TypeFactory ok, even if it's not our first choice:
         */
        _keyDeserializers.put(TypeFactory.defaultInstance().constructType(keyClass), kdeser);
    }

    public static HashMap<JavaType, KeyDeserializer> constructAll()
    {
        return new StdKeyDeserializers()._keyDeserializers;
    }

    /*
    /**********************************************************
    /* Dynamic factory methods
    /**********************************************************
     */

    public static KeyDeserializer constructStringKeyDeserializer(DeserializationConfig config, JavaType type) {
        return StdKeyDeserializer.StringKD.forType(type.getRawClass());
    }
    
    public static KeyDeserializer constructEnumKeyDeserializer(EnumResolver<?> enumResolver) {
        return new StdKeyDeserializer.EnumKD(enumResolver, null);
    }

    public static KeyDeserializer constructEnumKeyDeserializer(EnumResolver<?> enumResolver,
            AnnotatedMethod factory) {
        return new StdKeyDeserializer.EnumKD(enumResolver, factory);
    }
    
    public static KeyDeserializer constructDelegatingKeyDeserializer(DeserializationConfig config,
            JavaType type, JsonDeserializer<?> deser)
    {
        return new StdKeyDeserializer.DelegatingKD(type.getRawClass(), deser);
    }
    
    public static KeyDeserializer findStringBasedKeyDeserializer(DeserializationConfig config, JavaType type)
    {
        /* We don't need full deserialization information, just need to
         * know creators.
         */
        BeanDescription beanDesc = config.introspect(type);
        // Ok, so: can we find T(String) constructor?
        Constructor<?> ctor = beanDesc.findSingleArgConstructor(String.class);
        if (ctor != null) {
            if (config.canOverrideAccessModifiers()) {
                ClassUtil.checkAndFixAccess(ctor);
            }
            return new StdKeyDeserializer.StringCtorKeyDeserializer(ctor);
        }
        /* or if not, "static T valueOf(String)" (or equivalent marked
         * with @JsonCreator annotation?)
         */
        Method m = beanDesc.findFactoryMethod(String.class);
        if (m != null){
            if (config.canOverrideAccessModifiers()) {
                ClassUtil.checkAndFixAccess(m);
            }
            return new StdKeyDeserializer.StringFactoryKeyDeserializer(m);
        }
        // nope, no such luck...
        return null;
    }
}
