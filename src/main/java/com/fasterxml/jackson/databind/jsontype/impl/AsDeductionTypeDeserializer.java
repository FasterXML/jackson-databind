package com.fasterxml.jackson.databind.jsontype.impl;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.util.ClassUtil;
import com.fasterxml.jackson.databind.util.TokenBuffer;

/**
 * A {@link TypeDeserializer} capable of deducing polymorphic types based
 * on the properties available Deduction is limited to the <i>names</i> of
 * direct child properties (not their values or, consequently, any nested descendants).
 * Exceptions will be thrown if not enough unique information is present to select a
 * single subtype.
 */
public class AsDeductionTypeDeserializer extends AsPropertyTypeDeserializer
{
    // Property name -> bitmap-index of every Property discovered, across all subtypes
    private final Map<String, Integer> propertyBitIndex;
    // Bitmap of available properties in each subtype (including its parents)
    private final Map<BitSet, String> subtypeFingerprints;

    public AsDeductionTypeDeserializer(DeserializationContext ctxt,
            JavaType bt, TypeIdResolver idRes, JavaType defaultImpl,
            Collection<NamedType> subtypes) {
        super(bt, idRes, null, false, defaultImpl);
        propertyBitIndex = new HashMap<>();
        subtypeFingerprints = buildFingerprints(ctxt, subtypes);
    }

    public AsDeductionTypeDeserializer(AsDeductionTypeDeserializer src, BeanProperty property) {
        super(src, property);
        propertyBitIndex = src.propertyBitIndex;
        subtypeFingerprints = src.subtypeFingerprints;
    }

    @Override
    public JsonTypeInfo.As getTypeInclusion() {
        return null;
    }

    @Override
    public TypeDeserializer forProperty(BeanProperty prop) {
        return (prop == _property) ? this : new AsDeductionTypeDeserializer(this, prop);
    }

    protected Map<BitSet, String> buildFingerprints(DeserializationContext ctxt,
            Collection<NamedType> subtypes)
    {
        boolean ignoreCase = ctxt.isEnabled(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES);

        int nextProperty = 0;
        Map<BitSet, String> fingerprints = new HashMap<>();

        for (NamedType subtype : subtypes) {
            JavaType subtyped = ctxt.constructType(subtype.getType());
            List<BeanPropertyDefinition> properties = ctxt.introspectBeanDescription(subtyped).findProperties();

            BitSet fingerprint = new BitSet(nextProperty + properties.size());
            for (BeanPropertyDefinition property : properties) {
                String name = property.getName();
                if (ignoreCase) name = name.toLowerCase();
                Integer bitIndex = propertyBitIndex.get(name);
                if (bitIndex == null) {
                    bitIndex = nextProperty;
                    propertyBitIndex.put(name, nextProperty++);
                }
                fingerprint.set(bitIndex);
            }

            String existingFingerprint = fingerprints.put(fingerprint, subtype.getType().getName());

            // Validate uniqueness
            if (existingFingerprint != null) {
                throw new IllegalStateException(
                        String.format("Subtypes %s and %s have the same signature and cannot be uniquely deduced.", existingFingerprint, subtype.getType().getName())
                        );
            }
        }
        return fingerprints;
    }

    @Override
    public Object deserializeTypedFromObject(JsonParser p, DeserializationContext ctxt)
        throws JacksonException
    {
        JsonToken t = p.currentToken();
        if (t == JsonToken.START_OBJECT) {
            t = p.nextToken();
        } else {
            /* This is most likely due to the fact that not all Java types are
             * serialized as JSON Objects; so if "as-property" inclusion is requested,
             * serialization of things like Lists must be instead handled as if
             * "as-wrapper-array" was requested.
             * But this can also be due to some custom handling: so, if "defaultImpl"
             * is defined, it will be asked to handle this case.
             */
            return _deserializeTypedUsingDefaultImpl(p, ctxt, null);
        }

        List<BitSet> candidates = new LinkedList<>(subtypeFingerprints.keySet());

        // Record processed tokens as we must rewind once after deducing the deserializer to use
        TokenBuffer tb = TokenBuffer.forInputBuffering(p, ctxt);
        boolean ignoreCase = ctxt.isEnabled(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES);

        for (; t == JsonToken.PROPERTY_NAME; t = p.nextToken()) {
            String name = p.currentName();
            if (ignoreCase) name = name.toLowerCase();

            tb.copyCurrentStructure(p);

            Integer bit = propertyBitIndex.get(name);
            if (bit != null) {
                // Property is known by at least one subtype
                prune(candidates, bit);
                if (candidates.size() == 1) {
                    return _deserializeTypedForId(p, ctxt, tb,
                            subtypeFingerprints.get(candidates.get(0)));
                }
            }
        }

        throw new InvalidTypeIdException(p,
                String.format("Cannot deduce unique subtype of %s (%d candidates match)",
                        ClassUtil.getTypeDescription(_baseType),
                        candidates.size()),
                _baseType
                , "DEDUCED"
        );
    }

    // Keep only fingerprints containing this property
    private static void prune(List<BitSet> candidates, int bit) {
        for (Iterator<BitSet> iter = candidates.iterator(); iter.hasNext(); ) {
            if (!iter.next().get(bit)) {
                iter.remove();
            }
        }
    }
}
