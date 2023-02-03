package com.fasterxml.jackson.databind.jsontype.impl;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.databind.type.TypeFactory;

public class MinimalClassNameIdResolver
    extends ClassNameIdResolver
{
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

    protected MinimalClassNameIdResolver(JavaType baseType, TypeFactory typeFactory,
            PolymorphicTypeValidator ptv)
    {
        super(baseType, typeFactory, ptv);
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

    public static MinimalClassNameIdResolver construct(JavaType baseType, MapperConfig<?> config,
            PolymorphicTypeValidator ptv) {
        return new MinimalClassNameIdResolver(baseType, config.getTypeFactory(), ptv);
    }

    @Override
    public JsonTypeInfo.Id getMechanism() { return JsonTypeInfo.Id.MINIMAL_CLASS; }

    @Override
    public String idFromValue(Object value)
    {
        String n = value.getClass().getName();
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
