package tools.jackson.databind.jsontype.impl;

import java.util.*;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.*;
import tools.jackson.databind.introspect.BeanPropertyDefinition;
import tools.jackson.databind.jsontype.NamedType;
import tools.jackson.databind.jsontype.TypeDeserializer;
import tools.jackson.databind.jsontype.TypeIdResolver;
import tools.jackson.databind.util.ClassUtil;
import tools.jackson.databind.util.TokenBuffer;

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
    // 03-May-2021, tatu: for [databind#3139], support for "empty" type
    private static final BitSet EMPTY_CLASS_FINGERPRINT = new BitSet(0);
    // Property name -> bitmap-index of every Property discovered, across all subtypes
    private final Map<String, Integer> propertyBitIndex;
    // Bitmap of available properties in each subtype (including its parents)

    private final Map<BitSet, String> subtypeFingerprints;

    public AsDeductionTypeDeserializer(DeserializationContext ctxt,
            JavaType bt, TypeIdResolver idRes, JavaType defaultImpl,
            Collection<NamedType> subtypes)
    {
        super(bt, idRes, null, false, defaultImpl, null, true);
        propertyBitIndex = new HashMap<>();
        subtypeFingerprints = buildFingerprints(ctxt, subtypes);
    }

    public AsDeductionTypeDeserializer(AsDeductionTypeDeserializer src, BeanProperty property)
    {
        super(src, property);
        propertyBitIndex = src.propertyBitIndex;
        subtypeFingerprints = src.subtypeFingerprints;
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
                if (ignoreCase) {
                    name = name.toLowerCase();
                }
                Integer bitIndex = propertyBitIndex.get(name);
                if (bitIndex == null) {
                    bitIndex = nextProperty;
                    propertyBitIndex.put(name, nextProperty++);
                }
                // [databind#4327] 2.17 @JsonAlias should be respected by polymorphic deduction
                for (PropertyName alias : property.findAliases()) {
                    String simpleName = alias.getSimpleName();
                    if (ignoreCase) {
                        simpleName = simpleName.toLowerCase();
                    }
                    // but do not override entries (in case alias overlaps a regular name;
                    // is this even legal?)
                    if (!propertyBitIndex.containsKey(simpleName)) {
                        propertyBitIndex.put(simpleName, bitIndex);
                    }
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
        } else if (/*t == JsonToken.START_ARRAY ||*/ t != JsonToken.PROPERTY_NAME) {
            /* This is most likely due to the fact that not all Java types are
             * serialized as JSON Objects; so if "as-property" inclusion is requested,
             * serialization of things like Lists must be instead handled as if
             * "as-wrapper-array" was requested.
             * But this can also be due to some custom handling: so, if "defaultImpl"
             * is defined, it will be asked to handle this case.
             */
            return _deserializeTypedUsingDefaultImpl(p, ctxt, null, "Unexpected input");
        }

        // 03-May-2021, tatu: [databind#3139] Special case, "empty" Object
        if (t == JsonToken.END_OBJECT) {
            String emptySubtype = subtypeFingerprints.get(EMPTY_CLASS_FINGERPRINT);
            if (emptySubtype != null) { // ... and an "empty" subtype registered
                return _deserializeTypedForId(p, ctxt, null, emptySubtype);
            }
        }

        List<BitSet> candidates = new LinkedList<>(subtypeFingerprints.keySet());

        // Keep track of processed tokens as we must rewind once after deducing
        // the deserializer to use
        final TokenBuffer tb = ctxt.bufferForInputBuffering(p);
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

        // We have zero or multiple candidates, deduction has failed
        String msgToReportIfDefaultImplFailsToo = String.format("Cannot deduce unique subtype of %s (%d candidates match)", ClassUtil.getTypeDescription(_baseType), candidates.size());
        return _deserializeTypedUsingDefaultImpl(p, ctxt, tb, msgToReportIfDefaultImplFailsToo);
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
