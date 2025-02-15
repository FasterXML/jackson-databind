package tools.jackson.databind.ext;

import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import tools.jackson.databind.*;
import tools.jackson.databind.ext.sql.JavaSqlTypeHandlerFactory;
import tools.jackson.databind.ser.std.ToStringSerializer;

/**
 * Helper class used for isolating details of handling optional+external types
 * (javax.xml classes) from standard factories that offer them.
 */
public class OptionalHandlerFactory
{
    // To make 2 main "optional" handler groups (javax.xml.stream)
    // more dynamic, we better only figure out handlers completely dynamically, if and
    // when they are needed. To do this we need to assume package prefixes.
    private final static String PACKAGE_PREFIX_JAVAX_XML = "javax.xml.";

    // // Since 2.7, we will assume DOM classes are always found, both due to JDK 1.6 minimum
    // // and because Android (and presumably GAE) have these classes

    private final static Class<?> CLASS_DOM_NODE = org.w3c.dom.Node.class;
    private final static Class<?> CLASS_DOM_DOCUMENT = org.w3c.dom.Document.class;

    public final static OptionalHandlerFactory instance = new OptionalHandlerFactory();

    protected OptionalHandlerFactory() {
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
        if (className.startsWith(PACKAGE_PREFIX_JAVAX_XML)
                || hasSuperClassStartingWith(rawType, PACKAGE_PREFIX_JAVAX_XML)) {
            if (Duration.class.isAssignableFrom(rawType)) {
                return ToStringSerializer.instance;
            }
            if (QName.class.isAssignableFrom(rawType)) {
                return QNameSerializer.instance;
            }
            if (XMLGregorianCalendar.class.isAssignableFrom(rawType)) {
                return XMLGregorianCalendarSerializer.instance;
            }
        }
        return JavaSqlTypeHandlerFactory.instance.findSerializer(config, type);
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
        if (className.startsWith(PACKAGE_PREFIX_JAVAX_XML)
                || hasSuperClassStartingWith(rawType, PACKAGE_PREFIX_JAVAX_XML)) {
            return CoreXMLDeserializers.findBeanDeserializer(config, type);
        }
        return JavaSqlTypeHandlerFactory.instance.findDeserializer(config, type);
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
        return JavaSqlTypeHandlerFactory.instance.hasDeserializerFor(valueType);
    }

    private boolean _IsXOfY(Class<?> valueType, Class<?> expType) {
        return (expType != null) && expType.isAssignableFrom(valueType);
    }

    /*
    /**********************************************************************
    /* Internal helper methods
    /**********************************************************************
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
