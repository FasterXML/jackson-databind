package com.fasterxml.jackson.databind.jsontype.impl;

import java.io.IOException;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.util.JsonParserSequence;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.util.TokenBuffer;

/**
 * A {@link TypeDeserializer} capable of deducing polymorphic types based on the fields available. Deduction
 * is limited to the <i>names</i> of child fields (not their values or, consequently, any nested descendants).
 * Exceptions will be thrown if not enough unique information is present to select a single subtype.
 */
public class AsDeductionTypeDeserializer extends AsArrayTypeDeserializer {

  // Fingerprint-indices of every field discovered across all subtypes
  private final Map<String, Integer> fieldBitIndex;
  // Bitmap of fields in each subtype
  private final Map<BitSet, String> subtypeFingerprints;

  public AsDeductionTypeDeserializer(JavaType bt, TypeIdResolver idRes, JavaType defaultImpl, DeserializationConfig config, Collection<NamedType> subtypes) {
    super(bt, idRes, null, false, defaultImpl);
    fieldBitIndex = new HashMap<>();
    subtypeFingerprints = buildFingerprints(config, subtypes);
  }

  public AsDeductionTypeDeserializer(AsDeductionTypeDeserializer src, BeanProperty property) {
    super(src, property);
    fieldBitIndex = src.fieldBitIndex;
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

  protected Map<BitSet, String> buildFingerprints(DeserializationConfig config, Collection<NamedType> subtypes) {
    int nextField = 0;
    Map<BitSet, String> fingerprints = new HashMap<>();

    for (NamedType subtype : subtypes) {
      JavaType subtyped = config.getTypeFactory().constructType(subtype.getType());
      List<BeanPropertyDefinition> properties = config.introspect(subtyped).findProperties();

      BitSet fingerprint = new BitSet(nextField + properties.size());
      for (BeanPropertyDefinition property : properties) {
        Integer bitIndex = fieldBitIndex.get(property.getName());
        if (bitIndex == null) {
          bitIndex = nextField;
          fieldBitIndex.put(property.getName(), nextField++);
        }
        fingerprint.set(bitIndex);
      }

      // Validate uniqueness
//      if ( fingerprints.containsKey(fingerprint)) {
//        throw InvalidDefinitionException.from(
//          (JsonParser)null,
//          String.format("Subtypes %s and %s have the same signature and cannot be uniquely deduced.", fingerprints.get(fingerprint), subtype.getType().getName()),
//          _baseType
//          );
//      }

      fingerprints.put(fingerprint, subtype.getType().getName());
    }
    return fingerprints;
  }

  @Override
  public Object deserializeTypedFromObject(JsonParser p, DeserializationContext ctxt) throws IOException {

    JsonToken t = p.currentToken();
    if (t == JsonToken.START_OBJECT) {
      t = p.nextToken();
    } else {
      // FIXME
    }

    List<BitSet> candidates = new LinkedList<>(subtypeFingerprints.keySet());

    // Record tokens we process as we'll have to rewind
    // once we have deduced what deserialzer to use
    TokenBuffer tb = new TokenBuffer(p, ctxt);
    boolean ignoreCase = ctxt.isEnabled(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES); // FIXME

    for (; t == JsonToken.FIELD_NAME; t = p.nextToken()) {
      String name = p.getCurrentName();
      tb.copyCurrentStructure(p);

      Integer bit = fieldBitIndex.get(name);
      if (bit != null) {
        // fieldname is known by at least one subtype
        prune(candidates, bit);
        if (candidates.size() == 1) {
          return _deserializeTypedForId(p, ctxt, tb, subtypeFingerprints.get(candidates.get(0)));
        }
      }
    }

    throw new InvalidTypeIdException(
      p,
      String.format("Cannot deduce unique subtype of %s (%d candidates match)", _baseType.toString(), candidates.size()),
      _baseType
      , "DEDUCED"
    );
  }

  // Cut n paste from AsPropertyTypeDeserializer with one improvement to the signature
  protected Object _deserializeTypedForId(JsonParser p, DeserializationContext ctxt,
                                          TokenBuffer tb, String typeId) throws IOException {
    JsonDeserializer<Object> deser = _findDeserializer(ctxt, typeId);
    if (_typeIdVisible) { // need to merge id back in JSON input?
      if (tb == null) {
        tb = new TokenBuffer(p, ctxt);
      }
      tb.writeFieldName(p.getCurrentName());
      tb.writeString(typeId);
    }
    if (tb != null) { // need to put back skipped properties?
      // 02-Jul-2016, tatu: Depending on for JsonParserSequence is initialized it may
      //   try to access current token; ensure there isn't one
      p.clearCurrentToken();
      p = JsonParserSequence.createFlattened(false, tb.asParser(p), p);
    }
    // Must point to the next value; tb had no current, jp pointed to VALUE_STRING:
    p.nextToken(); // to skip past String value
    // deserializer should take care of closing END_OBJECT as well
    return deser.deserialize(p, ctxt);
  }

  private static void prune(List<BitSet> candidates, int bit) {
    for (Iterator<BitSet> iter = candidates.iterator(); iter.hasNext(); ) {
      if (!iter.next().get(bit)) {
        iter.remove();
      }
    }
  }

}
