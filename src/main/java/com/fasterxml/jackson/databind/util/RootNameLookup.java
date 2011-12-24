package com.fasterxml.jackson.databind.util;

import com.fasterxml.jackson.core.io.SerializedString;

import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.MapperConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.BasicBeanDescription;
import com.fasterxml.jackson.databind.type.ClassKey;

/**
 * Helper class for caching resolved root names.
 */
public class RootNameLookup
{
    /**
     * For efficient operation, let's try to minimize number of times we
     * need to introspect root element name to use.
     */
    protected LRUMap<ClassKey,SerializedString> _rootNames;

    public RootNameLookup() { }

    public SerializedString findRootName(JavaType rootType, MapperConfig<?> config)
    {
        return findRootName(rootType.getRawClass(), config);
    }

    public synchronized SerializedString findRootName(Class<?> rootType, MapperConfig<?> config)
    {
        ClassKey key = new ClassKey(rootType);

        if (_rootNames == null) {
            _rootNames = new LRUMap<ClassKey,SerializedString>(20, 200);
        } else {
            SerializedString name = _rootNames.get(key);
            if (name != null) {
                return name;
            }
        }
        BasicBeanDescription beanDesc = (BasicBeanDescription) config.introspectClassAnnotations(rootType);
        AnnotationIntrospector intr = config.getAnnotationIntrospector();
        AnnotatedClass ac = beanDesc.getClassInfo();
        String nameStr = intr.findRootName(ac);
        // No answer so far? Let's just default to using simple class name
        if (nameStr == null) {
            // Should we strip out enclosing class tho? For now, nope:
            nameStr = rootType.getSimpleName();
        }
        SerializedString name = new SerializedString(nameStr);
        _rootNames.put(key, name);
        return name;
    }
}
