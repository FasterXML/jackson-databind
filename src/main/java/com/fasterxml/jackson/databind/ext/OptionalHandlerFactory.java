package com.fasterxml.jackson.databind.ext;

import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

/**
 * Helper class used for isolating details of handling optional+external types
 * (javax.xml classes) from standard factories that offer them.
 *<p>
 * Note that with 3.0 need for separate class has been reduced somewhat
 * and this class may be eliminated.
 */
public class OptionalHandlerFactory implements java.io.Serializable
{
    private static final long serialVersionUID = 1;

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
    
    protected OptionalHandlerFactory() { }

    /*
    /**********************************************************
    /* Public API
    /**********************************************************
     */
    
    public JsonSerializer<?> findSerializer(SerializationConfig config, JavaType type,
            BeanDescription beanDesc)
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

    public JsonDeserializer<?> findDeserializer(JavaType type, DeserializationConfig config,
            BeanDescription beanDesc)
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

        if (className.startsWith(PACKAGE_PREFIX_JAVAX_XML)
                || hasSuperClassStartingWith(rawType, PACKAGE_PREFIX_JAVAX_XML)) {
            return CoreXMLDeserializers.findBeanDeserializer(type, config, beanDesc);
        }
        return null;
    }
    
    /*
    /**********************************************************
    /* Internal helper methods
    /**********************************************************
     */

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
