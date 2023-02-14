package com.fasterxml.jackson.databind.util;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.type.ClassKey;

/**
 * Helper class for caching resolved root names.
 */
public class RootNameLookup implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    /**
     * For efficient operation, let's try to minimize number of times we
     * need to introspect root element name to use.
     */
    protected transient LRUMap<ClassKey,PropertyName> _rootNames;

    public RootNameLookup() {
        _rootNames = new LRUMap<ClassKey,PropertyName>(20, 200);
   }

    public PropertyName findRootName(JavaType rootType, MapperConfig<?> config) {
        return findRootName(rootType.getRawClass(), config);
    }

    public PropertyName findRootName(Class<?> rootType, MapperConfig<?> config)
    {
        ClassKey key = new ClassKey(rootType);
        PropertyName name = _rootNames.get(key);
        if (name != null) {
            return name;
        }
        BeanDescription beanDesc = config.introspectClassAnnotations(rootType);
        AnnotationIntrospector intr = config.getAnnotationIntrospector();
        AnnotatedClass ac = beanDesc.getClassInfo();
        name = intr.findRootName(ac);
        // No answer so far? Let's just default to using simple class name
        if (name == null || !name.hasSimpleName()) {
            // Should we strip out enclosing class tho? For now, nope:
            name = PropertyName.construct(rootType.getSimpleName());
        }
        _rootNames.put(key, name);
        return name;
    }

    /*
    /**********************************************************
    /* Serializable overrides
    /**********************************************************
     */

    /**
     * Need to override to reproduce cache object via constructor, instead
     * of serialize/deserialize (since we do NOT want to retain cached data)
     */
    protected Object readResolve() {
        return new RootNameLookup();
    }
}
