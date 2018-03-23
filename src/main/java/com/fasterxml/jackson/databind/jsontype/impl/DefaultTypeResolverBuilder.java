package com.fasterxml.jackson.databind.jsontype.impl;

import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import com.fasterxml.jackson.core.TreeNode;

import com.fasterxml.jackson.databind.DefaultTyping;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;

/**
 * Customized {@link TypeResolverBuilder} that provides type resolver builders
 * used with so-called "default typing"
 * (see <code>MapperBuilder.enableDefaultTyping()</code> for details).
 *<p>
 * Type resolver construction is based on configuration: implementation takes care
 * of only providing builders in cases where type information should be applied.
 * This is important since build calls may be sent for any and all types, and
 * type information should NOT be applied to all of them.
 */
public class DefaultTypeResolverBuilder
    extends StdTypeResolverBuilder
    implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    /**
     * Definition of what types is this default typer valid for.
     */
    protected final DefaultTyping _appliesFor;

    public DefaultTypeResolverBuilder(DefaultTyping t, JsonTypeInfo.As includeAs) {
        _appliesFor = t;
        _idType = JsonTypeInfo.Id.CLASS;
        _includeAs = includeAs;
        _typeProperty = _idType.getDefaultPropertyName();
    }

    public DefaultTypeResolverBuilder(DefaultTyping t, String propertyName) {
        _appliesFor = t;
        _idType = JsonTypeInfo.Id.CLASS;
        _includeAs = JsonTypeInfo.As.PROPERTY;
        _typeProperty = propertyName;
    }

    public DefaultTypeResolverBuilder(DefaultTyping t, JsonTypeInfo.As includeAs,
            JsonTypeInfo.Id idType, String propertyName) {
        _appliesFor = t;
        _idType = idType;
        _includeAs = includeAs;
        if (propertyName == null) {
            propertyName = _idType.getDefaultPropertyName();
        }
        _typeProperty = propertyName;
    }

    @Override
    public TypeDeserializer buildTypeDeserializer(DeserializationConfig config,
            JavaType baseType, Collection<NamedType> subtypes)
    {
        return useForType(baseType) ? super.buildTypeDeserializer(config, baseType, subtypes) : null;
    }

    @Override
    public TypeSerializer buildTypeSerializer(SerializationConfig config,
            JavaType baseType, Collection<NamedType> subtypes)
    {
        return useForType(baseType) ? super.buildTypeSerializer(config, baseType, subtypes) : null;            
    }

    public DefaultTypeResolverBuilder typeIdVisibility(boolean isVisible) {
        _typeIdVisible = isVisible;
        return this;
    }

    /**
     * Method called to check if the default type handler should be
     * used for given type.
     * Note: "natural types" (String, Boolean, Integer, Double) will never
     * use typing; that is both due to them being concrete and final,
     * and since actual serializers and deserializers will also ignore any
     * attempts to enforce typing.
     */
    public boolean useForType(JavaType t)
    {
        // 03-Oct-2016, tatu: As per [databind#1395], need to skip
        //  primitive types too, regardless
        if (t.isPrimitive()) {
            return false;
        }

        switch (_appliesFor) {
        case NON_CONCRETE_AND_ARRAYS:
            while (t.isArrayType()) {
                t = t.getContentType();
            }
            // fall through
        case OBJECT_AND_NON_CONCRETE:
            // 19-Apr-2016, tatu: ReferenceType like Optional also requires similar handling:
            while (t.isReferenceType()) {
                t = t.getReferencedType();
            }
            return t.isJavaLangObject()
                    || (!t.isConcrete()
                            // [databind#88] Should not apply to JSON tree models:
                            && !TreeNode.class.isAssignableFrom(t.getRawClass()));

        case NON_FINAL:
            while (t.isArrayType()) {
                t = t.getContentType();
            }
            // 19-Apr-2016, tatu: ReferenceType like Optional also requires similar handling:
            while (t.isReferenceType()) {
                t = t.getReferencedType();
            }
            // [databind#88] Should not apply to JSON tree models:
            return !t.isFinal() && !TreeNode.class.isAssignableFrom(t.getRawClass());
        default:
        //case JAVA_LANG_OBJECT:
            return t.isJavaLangObject();
        }
    }
}