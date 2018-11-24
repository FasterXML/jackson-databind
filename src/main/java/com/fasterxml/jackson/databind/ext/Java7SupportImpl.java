package com.fasterxml.jackson.databind.ext;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.AnnotatedParameter;
import com.fasterxml.jackson.databind.introspect.AnnotatedWithParams;

import java.beans.ConstructorProperties;
import java.beans.Transient;
import java.nio.file.Path;

/**
 * @since 2.8
 *
 * TODO Move this into a Java 7 Module
 */
public class Java7SupportImpl extends Java7Support
{
    @SuppressWarnings("unused") // compiler warns, just needed side-effects
    private final Class<?> _bogus;

    public Java7SupportImpl() {
        // Trigger loading of annotations that only JDK 7 has...
        Class<?> cls = Transient.class;
        cls = ConstructorProperties.class;
        _bogus = cls;
    }

    @Override
    public Class<?> getClassJavaNioFilePath() {
        return Path.class;
    }

    @Override
    public JsonDeserializer<?> getDeserializerForJavaNioFilePath(Class<?> rawType) {
        if (rawType == Path.class) {
            return new NioPathDeserializer();
        }
        return null;
    }

    @Override
    public JsonSerializer<?> getSerializerForJavaNioFilePath(Class<?> rawType) {
        if (Path.class.isAssignableFrom(rawType)) {
            return new NioPathSerializer();
        }
        return null;
    }

    @Override
    public Boolean findTransient(Annotated a) {
        Transient t = a.getAnnotation(Transient.class);
        if (t != null) {
            return t.value();
        }
        return null;
    }

    @Override
    public Boolean hasCreatorAnnotation(Annotated a) {
        ConstructorProperties props = a.getAnnotation(ConstructorProperties.class);
        // 08-Nov-2015, tatu: One possible check would be to ensure there is at least
        //    one name iff constructor has arguments. But seems unnecessary for now.
        if (props != null) {
            return Boolean.TRUE;
        }
        return null;
    }

    @Override
    public PropertyName findConstructorName(AnnotatedParameter p)
    {
        AnnotatedWithParams ctor = p.getOwner();
        if (ctor != null) {
            ConstructorProperties props = ctor.getAnnotation(ConstructorProperties.class);
            if (props != null) {
                String[] names = props.value();
                int ix = p.getIndex();
                if (ix < names.length) {
                    return PropertyName.construct(names[ix]);
                }
            }
        }
        return null;
    }
}
