package com.fasterxml.jackson.databind.introspect;

import java.lang.annotation.Annotation;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.DatabindVersion;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize.Typing;
/**
 * Dummy, "no-operation" implementation of {@link AnnotationIntrospector}.
 * Can be used as is to suppress handling of annotations; or as a basis
 * for simple complementary annotators
 */
public class NopAnnotationIntrospector
    extends AnnotationIntrospector
{
    /**
     * Static immutable and shareable instance that can be used as
     * "null" introspector: one that never finds any annotation
     * information.
     */
    public final static NopAnnotationIntrospector instance = new NopAnnotationIntrospector();

    @Override
    public Version version() {
        return DatabindVersion.instance.version();
    }

    /*
    /**********************************************************
    /* General annotation properties
    /**********************************************************
     */

    @Override
    public boolean isHandled(Annotation ann) {
        return false;
    }

    /*
    /**********************************************************
    /* General annotations
    /**********************************************************
     */

    @Override
    public String findEnumValue(Enum<?> value) {
        return null;
    }
    
    /*
    /**********************************************************
    /* General Class annotations
    /**********************************************************
     */

    @Override
    public String findRootName(AnnotatedClass ac) {
        return null;
    }

    @Override
    public String[] findPropertiesToIgnore(AnnotatedClass ac) {
        return null;
    }

    @Override
    public Boolean findIgnoreUnknownProperties(AnnotatedClass ac) {
        return null;
    }

    /*
    /**********************************************************
    /* General member (field, method/constructor) annotations
    /**********************************************************
     */

    @Override
    public boolean hasIgnoreMarker(AnnotatedMember member) {
        return false;
    }
    
    /*
    /**********************************************************
    /* General Method annotations
    /**********************************************************
     */

    @Override
    public boolean isIgnorableConstructor(AnnotatedConstructor c) {
        return false;
    }

    @Override
    public boolean isIgnorableMethod(AnnotatedMethod m) {
        return false;
    }
    
    /*
    /**********************************************************
    /* General field annotations
    /**********************************************************
     */

    @Override
    public boolean isIgnorableField(AnnotatedField f) {
        return false;
    }

    /*
    /**********************************************************
    /* Serialization: general annotations
    /**********************************************************
     */

    @Override
    public Object findSerializer(Annotated am) {
        return null;
    }

    @Override
    public Class<?> findSerializationType(Annotated a) {
        return null;
    }

    @Override
    public Typing findSerializationTyping(Annotated a) {
        return null;
    }

    @Override
    public Class<?>[] findSerializationViews(Annotated a) {
        return null;
    }

    /*
    /**********************************************************
    /* Serialization: class annotations
    /**********************************************************
     */

    @Override
    public String[] findSerializationPropertyOrder(AnnotatedClass ac) {
        return null;
    }

    @Override
    public Boolean findSerializationSortAlphabetically(AnnotatedClass ac) {
        return null;
    }

    /*
    /**********************************************************
    /* Serialization: method annotations
    /**********************************************************
     */

    @Override
    public String findGettablePropertyName(AnnotatedMethod am) {
        return null;
    }

    @Override
    public boolean hasAsValueAnnotation(AnnotatedMethod am) {
        return false;
    }
    
    @Override
    public String findDeserializablePropertyName(AnnotatedField af) {
        return null;
    }

    @Override
    public Class<?> findDeserializationContentType(Annotated am, JavaType t, String propName) {
        return null;
    }

    @Override
    public Class<?> findDeserializationKeyType(Annotated am, JavaType t, String propName) {
        return null;
    }

    @Override
    public Class<?> findDeserializationType(Annotated am, JavaType t, String propName) {
        return null;
    }

    @Override
    public Object findDeserializer(Annotated am) { return null; }

    @Override
    public Class<KeyDeserializer> findKeyDeserializer(Annotated am) { return null; }

    @Override
    public Class<JsonDeserializer<?>> findContentDeserializer(Annotated am) { return null; }


    @Override
    public String findPropertyNameForParam(AnnotatedParameter param) {
        return null;
    }

    @Override
    public String findSerializablePropertyName(AnnotatedField af) {
        return null;
    }
    
    @Override
    public String findSettablePropertyName(AnnotatedMethod am) {
        return null;
    }
}
