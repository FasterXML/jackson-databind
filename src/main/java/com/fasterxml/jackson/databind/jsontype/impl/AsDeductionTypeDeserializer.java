package com.fasterxml.jackson.databind.jsontype.impl;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.util.ClassUtil;
import com.fasterxml.jackson.databind.util.TokenBuffer;

/**
 * A {@link TypeDeserializer} capable of deducing polymorphic types based on the
 * fields available. Deduction is limited to the <i>names</i> of child properties
 * (not their values or, consequently, any nested descendants).
 * Exceptions will be thrown if not enough unique information is present
 * to select a single subtype.
 * <p>
 * The current deduction process <b>does not</b> support pojo-hierarchies such that
 * the absence of child fields infers a parent type. That is, every deducible subtype
 * MUST have some unique fields and the input data MUST contain said unique fields
 * to provide a <i>positive match</i>.
 */
public class AsDeductionTypeDeserializer extends AsPropertyTypeDeserializer
{
    private static final long serialVersionUID = 1L;

    // Fieldname -> bitmap-index of every field discovered, across all subtypes
    private final Map<String, Integer> fieldBitIndex;
    // Bitmap of available fields in each subtype (including its parents)
    private final Map<BitSet, String> subtypeFingerprints;

    private static final String EMPTY_CLASS_MARKER = "";

    public AsDeductionTypeDeserializer(JavaType bt, TypeIdResolver idRes, JavaType defaultImpl,
            DeserializationConfig config, Collection<NamedType> subtypes)
    {
        super(bt, idRes, null, false, defaultImpl, null);
        fieldBitIndex = new HashMap<>();
        subtypeFingerprints = buildFingerprints(config, subtypes);
    }

    public AsDeductionTypeDeserializer(AsDeductionTypeDeserializer src, BeanProperty property) {
        super(src, property);
        fieldBitIndex = src.fieldBitIndex;
        subtypeFingerprints = src.subtypeFingerprints;
    }

    @Override
    public TypeDeserializer forProperty(BeanProperty prop) {
        return (prop == _property) ? this : new AsDeductionTypeDeserializer(this, prop);
    }

    protected Map<BitSet, String> buildFingerprints(DeserializationConfig config, Collection<NamedType> subtypes) {
        boolean ignoreCase = config.isEnabled(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES);

        int nextField = 0;
        Map<BitSet, String> fingerprints = new HashMap<>();

        for (NamedType subtype : subtypes) {
            JavaType subtyped = config.getTypeFactory().constructType(subtype.getType());
            List<BeanPropertyDefinition> properties = config.introspect(subtyped).findProperties();

            BitSet fingerprint = new BitSet(nextField + properties.size());
            if (properties.size() > 0) {
                for (BeanPropertyDefinition property : properties) {
                    String name = property.getName();
                    if (ignoreCase) name = name.toLowerCase();
                    nextField = setField(fingerprint, name, nextField);
                }
            } else {
                nextField = setField(fingerprint, EMPTY_CLASS_MARKER, nextField);
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

    private int setField(BitSet fingerprint, String name, int nextField) {
        Integer bitIndex = fieldBitIndex.get(name);
        if (bitIndex == null) {
            bitIndex = nextField;
            fieldBitIndex.put(name, nextField++);
        }
        fingerprint.set(bitIndex);
        return nextField;
    }

    @Override
    public Object deserializeTypedFromObject(JsonParser p, DeserializationContext ctxt) throws IOException {

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
            return _deserializeTypedUsingDefaultImpl(p, ctxt, null, "Unexpected input");
        }

        List<BitSet> candidates = new LinkedList<>(subtypeFingerprints.keySet());

        // Record processed tokens as we must rewind once after deducing the deserializer to use
        @SuppressWarnings("resource")
        TokenBuffer tb = new TokenBuffer(p, ctxt);
        Object result = null;

        // Next character of empty class is '}'
        if (t == JsonToken.END_OBJECT) {
            result = deserializeCandidates(EMPTY_CLASS_MARKER, candidates, p, ctxt, tb);
        } else {
            boolean ignoreCase = ctxt.isEnabled(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES);
            for (; t == JsonToken.FIELD_NAME; t = p.nextToken()) {
                String name = p.currentName();
                if (ignoreCase) name = name.toLowerCase();
                if ((result = deserializeCandidates(name, candidates, p, ctxt, tb)) != null) {
                    break;
                }
            }
        }
        if (result != null) {
            return result;
        }

        // We have zero or multiple candidates, deduction has failed
        String msgToReportIfDefaultImplFailsToo = String.format("Cannot deduce unique subtype of %s (%d candidates match)", ClassUtil.getTypeDescription(_baseType), candidates.size());
        return _deserializeTypedUsingDefaultImpl(p, ctxt, tb, msgToReportIfDefaultImplFailsToo);
    }

    private Object deserializeCandidates(String name, List<BitSet> candidates, JsonParser p, DeserializationContext ctxt, TokenBuffer tb) throws IOException {
        Integer bit = fieldBitIndex.get(name);
        // No empty class is registered
        if (bit == null && name.equals(EMPTY_CLASS_MARKER)) {
            return null;
        }
        if (bit != null) {
            // field is known by at least one subtype
            candidates.removeIf(bitSet -> !bitSet.get(bit));
        }
        tb.copyCurrentStructure(p);
        if (candidates.size() != 1) {
            return null;
        }
        return _deserializeTypedForId(p, ctxt, tb, subtypeFingerprints.get(candidates.get(0)));
    }
}
