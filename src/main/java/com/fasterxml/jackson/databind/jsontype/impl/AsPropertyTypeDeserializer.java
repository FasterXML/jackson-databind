package com.fasterxml.jackson.databind.jsontype.impl;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonTypeInfo.As;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.util.JsonParserSequence;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.util.TokenBuffer;

/**
 * Type deserializer used with {@link As#PROPERTY}
 * inclusion mechanism.
 * Uses regular form (additional key/value entry before actual data)
 * when typed object is expressed as JSON Object; otherwise behaves similar to how
 * {@link As#WRAPPER_ARRAY} works.
 * Latter is used if JSON representation is polymorphic
 */
@SuppressWarnings("resource")
public class AsPropertyTypeDeserializer extends AsArrayTypeDeserializer
{
    private static final long serialVersionUID = 1L;

    protected final As _inclusion;

    // @since 2.12.2 (see [databind#3055]
    protected final String _msgForMissingId = (_property == null)
            ? String.format("missing type id property '%s'", _typePropertyName)
            : String.format("missing type id property '%s' (for POJO property '%s')", _typePropertyName, _property.getName());

    /**
     * @since 2.8
     */
    public AsPropertyTypeDeserializer(JavaType bt, TypeIdResolver idRes,
            String typePropertyName, boolean typeIdVisible, JavaType defaultImpl)
    {
        this(bt, idRes, typePropertyName, typeIdVisible, defaultImpl, As.PROPERTY);
    }

    /**
     * @since 2.8
     */
    public AsPropertyTypeDeserializer(JavaType bt, TypeIdResolver idRes,
            String typePropertyName, boolean typeIdVisible, JavaType defaultImpl,
            As inclusion)
    {
        super(bt, idRes, typePropertyName, typeIdVisible, defaultImpl);
        _inclusion = inclusion;
    }

    public AsPropertyTypeDeserializer(AsPropertyTypeDeserializer src, BeanProperty property) {
        super(src, property);
        _inclusion = src._inclusion;
    }

    @Override
    public TypeDeserializer forProperty(BeanProperty prop) {
        return (prop == _property) ? this : new AsPropertyTypeDeserializer(this, prop);
    }

    @Override
    public As getTypeInclusion() { return _inclusion; }

    /**
     * This is the trickiest thing to handle, since property we are looking
     * for may be anywhere...
     */
    @Override
    public Object deserializeTypedFromObject(JsonParser p, DeserializationContext ctxt) throws IOException
    {
        // 02-Aug-2013, tatu: May need to use native type ids
        if (p.canReadTypeId()) {
            Object typeId = p.getTypeId();
            if (typeId != null) {
                return _deserializeWithNativeTypeId(p, ctxt, typeId);
            }
        }

        // but first, sanity check to ensure we have START_OBJECT or FIELD_NAME
        JsonToken t = p.currentToken();
        if (t == JsonToken.START_OBJECT) {
            t = p.nextToken();
        } else if (/*t == JsonToken.START_ARRAY ||*/ t != JsonToken.FIELD_NAME) {
            /* This is most likely due to the fact that not all Java types are
             * serialized as JSON Objects; so if "as-property" inclusion is requested,
             * serialization of things like Lists must be instead handled as if
             * "as-wrapper-array" was requested.
             * But this can also be due to some custom handling: so, if "defaultImpl"
             * is defined, it will be asked to handle this case.
             */
            return _deserializeTypedUsingDefaultImpl(p, ctxt, null, _msgForMissingId);
        }
        // Ok, let's try to find the property. But first, need token buffer...
        TokenBuffer tb = null;
        boolean ignoreCase = ctxt.isEnabled(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES);

        for (; t == JsonToken.FIELD_NAME; t = p.nextToken()) {
            final String name = p.currentName();
            p.nextToken(); // to point to the value
            if (name.equals(_typePropertyName)
                    || (ignoreCase && name.equalsIgnoreCase(_typePropertyName))) { // gotcha!
                // 09-Sep-2021, tatu: [databind#3271]: Avoid converting null to "null"
                String typeId = p.getValueAsString();
                if (typeId != null) {
                    return _deserializeTypedForId(p, ctxt, tb, typeId);
                }
            }
            if (tb == null) {
                tb = ctxt.bufferForInputBuffering(p);
            }
            tb.writeFieldName(name);
            tb.copyCurrentStructure(p);
        }
        return _deserializeTypedUsingDefaultImpl(p, ctxt, tb, _msgForMissingId);
    }

    protected Object _deserializeTypedForId(JsonParser p, DeserializationContext ctxt,
            TokenBuffer tb, String typeId) throws IOException
    {
        JsonDeserializer<Object> deser = _findDeserializer(ctxt, typeId);
        if (_typeIdVisible) { // need to merge id back in JSON input?
            if (tb == null) {
                tb = ctxt.bufferForInputBuffering(p);
            }
            tb.writeFieldName(p.currentName());
            tb.writeString(typeId);
        }
        if (tb != null) { // need to put back skipped properties?
            // 02-Jul-2016, tatu: Depending on for JsonParserSequence is initialized it may
            //   try to access current token; ensure there isn't one
            p.clearCurrentToken();
            p = JsonParserSequence.createFlattened(false, tb.asParser(p), p);
        }
        if (p.currentToken() != JsonToken.END_OBJECT) {
            // Must point to the next value; tb had no current, p pointed to VALUE_STRING:
            p.nextToken(); // to skip past String value
        }
        // deserializer should take care of closing END_OBJECT as well
        return deser.deserialize(p, ctxt);
    }

    @Deprecated // since 2.12.2 (remove from 2.14 or later)
    protected Object _deserializeTypedUsingDefaultImpl(JsonParser p,
            DeserializationContext ctxt, TokenBuffer tb) throws IOException {
        return _deserializeTypedUsingDefaultImpl(p, ctxt, tb, null);
    }

    // off-lined to keep main method lean and mean...
    // @since 2.12.2 (last arg added)
    protected Object _deserializeTypedUsingDefaultImpl(JsonParser p,
            DeserializationContext ctxt, TokenBuffer tb, String priorFailureMsg)
        throws IOException
    {
        // May have default implementation to use
        // 13-Oct-2020, tatu: As per [databind#2775], need to be careful to
        //    avoid ending up using "nullifying" deserializer
        if (!hasDefaultImpl()) {
            // or, perhaps we just bumped into a "natural" value (boolean/int/double/String)?
            Object result = TypeDeserializer.deserializeIfNatural(p, ctxt, _baseType);
            if (result != null) {
                return result;
            }
            // or, something for which "as-property" won't work, changed into "wrapper-array" type:
            if (p.isExpectedStartArrayToken()) {
                return super.deserializeTypedFromAny(p, ctxt);
            }
            if (p.hasToken(JsonToken.VALUE_STRING)) {
                if (ctxt.isEnabled(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)) {
                    String str = p.getText().trim();
                    if (str.isEmpty()) {
                        return null;
                    }
                }
            }
        }
        // ... and here we will check for default implementation handling (either
        // genuine, or faked for "dont fail on bad type id")
        JsonDeserializer<Object> deser = _findDefaultImplDeserializer(ctxt);
        if (deser == null) {
            JavaType t = _handleMissingTypeId(ctxt, priorFailureMsg);
            if (t == null) {
                // 09-Mar-2017, tatu: Is this the right thing to do?
                return null;
            }
            // ... would this actually work?
            deser = ctxt.findContextualValueDeserializer(t, _property);
        }
        if (tb != null) {
            tb.writeEndObject();
            p = tb.asParser(p);
            // must move to point to the first token:
            p.nextToken();
        }
        return deser.deserialize(p, ctxt);
    }

    /* Also need to re-route "unknown" version. Need to think
     * this through bit more in future, but for now this does address issue and has
     * no negative side effects (at least within existing unit test suite).
     */
    @Override
    public Object deserializeTypedFromAny(JsonParser p, DeserializationContext ctxt) throws IOException {
        // Sometimes, however, we get an array wrapper; specifically
        // when an array or list has been serialized with type information.
        if (p.hasToken(JsonToken.START_ARRAY)) {
            return super.deserializeTypedFromArray(p, ctxt);
        }
        return deserializeTypedFromObject(p, ctxt);
    }

    // These are fine from base class:
    //public Object deserializeTypedFromArray(JsonParser jp, DeserializationContext ctxt)
    //public Object deserializeTypedFromScalar(JsonParser jp, DeserializationContext ctxt)
}
