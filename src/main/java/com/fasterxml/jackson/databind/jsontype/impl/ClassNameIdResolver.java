package com.fasterxml.jackson.databind.jsontype.impl;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.ClassUtil;

/**
 * {@link com.fasterxml.jackson.databind.jsontype.TypeIdResolver} implementation
 * that converts between fully-qualified
 * Java class names and (JSON) Strings.
 */
public class ClassNameIdResolver
    extends TypeIdResolverBase
{
    public ClassNameIdResolver(JavaType baseType, TypeFactory typeFactory) {
        super(baseType, typeFactory);
    }

    @Override
    public JsonTypeInfo.Id getMechanism() { return JsonTypeInfo.Id.CLASS; }

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
        JavaType t = ctxt.resolveSubType(_baseType, id);
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
    /**********************************************************
    /* Internal methods
    /**********************************************************
     */

    protected String _idFrom(Object value, Class<?> cls, TypeFactory typeFactory)
    {
        // Need to ensure that "enum subtypes" work too
        if (Enum.class.isAssignableFrom(cls)) {
            if (!cls.isEnum()) { // means that it's sub-class of base enum, so:
                cls = cls.getSuperclass();
            }
        }
        String str = cls.getName();
        if (str.startsWith("java.util.")) {
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
            } else {
                // 17-Feb-2010, tatus: Another such case: result of Arrays.asList() is
                // named like so in Sun JDK... Let's just plain old ArrayList in its place.
                // ... also, other similar cases exist...
                String suffix = str.substring(10);
                if (isJavaUtilCollectionClass(suffix, "List")) {
                    str = ArrayList.class.getName();
                } else if(isJavaUtilCollectionClass(suffix, "Map")){
                    str = HashMap.class.getName();
                } else if(isJavaUtilCollectionClass(suffix, "Set")){
                    str = HashSet.class.getName();
                }
            }
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
                /* one more check: let's actually not worry if the declared
                 * static type is non-static as well; if so, deserializer does
                 * have a chance at figuring it all out.
                 */
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
    
    private static boolean isJavaUtilCollectionClass(String clz, String type){
        return (clz.startsWith("Collections$") || clz.startsWith("Arrays$"))
                && clz.indexOf(type) > 0;
    }
}
