package com.fasterxml.jackson.databind.ext;

import java.util.HashMap;
import java.util.Map;
import javax.xml.bind.JAXBElement;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.ser.jdk.JavaUtilDateSerializer;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.databind.util.ClassUtil;

/**
 * Helper class used for isolating details of handling optional+external types
 * (javax.xml classes) from standard factories that offer them.
 *<p>
 * Note that with 3.0 need for separate class has been reduced somewhat
 * and this class may be eliminated.
 */
public class OptionalHandlerFactory
{
    /* To make 2 main "optional" handler groups (javax.xml.stream)
     * more dynamic, we better only figure out handlers completely dynamically, if and
     * when they are needed. To do this we need to assume package prefixes.
     */
    private final static String PACKAGE_PREFIX_JAVAX_XML = "javax.xml.";

    // // Since 2.7, we will assume DOM classes are always found, both due to JDK 1.6 minimum
    // // and because Android (and presumably GAE) have these classes

    private final static Class<?> CLASS_DOM_NODE = org.w3c.dom.Node.class;
    private final static Class<?> CLASS_DOM_DOCUMENT = org.w3c.dom.Document.class;

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
                "com.fasterxml.jackson.databind.ext.sql.JavaSqlDateDeserializer");
        _sqlDeserializers.put(CLS_NAME_JAVA_SQL_TIMESTAMP,
                "com.fasterxml.jackson.databind.ext.sql.JavaSqlTimestampDeserializer");
        // 09-Nov-2020, tatu: No deserializer for `java.sql.Blob` yet; would require additional
        //    dependency and not yet requested by anyone. Add if requested

        _sqlSerializers = new HashMap<>();
        // 09-Jan-2015, tatu: As per [databind#1073], let's try to guard against possibility
        //   of some environments missing `java.sql.` types

        // note: timestamps are very similar to java.util.Date, thus serialized as such
        _sqlSerializers.put(CLS_NAME_JAVA_SQL_TIMESTAMP, JavaUtilDateSerializer.instance);
        _sqlSerializers.put(CLS_NAME_JAVA_SQL_DATE, "com.fasterxml.jackson.databind.ext.sql.JavaSqlDateSerializer");
        _sqlSerializers.put(CLS_NAME_JAVA_SQL_TIME, "com.fasterxml.jackson.databind.ext.sql.JavaSqlTimeSerializer");

        // 09-Nov-2020, tatu: Not really optimal way to deal with these, problem  being that
        //   Blob is interface and actual instance we get is usually different. So may
        //   need to improve if we reported bugs. But for now, do this
        
        _sqlSerializers.put(CLS_NAME_JAVA_SQL_BLOB, "com.fasterxml.jackson.databind.ext.sql.JavaSqlBlobSerializer");
        _sqlSerializers.put(CLS_NAME_JAVA_SQL_SERIALBLOB, "com.fasterxml.jackson.databind.ext.sql.JavaSqlBlobSerializer");
    }

    /*
    /**********************************************************************
    /* Public API
    /**********************************************************************
     */

    public ValueSerializer<?> findSerializer(SerializationConfig config, JavaType type)
    {
        final Class<?> rawType = type.getRawClass();
        if (_IsXOfY(rawType, CLASS_DOM_NODE)) {
            return new DOMSerializer();
        }

        String className = rawType.getName();
        Object sqlHandler = _sqlSerializers.get(className);

        if (sqlHandler != null) {
            if (sqlHandler instanceof ValueSerializer<?>) {
                return (ValueSerializer<?>) sqlHandler;
            }
            // must be class name otherwise
            return (ValueSerializer<?>) instantiate((String) sqlHandler, type);
        }
        if (className.startsWith(PACKAGE_PREFIX_JAVAX_XML) || hasSuperClassStartingWith(rawType, PACKAGE_PREFIX_JAVAX_XML)) {
            if (Duration.class.isAssignableFrom(rawType) || QName.class.isAssignableFrom(rawType)) {
                return ToStringSerializer.instance;
            }
            if (XMLGregorianCalendar.class.isAssignableFrom(rawType)) {
                return XMLGregorianCalendarSerializer.instance;
            }
            if (JAXBElement.class.isAssignableFrom(rawType)) {
                return JAXBElementSerializer.instance;
            }
        }
        return null;
    }

    public ValueDeserializer<?> findDeserializer(DeserializationConfig config, JavaType type)
    {
        final Class<?> rawType = type.getRawClass();
        if (_IsXOfY(rawType, CLASS_DOM_NODE)) {
            return new DOMDeserializer.NodeDeserializer();
        }
        if (_IsXOfY(rawType, CLASS_DOM_DOCUMENT)) {
            return new DOMDeserializer.DocumentDeserializer();
        }
        String className = rawType.getName();
        final String deserName = _sqlDeserializers.get(className);
        if (deserName != null) {
            return (ValueDeserializer<?>) instantiate(deserName, type);
        }
        if (className.startsWith(PACKAGE_PREFIX_JAVAX_XML)
                || hasSuperClassStartingWith(rawType, PACKAGE_PREFIX_JAVAX_XML)) {
            return CoreXMLDeserializers.findBeanDeserializer(config, type);
        }
        return null;
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
            return CoreXMLDeserializers.hasDeserializerFor(valueType);
        }
        // 06-Nov-2020, tatu: One of "java.sql" types?
        return _sqlDeserializers.containsKey(className);
    }

    private boolean _IsXOfY(Class<?> valueType, Class<?> expType) {
        return (expType != null) && expType.isAssignableFrom(valueType);
    }

    /*
    /**********************************************************************
    /* Internal helper methods
    /**********************************************************************
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
     * super classes
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
