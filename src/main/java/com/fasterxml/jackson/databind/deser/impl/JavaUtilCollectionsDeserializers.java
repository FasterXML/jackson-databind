package com.fasterxml.jackson.databind.deser.impl;

import java.util.*;

import com.fasterxml.jackson.databind.*;
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

    // 2.12.1
    private final static int TYPE_SYNC_SET = 7;
    private final static int TYPE_SYNC_COLLECTION = 8;
    private final static int TYPE_SYNC_LIST = 9;
    private final static int TYPE_SYNC_MAP = 10;

    public final static int TYPE_AS_LIST = 11;

    private final static String PREFIX_JAVA_UTIL_COLLECTIONS = "java.util.Collections$";
    private final static String PREFIX_JAVA_UTIL_ARRAYS = "java.util.Arrays$";
    // for Java 11+ List.of(), Map.of() stuff
    private final static String PREFIX_JAVA_UTIL_IMMUTABLE_COLL = "java.util.ImmutableCollections$";

    public static JsonDeserializer<?> findForCollection(DeserializationContext ctxt,
            JavaType type)
        throws JsonMappingException
    {
        final String clsName = type.getRawClass().getName();
        if (!clsName.startsWith("java.util.")) {
            return null;
        }
        // 10-Jan-2017, tatu: Some types from `java.util.Collections`/`java.util.Arrays`
        //    need a bit of help...
        String localName = _findUtilCollectionsTypeName(clsName);
        if (localName != null) {
            JavaUtilCollectionsConverter conv = null;
            String name;

            if ((name = _findUnmodifiableTypeName(localName)) != null) {
                if (name.endsWith("Set")) {
                    conv = converter(TYPE_UNMODIFIABLE_SET, type, Set.class);
                } else if (name.endsWith("List")) {
                    conv = converter(TYPE_UNMODIFIABLE_LIST, type, List.class);
                }
            } else if ((name = _findSingletonTypeName(localName)) != null) {
                if (name.endsWith("Set")) {
                    conv = converter(TYPE_SINGLETON_SET, type, Set.class);
                } else if (name.endsWith("List")) {
                    conv = converter(TYPE_SINGLETON_LIST, type, List.class);
                }
            } else if ((name = _findSyncTypeName(localName)) != null) {
                // [databind#3009]: synchronized, too
                if (name.endsWith("Set")) {
                    conv = converter(TYPE_SYNC_SET, type, Set.class);
                } else if (name.endsWith("List")) {
                    conv = converter(TYPE_SYNC_LIST, type, List.class);
                } else if (name.endsWith("Collection")) {
                    conv = converter(TYPE_SYNC_COLLECTION, type, Collection.class);
                }
            }

            return (conv == null) ? null : new StdDelegatingDeserializer<Object>(conv);
        }
        if ((localName = _findUtilArrayTypeName(clsName)) != null) {
            // Typically ends with "List" but let's just look for it
            if (localName.contains("List")) {
                // 21-Aug-2022, tatu: [databind#3565] Let's try avoid making "Arrays.asList()"
                //    unmodifiable tho. Could either match "ArrayList" or just, well,
                //    default to "any" modifiable List:
                return new StdDelegatingDeserializer<Object>(
                        converter(TYPE_AS_LIST, type, List.class));
            }
            return null;
        }

        if ((localName = _findUtilCollectionsImmutableTypeName(clsName)) != null) {
            // NOTE: names are "List12" and "ListN" but... let's just look for "List"
            if (localName.contains("List")) {
                return new StdDelegatingDeserializer<Object>(
                        converter(TYPE_AS_LIST, type, List.class));
            }
            if (localName.contains("Set")) {
                return new StdDelegatingDeserializer<Object>(
                        converter(TYPE_UNMODIFIABLE_SET, type, Set.class));
            }
            return null;
        }

        return null;
    }

    public static JsonDeserializer<?> findForMap(DeserializationContext ctxt,
            JavaType type)
        throws JsonMappingException
    {
        final String clsName = type.getRawClass().getName();
        String localName;
        JavaUtilCollectionsConverter conv = null;

        if ((localName = _findUtilCollectionsTypeName(clsName)) != null) {
            String name;

            if ((name = _findUnmodifiableTypeName(localName)) != null) {
                if (name.contains("Map")) {
                    conv = converter(TYPE_UNMODIFIABLE_MAP, type, Map.class);
                }
            } else if ((name = _findSingletonTypeName(localName)) != null) {
                if (name.contains("Map")) {
                    conv = converter(TYPE_SINGLETON_MAP, type, Map.class);
                }
            } else if ((name = _findSyncTypeName(localName)) != null) {
                // [databind#3009]: synchronized, too
                if (name.contains("Map")) {
                    conv = converter(TYPE_SYNC_MAP, type, Map.class);
                }
            }
        } else if ((localName = _findUtilCollectionsImmutableTypeName(clsName)) != null) {
            if (localName.contains("Map")) {
                conv = converter(TYPE_UNMODIFIABLE_MAP, type, Map.class);
            }
        }
        return (conv == null) ? null : new StdDelegatingDeserializer<Object>(conv);
    }

    static JavaUtilCollectionsConverter converter(int kind,
            JavaType concreteType, Class<?> rawSuper)
    {
        return new JavaUtilCollectionsConverter(kind, concreteType.findSuperType(rawSuper));
    }

    private static String _findUtilArrayTypeName(String clsName) {
        if (clsName.startsWith(PREFIX_JAVA_UTIL_ARRAYS)) {
            return clsName.substring(PREFIX_JAVA_UTIL_ARRAYS.length());
        }
        return null;
    }

    private static String _findUtilCollectionsTypeName(String clsName) {
        if (clsName.startsWith(PREFIX_JAVA_UTIL_COLLECTIONS)) {
            return clsName.substring(PREFIX_JAVA_UTIL_COLLECTIONS.length());
        }
        return null;
    }

    private static String _findUtilCollectionsImmutableTypeName(String clsName) {
        if (clsName.startsWith(PREFIX_JAVA_UTIL_IMMUTABLE_COLL)) {
            return clsName.substring(PREFIX_JAVA_UTIL_IMMUTABLE_COLL.length());
        }
        return null;
    }

    private static String _findSingletonTypeName(String localName) {
        return localName.startsWith("Singleton") ? localName.substring(9): null;
    }

    private static String _findSyncTypeName(String localName) {
        return localName.startsWith("Synchronized") ? localName.substring(12): null;
    }

    private static String _findUnmodifiableTypeName(String localName) {
        return localName.startsWith("Unmodifiable") ? localName.substring(12): null;
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

        JavaUtilCollectionsConverter(int kind, JavaType inputType) {
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

            case TYPE_SYNC_SET:
                return Collections.synchronizedSet((Set<?>) value);
            case TYPE_SYNC_LIST:
                return Collections.synchronizedList((List<?>) value);
            case TYPE_SYNC_COLLECTION:
                return Collections.synchronizedCollection((Collection<?>) value);
            case TYPE_SYNC_MAP:
                return Collections.synchronizedMap((Map<?,?>) value);

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
