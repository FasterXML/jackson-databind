package com.fasterxml.jackson.databind.jsontype.impl;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.ClassUtil;

/**
 * {@link com.fasterxml.jackson.databind.jsontype.TypeIdResolver} implementation
 * that converts between fully-qualified
 * Java class names and (JSON) Strings.
 */
public class ClassNameIdResolver
    extends TypeIdResolverBase
    implements java.io.Serializable // @since 2.16.2
{
    private static final long serialVersionUID = 1L;

    private final static String JAVA_UTIL_PKG = "java.util.";

    protected final PolymorphicTypeValidator _subTypeValidator;

    /**
     * @since 2.19 (to support {@code DeserializationFeature.FAIL_ON_SUBTYPE_CLASS_NOT_REGISTERED})
     */
    protected final Set<String> _allowedSubtypes;

    /**
     * @deprecated Since 2.10 use variant that takes {@link PolymorphicTypeValidator}
     */
    @Deprecated
    protected ClassNameIdResolver(JavaType baseType, TypeFactory typeFactory) {
        this(baseType, typeFactory, LaissezFaireSubTypeValidator.instance);
    }

    /**
     * @since 2.10
     * @deprecated Since 2.19 use variant that takes {@code Collection<NamedType>}
     */
    @Deprecated
    public ClassNameIdResolver(JavaType baseType, TypeFactory typeFactory,
            PolymorphicTypeValidator ptv) {
        this(baseType, typeFactory, null, ptv);
    }

    /**
     * @since 2.19
     */
    public ClassNameIdResolver(JavaType baseType, TypeFactory typeFactory,
            Collection<NamedType> subtypes, PolymorphicTypeValidator ptv) {
        super(baseType, typeFactory);
        _subTypeValidator = ptv;
        Set<String> allowedSubtypes = null;
        if (subtypes != null) {
            for (NamedType t : subtypes) {
                if (allowedSubtypes == null) {
                    allowedSubtypes = new HashSet<>();                    
                }
                allowedSubtypes.add(t.getType().getName());
            }
        }
        _allowedSubtypes = (allowedSubtypes == null) ? Collections.emptySet() : allowedSubtypes; 
    }

    /**
     * @deprecated since 2.19
     */
    @Deprecated
    public static ClassNameIdResolver construct(JavaType baseType,
            MapperConfig<?> config, PolymorphicTypeValidator ptv) {
        return new ClassNameIdResolver(baseType, config.getTypeFactory(), ptv);
    }

    /**
     * @since 2.19
     */
    public static ClassNameIdResolver construct(JavaType baseType, MapperConfig<?> config,
            Collection<NamedType> subtypes,
            PolymorphicTypeValidator ptv) {
        return new ClassNameIdResolver(baseType, config.getTypeFactory(), subtypes, ptv);
    }

    @Override
    public JsonTypeInfo.Id getMechanism() { return JsonTypeInfo.Id.CLASS; }

    // 28-Mar-2025, tatu: Why is this here; not overridden so... ?
    @Deprecated // since 2.19
    public void registerSubtype(Class<?> type, String name) {
        // not used with class name - based resolvers
    }

    @Override
    public String idFromValue(Object value) {
        return _idFrom(value, value.getClass(), _typeFactory);
    }

    @Override
    public String idFromValueAndType(Object value, Class<?> type) {
        return _idFrom(value, type, _typeFactory);
    }

    @Override
    public JavaType typeFromId(DatabindContext context, String id) throws IOException {
        return _typeFromId(id, context);
    }

    protected JavaType _typeFromId(String id, DatabindContext ctxt) throws IOException
    {
        DeserializationContext deserializationContext = null;
        if (ctxt instanceof DeserializationContext) {
            deserializationContext = (DeserializationContext) ctxt;
        }
        if ((_allowedSubtypes != null) && (deserializationContext != null)
                && deserializationContext.isEnabled(
                        DeserializationFeature.FAIL_ON_SUBTYPE_CLASS_NOT_REGISTERED)) {
            if (!_allowedSubtypes.contains(id)) {
                throw deserializationContext.invalidTypeIdException(_baseType, id,
"`DeserializationFeature.FAIL_ON_SUBTYPE_CLASS_NOT_REGISTERED` is enabled and the input class is not registered using `@JsonSubTypes` annotation");
            }
        }
        final JavaType t = ctxt.resolveAndValidateSubType(_baseType, id, _subTypeValidator);
        if (t == null && deserializationContext != null) {
            return deserializationContext.handleUnknownTypeId(_baseType, id, this, "no such class found");
        }
        return t;
    }

    /*
    /**********************************************************
    /* Internal methods
    /**********************************************************
     */

    protected String _idFrom(Object value, Class<?> cls, TypeFactory typeFactory)
    {
        cls = _resolveToParentAsNecessary(cls);
        String str = cls.getName();
        if (str.startsWith(JAVA_UTIL_PKG)) {
            // 25-Jan-2009, tatu: There are some internal classes that we cannot access as is.
            //     We need better mechanism; for now this has to do...

            // Enum sets and maps are problematic since we MUST know type of
            // contained enums, to be able to deserialize.
            // In addition, EnumSet is not a concrete type either
            if (value instanceof EnumSet<?>) { // Regular- and JumboEnumSet...
                Class<?> enumClass = ClassUtil.findEnumType((EnumSet<?>) value);
                // not optimal: but EnumSet is not a customizable type so this is sort of ok
               str = typeFactory.constructCollectionType(EnumSet.class, enumClass).toCanonical();
            } else if (value instanceof EnumMap<?,?>) {
                Class<?> enumClass = ClassUtil.findEnumType((EnumMap<?,?>) value);
                Class<?> valueClass = Object.class;
                // not optimal: but EnumMap is not a customizable type so this is sort of ok
                str = typeFactory.constructMapType(EnumMap.class, enumClass, valueClass).toCanonical();
            }
            // 10-Jan-2018, tatu: Up until 2.9.4 we used to have other conversions for `Collections.xxx()`
            //    and `Arrays.asList(...)`; but it was changed to be handled on receiving end instead
        } else if (str.indexOf('$') >= 0) {
            /* Other special handling may be needed for inner classes,
             * The best way to handle would be to find 'hidden' constructor; pass parent
             * value etc (which is actually done for non-anonymous static classes!),
             * but that is just not possible due to various things. So, we will instead
             * try to generalize type into something we will be more likely to be able
             * construct.
             */
            Class<?> outer = ClassUtil.getOuterClass(cls);
            if (outer != null) {
                // one more check: let's actually not worry if the declared static type is
                // non-static as well; if so, deserializer does have a chance at figuring it all out.
                Class<?> staticType = _baseType.getRawClass();
                if (ClassUtil.getOuterClass(staticType) == null) {
                    // Is this always correct? Seems like it should be...
                    cls = _baseType.getRawClass();
                    str = cls.getName();
                }
            }
        }
        return str;
    }

    @Override
    public String getDescForKnownTypeIds() {
        return "class name used as type id";
    }
}
