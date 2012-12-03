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
 * at JSON Object context; otherwise it can not work reliably, and will have
 * to revert operation similar to {@link AsPropertyTypeSerializer}.
 *<p>
 * Note that implementation of serialization is bit cumbersome as we must
 * serialized external type id AFTER object; this because callback only
 * occurs after field name has been written.
 */
public class AsExternalTypeSerializer
   extends TypeSerializerBase
{
   protected final String _typePropertyName;

   public AsExternalTypeSerializer(TypeIdResolver idRes, BeanProperty property,
          String propName)
   {
       super(idRes, property);
       _typePropertyName = propName;
   }

   @Override
   public AsExternalTypeSerializer forProperty(BeanProperty prop) {
       if (_property == prop) return this;
       return new AsExternalTypeSerializer(_idResolver, prop, _typePropertyName);
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
   public void writeTypePrefixForObject(Object value, JsonGenerator jgen)
       throws IOException, JsonProcessingException
   {
       _writeObjectPrefix(value, jgen);
   }

   @Override
   public void writeTypePrefixForObject(Object value, JsonGenerator jgen, Class<?> type)
       throws IOException, JsonProcessingException
   {
       _writeObjectPrefix(value, jgen);
   }
   
   @Override
   public void writeTypePrefixForArray(Object value, JsonGenerator jgen)
       throws IOException, JsonProcessingException
   {
       _writeArrayPrefix(value, jgen);
   }

   @Override
   public void writeTypePrefixForArray(Object value, JsonGenerator jgen, Class<?> type)
           throws IOException, JsonProcessingException
   {
       _writeArrayPrefix(value, jgen);
   }

   @Override
   public void writeTypePrefixForScalar(Object value, JsonGenerator jgen)
           throws IOException, JsonProcessingException
   {
       _writeScalarPrefix(value, jgen);
   }

   @Override
   public void writeTypePrefixForScalar(Object value, JsonGenerator jgen, Class<?> type)
           throws IOException, JsonProcessingException
   {
       _writeScalarPrefix(value, jgen);
   }

   /*
   /**********************************************************
   /* Writing suffixes
   /**********************************************************
    */
   
   @Override
   public void writeTypeSuffixForObject(Object value, JsonGenerator jgen)
       throws IOException, JsonProcessingException
   {
       _writeObjectSuffix(value, jgen, idFromValue(value));
   }

   @Override
   public void writeTypeSuffixForArray(Object value, JsonGenerator jgen)
       throws IOException, JsonProcessingException
   {
       _writeArraySuffix(value, jgen, idFromValue(value));
   }
   
   @Override
   public void writeTypeSuffixForScalar(Object value, JsonGenerator jgen)
       throws IOException, JsonProcessingException
   {
       _writeScalarSuffix(value, jgen, idFromValue(value));
   }

   /*
   /**********************************************************
   /* Writing with custom type id
   /**********************************************************
    */

   @Override
   public void writeCustomTypePrefixForScalar(Object value, JsonGenerator jgen, String typeId)
       throws IOException, JsonProcessingException
   {
       _writeScalarPrefix(value, jgen);
   }
   
   @Override
   public void writeCustomTypePrefixForObject(Object value, JsonGenerator jgen, String typeId)
       throws IOException, JsonProcessingException {
       _writeObjectPrefix(value, jgen);
   }
   
   @Override
   public void writeCustomTypePrefixForArray(Object value, JsonGenerator jgen, String typeId)
       throws IOException, JsonProcessingException
   {
       _writeArrayPrefix(value, jgen);
   }

   @Override
   public void writeCustomTypeSuffixForScalar(Object value, JsonGenerator jgen, String typeId)
       throws IOException, JsonProcessingException {
       _writeScalarSuffix(value, jgen, typeId);
   }

   @Override
   public void writeCustomTypeSuffixForObject(Object value, JsonGenerator jgen, String typeId)
       throws IOException, JsonProcessingException {
       _writeObjectSuffix(value, jgen, typeId);
   }

   @Override
   public void writeCustomTypeSuffixForArray(Object value, JsonGenerator jgen, String typeId)
           throws IOException, JsonProcessingException {
       _writeArraySuffix(value, jgen, typeId);
   }
   
   /*
   /**********************************************************
   /* Helper methods
   /**********************************************************
    */

   protected final void _writeScalarPrefix(Object value, JsonGenerator jgen)
       throws IOException, JsonProcessingException {
       // nothing to wrap it with
   }

   protected final void _writeObjectPrefix(Object value, JsonGenerator jgen)
       throws IOException, JsonProcessingException {
       jgen.writeStartObject();
   }

   protected final void _writeArrayPrefix(Object value, JsonGenerator jgen)
       throws IOException, JsonProcessingException {
       jgen.writeStartArray();
   }
   
   protected final void _writeScalarSuffix(Object value, JsonGenerator jgen, String typeId)
       throws IOException, JsonProcessingException
   {
       jgen.writeStringField(_typePropertyName, typeId);
   }
   
   protected final void _writeObjectSuffix(Object value, JsonGenerator jgen, String typeId)
       throws IOException, JsonProcessingException
   {
       jgen.writeEndObject();
       jgen.writeStringField(_typePropertyName, typeId);
   }

   protected final void _writeArraySuffix(Object value, JsonGenerator jgen, String typeId)
       throws IOException, JsonProcessingException
   {
       jgen.writeEndArray();
       jgen.writeStringField(_typePropertyName, typeId);
   }
}
