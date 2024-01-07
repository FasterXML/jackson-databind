package tools.jackson.databind.jsontype.impl;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.*;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;
import tools.jackson.databind.util.ClassUtil;

/**
 * {@link tools.jackson.databind.jsontype.TypeIdResolver} implementation
 * that converts between fully-qualified
 * Java class names and (JSON) Strings.
 */
public class ClassNameIdResolver
    extends TypeIdResolverBase
    implements java.io.Serializable // @since 2.17
{
    private static final long serialVersionUID = 1L;

    private final static String JAVA_UTIL_PKG = "java.util.";

    protected final PolymorphicTypeValidator _subTypeValidator;

    public ClassNameIdResolver(JavaType baseType, PolymorphicTypeValidator ptv) {
        super(baseType);
        _subTypeValidator = ptv;
    }

    public static ClassNameIdResolver construct(JavaType baseType,
            PolymorphicTypeValidator ptv) {
        return new ClassNameIdResolver(baseType, ptv);
    }

    @Override
    public JsonTypeInfo.Id getMechanism() { return JsonTypeInfo.Id.CLASS; }

    public void registerSubtype(Class<?> type, String name) {
        // not used with class name - based resolvers
    }

    @Override
    public String idFromValue(DatabindContext ctxt, Object value) {
        return _idFrom(ctxt, value, value.getClass());
    }

    @Override
    public String idFromValueAndType(DatabindContext ctxt, Object value, Class<?> type) {
        return _idFrom(ctxt, value, type);
    }

    @Override
    public JavaType typeFromId(DatabindContext ctxt, String id) throws JacksonException {
        return _typeFromId(ctxt, id);
    }

    protected JavaType _typeFromId(DatabindContext ctxt, String id) throws JacksonException
    {
        // 24-Apr-2019, tatu: [databind#2195] validate as well as resolve:
        JavaType t = ctxt.resolveAndValidateSubType(_baseType, id, _subTypeValidator);
        if (t == null) {
            if (ctxt instanceof DeserializationContext) {
                // First: we may have problem handlers that can deal with it?
                return ((DeserializationContext) ctxt).handleUnknownTypeId(_baseType, id, this, "no such class found");
            }
            // ... meaning that we really should never get here.
        }
        return t;
    }

    /*
    /**********************************************************************
    /* Internal methods
    /**********************************************************************
     */

    protected String _idFrom(DatabindContext ctxt, Object value, Class<?> cls)
    {
        // Need to ensure that "enum subtypes" work too
        if (ClassUtil.isEnumType(cls)) {
            // 29-Sep-2019, tatu: `Class.isEnum()` only returns true for main declaration,
            //   but NOT from sub-class thereof (extending individual values). This
            //   is why additional resolution is needed: we want class that contains
            //   enumeration instances.
            if (!cls.isEnum()) {
                // and this parent would then have `Enum.class` as its parent:
                cls = cls.getSuperclass();
            }
        }
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
               str = ctxt.getTypeFactory().constructCollectionType(EnumSet.class, enumClass).toCanonical();
            } else if (value instanceof EnumMap<?,?>) {
                Class<?> enumClass = ClassUtil.findEnumType((EnumMap<?,?>) value);
                Class<?> valueClass = Object.class;
                // not optimal: but EnumMap is not a customizable type so this is sort of ok
                str = ctxt.getTypeFactory().constructMapType(EnumMap.class, enumClass, valueClass).toCanonical();
            }
            // 10-Jan-2018, tatu: Up until 2.9.4 we used to have other conversions for `Collections.xxx()`
            //    and `Arrays.asList(...)`; but it was changed to be handled on receiving end instead
        }
        // 04-Sep-2020, tatu: 2.x used to have weird work-around for inner classes,
        //   for some... "interesting" usage. Since it was added in 1.x for some now
        //   unknown issue, remove it from 3.x; may be re-added but only with better
        //   understanding of all the complexities.

        /*
        else if (str.indexOf('$') >= 0) {
            // Other special handling may be needed for inner classes,
            // The best way to handle would be to find 'hidden' constructor; pass parent
            // value etc (which is actually done for non-anonymous static classes!),
            // but that is just not possible due to various things. So, we will instead
            // try to generalize type into something we will be more likely to be able construct.
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
        */

        return str;
    }

    @Override
    public String getDescForKnownTypeIds() {
        return "{class name used as type id}";
    }
}
