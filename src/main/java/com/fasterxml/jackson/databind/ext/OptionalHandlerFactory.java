package com.fasterxml.jackson.databind.ext;

import java.util.HashMap;
import java.util.Map;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.fasterxml.jackson.databind.*;
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

    // classes from java.sql module, this module may not be present at runtime
    private final Map<String, String> _sqlClasses;

    protected OptionalHandlerFactory() {
        _sqlClasses = new HashMap<>();
        try {
            _sqlClasses.put("java.sql.Date",
                    "com.fasterxml.jackson.databind.deser.std.DateDeserializers$SqlDateDeserializer");
            _sqlClasses.put("java.sql.Timestamp",
                    "com.fasterxml.jackson.databind.deser.std.DateDeserializers$TimestampDeserializer");
        } catch (Throwable t) { }
    }

    /*
    /**********************************************************************
    /* Public API
    /**********************************************************************
     */

    public JsonSerializer<?> findSerializer(SerializationConfig config, JavaType type)
    {
        final Class<?> rawType = type.getRawClass();
        if ((CLASS_DOM_NODE != null) && CLASS_DOM_NODE.isAssignableFrom(rawType)) {
            return new DOMSerializer();
        }

        String className = rawType.getName();
        if (className.startsWith(PACKAGE_PREFIX_JAVAX_XML) || hasSuperClassStartingWith(rawType, PACKAGE_PREFIX_JAVAX_XML)) {
            if (Duration.class.isAssignableFrom(rawType) || QName.class.isAssignableFrom(rawType)) {
                return ToStringSerializer.instance;
            }
            if (XMLGregorianCalendar.class.isAssignableFrom(rawType)) {
                return XMLGregorianCalendarSerializer.instance;
            }
        }
        return null;
    }

    public JsonDeserializer<?> findDeserializer(DeserializationConfig config, JavaType type)
        throws JsonMappingException
    {
        final Class<?> rawType = type.getRawClass();
        if ((CLASS_DOM_NODE != null) && CLASS_DOM_NODE.isAssignableFrom(rawType)) {
            return new DOMDeserializer.NodeDeserializer();
        }
        if ((CLASS_DOM_DOCUMENT != null) && CLASS_DOM_DOCUMENT.isAssignableFrom(rawType)) {
            return new DOMDeserializer.DocumentDeserializer();
        }
        String className = rawType.getName();
        final String deserName = _sqlClasses.get(className);
        if (deserName != null) {
            return (JsonDeserializer<?>) instantiate(deserName, type);
        }
        if (className.startsWith(PACKAGE_PREFIX_JAVAX_XML)
                || hasSuperClassStartingWith(rawType, PACKAGE_PREFIX_JAVAX_XML)) {
            return CoreXMLDeserializers.findBeanDeserializer(config, type);
        }
        return null;
    }

    public boolean hasDeserializerFor(Class<?> valueType) {
        if ((CLASS_DOM_NODE != null) && CLASS_DOM_NODE.isAssignableFrom(valueType)) {
            return true;
        }
        if ((CLASS_DOM_DOCUMENT != null) && CLASS_DOM_DOCUMENT.isAssignableFrom(valueType)) {
            return true;
        }
        String className = valueType.getName();

        if (className.startsWith(PACKAGE_PREFIX_JAVAX_XML)
                || hasSuperClassStartingWith(valueType, PACKAGE_PREFIX_JAVAX_XML)) {
            return CoreXMLDeserializers.hasDeserializerFor(valueType);
        }
        // 06-Nov-2020, tatu: One of "java.sql" types?
        return _sqlClasses.containsKey(className);
    }
    
    /*
    /**********************************************************************
    /* Internal helper methods
    /**********************************************************************
     */

    private Object instantiate(String className, JavaType valueType)
    {
        try {
            return ClassUtil.createInstance(Class.forName(className), false);
        } catch (Throwable e) {
            throw new IllegalStateException("Failed to create instance of `"
+className+"` for handling values of type "+ClassUtil.getTypeDescription(valueType)
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
