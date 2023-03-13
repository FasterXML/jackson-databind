package com.fasterxml.jackson.databind.ext;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.Deserializers;
import com.fasterxml.jackson.databind.ser.Serializers;
import com.fasterxml.jackson.databind.ser.std.DateSerializer;
import com.fasterxml.jackson.databind.util.ClassUtil;

/**
 * Helper class used for isolating details of handling optional+external types
 * (javax.xml classes) from standard factories that offer them.
 *<p>
 * Note that 2.7 changed handling to slightly less dynamic, to avoid having to
 * traverse class hierarchy, which turned to be a performance issue in
 * certain cases. Since DOM classes are assumed to exist on all Java 1.6
 * environments (yes, even on Android/GAE), this part could be simplified by
 * slightly less dynamic lookups.
 *<p>
 * Also with 2.7 we are supporting JDK 1.7/Java 7 type(s).
 */
public class OptionalHandlerFactory implements java.io.Serializable
{
    private static final long serialVersionUID = 1;

    /* To make 2 main "optional" handler groups (javax.xml.stream)
     * more dynamic, we better only figure out handlers completely dynamically, if and
     * when they are needed. To do this we need to assume package prefixes.
     */
    private final static String PACKAGE_PREFIX_JAVAX_XML = "javax.xml.";

    private final static String SERIALIZERS_FOR_JAVAX_XML = "com.fasterxml.jackson.databind.ext.CoreXMLSerializers";
    private final static String DESERIALIZERS_FOR_JAVAX_XML = "com.fasterxml.jackson.databind.ext.CoreXMLDeserializers";

    // Plus we also have a single serializer for DOM Node:
//    private final static String CLASS_NAME_DOM_NODE = "org.w3c.dom.Node";
//    private final static String CLASS_NAME_DOM_DOCUMENT = "org.w3c.dom.Document";
    private final static String SERIALIZER_FOR_DOM_NODE = "com.fasterxml.jackson.databind.ext.DOMSerializer";
    private final static String DESERIALIZER_FOR_DOM_DOCUMENT = "com.fasterxml.jackson.databind.ext.DOMDeserializer$DocumentDeserializer";
    private final static String DESERIALIZER_FOR_DOM_NODE = "com.fasterxml.jackson.databind.ext.DOMDeserializer$NodeDeserializer";

    // // Since 2.7, we will assume DOM classes are always found, both due to JDK 1.6 minimum
    // // and because Android (and presumably GAE) have these classes

    // // 02-Nov-2020, Xakep_SDK: java.xml module classes may be missing
    // // in actual runtime, if module java.xml is not present
    private final static Class<?> CLASS_DOM_NODE;
    private final static Class<?> CLASS_DOM_DOCUMENT;

    static {
        Class<?> doc = null, node = null;
        try {
            node = org.w3c.dom.Node.class;
            doc = org.w3c.dom.Document.class;
        } catch (Throwable e) {
            // not optimal but will do
            // 02-Nov-2020, Xakep_SDK: Remove java.logging module dependency
//            Logger.getLogger(OptionalHandlerFactory.class.getName())
//                .log(Level.INFO, "Could not load DOM `Node` and/or `Document` classes: no DOM support");
        }
        CLASS_DOM_NODE = node;
        CLASS_DOM_DOCUMENT = doc;
    }

    // // But Java7 type(s) may or may not be; dynamic lookup should be fine, still
    // // (note: also assume it comes from JDK so that ClassLoader issues with OSGi
    // // can, I hope, be avoided?)

    private static final Java7Handlers _jdk7Helper;
    static {
        Java7Handlers x = null;
        try {
            x = Java7Handlers.instance();
        } catch (Throwable t) { }
        _jdk7Helper = x;
    }

    public final static OptionalHandlerFactory instance = new OptionalHandlerFactory();

    // classes from java.sql module, this module may or may not be present at runtime
    // (is included on Java 8, but not part of JDK core for Java 9 and beyond)
    private final Map<String, String> _sqlDeserializers;
    private final Map<String, Object> _sqlSerializers;

    private final static String CLS_NAME_JAVA_SQL_TIMESTAMP = "java.sql.Timestamp";
    private final static String CLS_NAME_JAVA_SQL_DATE = "java.sql.Date";
    private final static String CLS_NAME_JAVA_SQL_TIME = "java.sql.Time";
    private final static String CLS_NAME_JAVA_SQL_BLOB = "java.sql.Blob";
    private final static String CLS_NAME_JAVA_SQL_SERIALBLOB = "javax.sql.rowset.serial.SerialBlob";

    protected OptionalHandlerFactory() {
        _sqlDeserializers = new HashMap<>();
        _sqlDeserializers.put(CLS_NAME_JAVA_SQL_DATE,
                "com.fasterxml.jackson.databind.deser.std.DateDeserializers$SqlDateDeserializer");
        _sqlDeserializers.put(CLS_NAME_JAVA_SQL_TIMESTAMP,
                "com.fasterxml.jackson.databind.deser.std.DateDeserializers$TimestampDeserializer");
        // 09-Nov-2020, tatu: No deserializer for `java.sql.Blob` yet; would require additional
        //    dependency and not yet requested by anyone. Add if requested

        _sqlSerializers = new HashMap<>();
        // 09-Jan-2015, tatu: As per [databind#1073], let's try to guard against possibility
        //   of some environments missing `java.sql.` types

        // note: timestamps are very similar to java.util.Date, thus serialized as such
        _sqlSerializers.put(CLS_NAME_JAVA_SQL_TIMESTAMP, DateSerializer.instance);
        _sqlSerializers.put(CLS_NAME_JAVA_SQL_DATE, "com.fasterxml.jackson.databind.ser.std.SqlDateSerializer");
        _sqlSerializers.put(CLS_NAME_JAVA_SQL_TIME, "com.fasterxml.jackson.databind.ser.std.SqlTimeSerializer");

        // 09-Nov-2020, tatu: Not really optimal way to deal with these, problem  being that
        //   Blob is interface and actual instance we get is usually different. So may
        //   need to improve if we reported bugs. But for now, do this

        _sqlSerializers.put(CLS_NAME_JAVA_SQL_BLOB, "com.fasterxml.jackson.databind.ext.SqlBlobSerializer");
        _sqlSerializers.put(CLS_NAME_JAVA_SQL_SERIALBLOB, "com.fasterxml.jackson.databind.ext.SqlBlobSerializer");
    }

    /*
    /**********************************************************
    /* Public API
    /**********************************************************
     */

    public JsonSerializer<?> findSerializer(SerializationConfig config, JavaType type,
            BeanDescription beanDesc)
    {
        final Class<?> rawType = type.getRawClass();

        if (_IsXOfY(rawType, CLASS_DOM_NODE)) {
            return (JsonSerializer<?>) instantiate(SERIALIZER_FOR_DOM_NODE, type);
        }

        if (_jdk7Helper != null) {
            JsonSerializer<?> ser = _jdk7Helper.getSerializerForJavaNioFilePath(rawType);
            if (ser != null) {
                return ser;
            }
        }

        String className = rawType.getName();
        Object sqlHandler = _sqlSerializers.get(className);

        if (sqlHandler != null) {
            if (sqlHandler instanceof JsonSerializer<?>) {
                return (JsonSerializer<?>) sqlHandler;
            }
            // must be class name otherwise
            return (JsonSerializer<?>) instantiate((String) sqlHandler, type);
        }

        String factoryName;
        if (className.startsWith(PACKAGE_PREFIX_JAVAX_XML) || hasSuperClassStartingWith(rawType, PACKAGE_PREFIX_JAVAX_XML)) {
            factoryName = SERIALIZERS_FOR_JAVAX_XML;
        } else {
            return null;
        }

        Object ob = instantiate(factoryName, type);
        if (ob == null) { // could warn, if we had logging system (j.u.l?)
            return null;
        }
        return ((Serializers) ob).findSerializer(config, type, beanDesc);
    }

    public JsonDeserializer<?> findDeserializer(JavaType type, DeserializationConfig config,
            BeanDescription beanDesc)
        throws JsonMappingException
    {
        final Class<?> rawType = type.getRawClass();

        if (_jdk7Helper != null) {
            JsonDeserializer<?> deser = _jdk7Helper.getDeserializerForJavaNioFilePath(rawType);
            if (deser != null) {
                return deser;
            }
        }
        if (_IsXOfY(rawType, CLASS_DOM_NODE)) {
            return (JsonDeserializer<?>) instantiate(DESERIALIZER_FOR_DOM_NODE, type);
        }
        if (_IsXOfY(rawType, CLASS_DOM_DOCUMENT)) {
            return (JsonDeserializer<?>) instantiate(DESERIALIZER_FOR_DOM_DOCUMENT, type);
        }
        String className = rawType.getName();
        final String deserName = _sqlDeserializers.get(className);
        if (deserName != null) {
            return (JsonDeserializer<?>) instantiate(deserName, type);
        }
        String factoryName;
        if (className.startsWith(PACKAGE_PREFIX_JAVAX_XML)
                || hasSuperClassStartingWith(rawType, PACKAGE_PREFIX_JAVAX_XML)) {
            factoryName = DESERIALIZERS_FOR_JAVAX_XML;
        } else {
            return null;
        }
        Object ob = instantiate(factoryName, type);
        if (ob == null) { // could warn, if we had logging system (j.u.l?)
            return null;
        }
        return ((Deserializers) ob).findBeanDeserializer(type, config, beanDesc);
    }

    public boolean hasDeserializerFor(Class<?> valueType) {
        if (_IsXOfY(valueType, CLASS_DOM_NODE)) {
            return true;
        }
        if (_IsXOfY(valueType, CLASS_DOM_DOCUMENT)) {
            return true;
        }
        String className = valueType.getName();
        if (className.startsWith(PACKAGE_PREFIX_JAVAX_XML)
                || hasSuperClassStartingWith(valueType, PACKAGE_PREFIX_JAVAX_XML)) {
            return true;
        }
        // 06-Nov-2020, tatu: One of "java.sql" types?
        return _sqlDeserializers.containsKey(className);
    }

    // @since 2.12.1
    private boolean _IsXOfY(Class<?> valueType, Class<?> expType) {
        return (expType != null) && expType.isAssignableFrom(valueType);
    }

    /*
    /**********************************************************
    /* Internal helper methods
    /**********************************************************
     */

    private Object instantiate(String className, JavaType valueType)
    {
        try {
            return instantiate(Class.forName(className), valueType);
        } catch (Throwable e) {
            throw new IllegalStateException("Failed to find class `"
+className+"` for handling values of type "+ClassUtil.getTypeDescription(valueType)
+", problem: ("+e.getClass().getName()+") "+e.getMessage());
        }
    }

    private Object instantiate(Class<?> handlerClass, JavaType valueType)
    {
        try {
            return ClassUtil.createInstance(handlerClass, false);
        } catch (Throwable e) {
            throw new IllegalStateException("Failed to create instance of `"
+handlerClass.getName()+"` for handling values of type "+ClassUtil.getTypeDescription(valueType)
+", problem: ("+e.getClass().getName()+") "+e.getMessage());
        }
    }

    /**
     * Since 2.7 we only need to check for class extension, as all implemented
     * types are classes, not interfaces. This has performance implications for
     * some cases, as we do not need to go over interfaces implemented, just
     * superclasses
     *
     * @since 2.7
     */
    private boolean hasSuperClassStartingWith(Class<?> rawType, String prefix)
    {
        for (Class<?> supertype = rawType.getSuperclass(); supertype != null; supertype = supertype.getSuperclass()) {
            if (supertype == Object.class) {
                return false;
            }
            if (supertype.getName().startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
