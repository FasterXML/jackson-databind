package com.fasterxml.jackson.databind.jsontype.impl;

import java.io.IOException;
import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.databind.type.TypeFactory;

/**
 * Specialization of {@link ClassNameIdResolver} that instead uses a
 * "minimal" derivation of {@link Class} name, using relative reference
 * from the base type (base class) that polymorphic value has.
 */
public class MinimalClassNameIdResolver
    extends ClassNameIdResolver
{
    private static final long serialVersionUID = 1L;

    /**
     * Package name of the base class, to be used for determining common
     * prefix that can be omitted from included type id.
     * Does not include the trailing dot.
     */
    protected final String _basePackageName;

    /**
     * Same as {@link #_basePackageName}, but includes trailing dot.
     */
    protected final String _basePackagePrefix;

    /**
     * @deprecated since 2.19
     */
    @Deprecated
    protected MinimalClassNameIdResolver(JavaType baseType, TypeFactory typeFactory,
            PolymorphicTypeValidator ptv)
    {
        this(baseType, typeFactory, null, ptv);
    }

    /**
     * @since 2.19
     */
    protected MinimalClassNameIdResolver(JavaType baseType, TypeFactory typeFactory,
            Collection<NamedType> subtypes,
            PolymorphicTypeValidator ptv)
    {
        super(baseType, typeFactory, subtypes, ptv);
        String base = baseType.getRawClass().getName();
        int ix = base.lastIndexOf('.');
        if (ix < 0) { // can this ever occur?
            _basePackageName = "";
            _basePackagePrefix = ".";
        } else {
            _basePackagePrefix = base.substring(0, ix+1);
            _basePackageName = base.substring(0, ix);
        }
    }

    /**
     * @deprecated since 2.19
     */
    @Deprecated
    public static MinimalClassNameIdResolver construct(JavaType baseType, MapperConfig<?> config,
            PolymorphicTypeValidator ptv) {
        return new MinimalClassNameIdResolver(baseType, config.getTypeFactory(), ptv);
    }

    /**
     * @since 2.19
     */
    public static MinimalClassNameIdResolver construct(JavaType baseType, MapperConfig<?> config,
            Collection<NamedType> subtypes,
            PolymorphicTypeValidator ptv) {
        return new MinimalClassNameIdResolver(baseType, config.getTypeFactory(), subtypes, ptv);
    }

    @Override
    public JsonTypeInfo.Id getMechanism() { return JsonTypeInfo.Id.MINIMAL_CLASS; }

    @Override
    public String idFromValue(Object value)
    {
        return idFromValueAndType(value, value.getClass());
    }

    @Override
    public String idFromValueAndType(Object value, Class<?> rawType) {
        // 04-Nov-2024, tatu: [databind#4733] Need to resolve enum sub-classes
        //   same way "ClassNameIdResolver" does
        rawType = _resolveToParentAsNecessary(rawType);
        String n = rawType.getName();
        if (n.startsWith(_basePackagePrefix)) {
            // note: we will leave the leading dot in there
            return n.substring(_basePackagePrefix.length()-1);
        }
        return n;
        
    }
    
    @Override
    protected JavaType _typeFromId(String id, DatabindContext ctxt) throws IOException
    {
        if (id.startsWith(".")) {
            StringBuilder sb = new StringBuilder(id.length() + _basePackageName.length());
            if  (_basePackageName.isEmpty()) {
                // no package; must remove leading '.' from id
                sb.append(id.substring(1));
            } else {
                // otherwise just concatenate package, with leading-dot-partial name
                sb.append(_basePackageName).append(id);
            }
            id = sb.toString();
        }
        return super._typeFromId(id, ctxt);
    }
}
