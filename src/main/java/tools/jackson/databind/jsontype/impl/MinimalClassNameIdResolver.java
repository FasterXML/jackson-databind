package tools.jackson.databind.jsontype.impl;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.DatabindContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;

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

    protected MinimalClassNameIdResolver(JavaType baseType,
            PolymorphicTypeValidator ptv)
    {
        super(baseType, ptv);
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

    public static MinimalClassNameIdResolver construct(JavaType baseType,
            PolymorphicTypeValidator ptv) {
        return new MinimalClassNameIdResolver(baseType, ptv);
    }

    @Override
    public JsonTypeInfo.Id getMechanism() { return JsonTypeInfo.Id.MINIMAL_CLASS; }

    @Override
    public String idFromValue(DatabindContext ctxt, Object value)
    {
        String n = value.getClass().getName();
        if (n.startsWith(_basePackagePrefix)) {
            // note: we will leave the leading dot in there
            return n.substring(_basePackagePrefix.length()-1);
        }
        return n;
    }

    @Override
    protected JavaType _typeFromId(DatabindContext ctxt, String id) throws JacksonException
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
        return super._typeFromId(ctxt, id);
    }
}
