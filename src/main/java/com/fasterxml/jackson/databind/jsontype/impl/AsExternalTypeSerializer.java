package com.fasterxml.jackson.databind.jsontype.impl;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;

/**
 * Type serializer that preferably embeds type information as an "external"
 * type property; embedded in enclosing JSON object.
 * Note that this serializer should only be used when value is being output
 * at JSON Object context; otherwise it cannot work reliably, and will have
 * to revert operation similar to {@link AsPropertyTypeSerializer}.
 *<p>
 * Note that implementation of serialization is bit cumbersome as we must
 * serialized external type id AFTER object; this because callback only
 * occurs after field name has been written.
 *<p>
 * Also note that this type of type id inclusion will NOT try to make use
 * of native Type Ids, even if those exist.
 */
public class AsExternalTypeSerializer extends TypeSerializerBase
{
    protected final String _typePropertyName;

    public AsExternalTypeSerializer(TypeIdResolver idRes, BeanProperty property, String propName) {
        super(idRes, property);
        _typePropertyName = propName;
    }

    @Override
    public AsExternalTypeSerializer forProperty(BeanProperty prop) {
        return (_property == prop) ? this : new AsExternalTypeSerializer(_idResolver, prop, _typePropertyName);
    }

    @Override
    public String getPropertyName() { return _typePropertyName; }

    @Override
    public As getTypeInclusion() { return As.EXTERNAL_PROPERTY; }

    /*
    /**********************************************************
    /* Writing prefixes
    /**********************************************************
     */
   
    @Override
    public void writeTypePrefixForObject(Object value, JsonGenerator g) throws IOException {
        _writeObjectPrefix(value, g);
    }

    @Override
    public void writeTypePrefixForObject(Object value, JsonGenerator g, Class<?> type) throws IOException {
        _writeObjectPrefix(value, g);
    }

    @Override
    public void writeTypePrefixForArray(Object value, JsonGenerator g) throws IOException {
        _writeArrayPrefix(value, g);
    }

    @Override
    public void writeTypePrefixForArray(Object value, JsonGenerator g, Class<?> type) throws IOException {
        _writeArrayPrefix(value, g);
    }

    @Override
    public void writeTypePrefixForScalar(Object value, JsonGenerator g) throws IOException {
        _writeScalarPrefix(value, g);
    }

    @Override
    public void writeTypePrefixForScalar(Object value, JsonGenerator g, Class<?> type) throws IOException {
        _writeScalarPrefix(value, g);
    }

    /*
    /**********************************************************
    /* Writing suffixes
    /**********************************************************
     */
   
   @Override
   public void writeTypeSuffixForObject(Object value, JsonGenerator g) throws IOException {
       _writeObjectSuffix(value, g, idFromValue(value));
   }

   @Override
   public void writeTypeSuffixForArray(Object value, JsonGenerator g) throws IOException {
       _writeArraySuffix(value, g, idFromValue(value));
   }
   
   @Override
   public void writeTypeSuffixForScalar(Object value, JsonGenerator g) throws IOException {
       _writeScalarSuffix(value, g, idFromValue(value));
   }

   /*
   /**********************************************************
   /* Writing with custom type id
   /**********************************************************
    */

   @Override
   public void writeCustomTypePrefixForScalar(Object value, JsonGenerator g, String typeId) throws IOException {
       _writeScalarPrefix(value, g);
   }
   
   @Override
   public void writeCustomTypePrefixForObject(Object value, JsonGenerator g, String typeId) throws IOException {
       _writeObjectPrefix(value, g);
   }
   
   @Override
   public void writeCustomTypePrefixForArray(Object value, JsonGenerator g, String typeId) throws IOException {
       _writeArrayPrefix(value, g);
   }

   @Override
   public void writeCustomTypeSuffixForScalar(Object value, JsonGenerator g, String typeId) throws IOException {
       _writeScalarSuffix(value, g, typeId);
   }

   @Override
   public void writeCustomTypeSuffixForObject(Object value, JsonGenerator g, String typeId) throws IOException {
       _writeObjectSuffix(value, g, typeId);
   }

   @Override
   public void writeCustomTypeSuffixForArray(Object value, JsonGenerator g, String typeId) throws IOException {
       _writeArraySuffix(value, g, typeId);
   }

   /*
   /**********************************************************
   /* Helper methods
   /**********************************************************
    */

   // nothing to wrap it with:
   protected final void _writeScalarPrefix(Object value, JsonGenerator g) throws IOException { }

   protected final void _writeObjectPrefix(Object value, JsonGenerator g) throws IOException {
       g.writeStartObject();
   }

   protected final void _writeArrayPrefix(Object value, JsonGenerator g) throws IOException {
       g.writeStartArray();
   }
   
   protected final void _writeScalarSuffix(Object value, JsonGenerator g, String typeId) throws IOException {
       if (typeId != null) {
           g.writeStringField(_typePropertyName, typeId);
       }
   }
   
   protected final void _writeObjectSuffix(Object value, JsonGenerator g, String typeId) throws IOException {
       g.writeEndObject();
       if (typeId != null) {
           g.writeStringField(_typePropertyName, typeId);
       }
   }

   protected final void _writeArraySuffix(Object value, JsonGenerator g, String typeId) throws IOException {
       g.writeEndArray();
       if (typeId != null) {
           g.writeStringField(_typePropertyName, typeId);
       }
   }
}
