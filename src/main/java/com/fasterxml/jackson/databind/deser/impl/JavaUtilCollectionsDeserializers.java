package com.fasterxml.jackson.databind.deser.impl;

import java.util.*;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.std.StdDelegatingDeserializer;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.Converter;

/**
 * Helper class used to contain logic for deserializing "special" containers
 * from {@code java.util.Collections} and {@code java.util.Arrays}. This is needed
 * because they do not have usable no-arguments constructor: however, are easy enough
 * to deserialize using delegating deserializer.
 *
 * @since 2.9.4
 */
public abstract class JavaUtilCollectionsDeserializers
{
    private final static int TYPE_SINGLETON_SET = 1;
    private final static int TYPE_SINGLETON_LIST = 2;
    private final static int TYPE_SINGLETON_MAP = 3;

    private final static int TYPE_UNMODIFIABLE_SET = 4;
    private final static int TYPE_UNMODIFIABLE_LIST = 5;
    private final static int TYPE_UNMODIFIABLE_MAP = 6;

    public final static int TYPE_AS_LIST = 7;

    // 10-Jan-2018, tatu: There are a few "well-known" special containers in JDK too:

    private final static Class<?> CLASS_AS_ARRAYS_LIST = Arrays.asList(null, null).getClass();

    private final static Class<?> CLASS_SINGLETON_SET = Collections.singleton(Boolean.TRUE).getClass();
    private final static Class<?> CLASS_SINGLETON_LIST = Collections.singletonList(Boolean.TRUE).getClass();
    private final static Class<?> CLASS_SINGLETON_MAP = Collections.singletonMap("a", "b").getClass();

    public static JsonDeserializer<?> findForCollection(DeserializationContext ctxt,
            JavaType type)
        throws JsonMappingException
    {
        JavaUtilCollectionsConverter conv;

        // 10-Jan-2017, tatu: Some types from `java.util.Collections`/`java.util.Arrays` need bit of help...
        if (type.hasRawClass(CLASS_AS_ARRAYS_LIST)) {
            conv = converter(TYPE_AS_LIST, type);
        } else if (type.hasRawClass(CLASS_SINGLETON_LIST)) {
            conv = converter(TYPE_SINGLETON_LIST, type);
        } else if (type.hasRawClass(CLASS_SINGLETON_SET)) {
            conv = converter(TYPE_SINGLETON_SET, type);
        } else {
            return null;
        }
        return new StdDelegatingDeserializer<Object>(conv);
    }

    public static JsonDeserializer<?> findForMap(DeserializationContext ctxt,
            JavaType type)
        throws JsonMappingException
    {
        JavaUtilCollectionsConverter conv;

        // 10-Jan-2017, tatu: Some types from `java.util.Collections`/`java.util.Arrays` need bit of help...
        if (type.hasRawClass(CLASS_SINGLETON_MAP)) {
            conv = converter(TYPE_SINGLETON_MAP, type);
        } else {
            return null;
        }
        return new StdDelegatingDeserializer<Object>(conv);
    }
    
    static JavaUtilCollectionsConverter converter(int kind, JavaType concreteType)
    {
        JavaType inputType;

        switch (kind) {
        case TYPE_SINGLETON_SET:
        case TYPE_UNMODIFIABLE_SET:
            inputType = concreteType.findSuperType(Set.class);
            break;

        case TYPE_SINGLETON_MAP:
        case TYPE_UNMODIFIABLE_MAP:
            inputType = concreteType.findSuperType(Map.class);
            break;

        case TYPE_SINGLETON_LIST:
        case TYPE_UNMODIFIABLE_LIST:
        case TYPE_AS_LIST:
        default:
            inputType = concreteType.findSuperType(List.class);
            break;
        }
        return new JavaUtilCollectionsConverter(kind, inputType);
    }

    /**
     * Implementation used for converting from various generic container
     * types ({@link java.util.Set}, {@link java.util.List}, {@link java.util.Map})
     * into more specific implementations accessible via {@code java.util.Collections}.
     */
    private static class JavaUtilCollectionsConverter implements Converter<Object,Object>
    {
        private final JavaType _inputType;

        private final int _kind;

        private JavaUtilCollectionsConverter(int kind, JavaType inputType) {
            _inputType = inputType;
            _kind = kind;
        }
        
        @Override
        public Object convert(Object value) {
            if (value == null) { // is this legal to get?
                return null;
            }
            
            switch (_kind) {
            case TYPE_SINGLETON_SET:
                {
                    Set<?> set = (Set<?>) value;
                    _checkSingleton(set.size());
                    return Collections.singleton(set.iterator().next());
                }
            case TYPE_SINGLETON_LIST:
                {
                    List<?> list = (List<?>) value;
                    _checkSingleton(list.size());
                    return Collections.singletonList(list.get(0));
                }
            case TYPE_SINGLETON_MAP:
                {
                    Map<?,?> map = (Map<?,?>) value;
                    _checkSingleton(map.size());
                    Map.Entry<?,?> entry = map.entrySet().iterator().next();
                    return Collections.singletonMap(entry.getKey(), entry.getValue());
                }

            case TYPE_UNMODIFIABLE_SET:
                return Collections.unmodifiableSet((Set<?>) value);
            case TYPE_UNMODIFIABLE_LIST:
                return Collections.unmodifiableList((List<?>) value);
            case TYPE_UNMODIFIABLE_MAP:
                return Collections.unmodifiableMap((Map<?,?>) value);

            case TYPE_AS_LIST:
            default:
                // Here we do not actually care about impl type, just return List as-is:
                return value;
            }
        }

        @Override
        public JavaType getInputType(TypeFactory typeFactory) {
            return _inputType;
        }

        @Override
        public JavaType getOutputType(TypeFactory typeFactory) {
            // we don't actually care, so:
            return _inputType;
        }

        private void _checkSingleton(int size) {
            if (size != 1) {
                // not the best error ever but... has to do
                throw new IllegalArgumentException("Can not deserialize Singleton container from "+size+" entries");
            }
        }
    }
    
}
