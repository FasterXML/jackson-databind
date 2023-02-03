package com.fasterxml.jackson.databind.deser.std;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.io.ContentReference;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.PropertyMetadata;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.deser.CreatorProperty;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.deser.ValueInstantiator;

/**
 * For {@link JsonLocation}, we should be able to just implement
 * {@link ValueInstantiator} (not that explicit one would be very
 * hard but...)
 */
public class JsonLocationInstantiator
    extends ValueInstantiator.Base
{
    private static final long serialVersionUID = 1L;

    public JsonLocationInstantiator() {
        super(JsonLocation.class);
    }

    @Override
    public boolean canCreateFromObjectWith() { return true; }

    @Override
    public SettableBeanProperty[] getFromObjectArguments(DeserializationConfig config) {
        JavaType intType = config.constructType(Integer.TYPE);
        JavaType longType = config.constructType(Long.TYPE);
        return new SettableBeanProperty[] {
                // 14-Mar-2021, tatu: with 2.13 and later, not really used,
                //   but may be produced by older versions so leave as is.
                creatorProp("sourceRef", config.constructType(Object.class), 0),
                creatorProp("byteOffset", longType, 1),
                creatorProp("charOffset", longType, 2),
                creatorProp("lineNr", intType, 3),
                creatorProp("columnNr", intType, 4)
        };
    }

    private static CreatorProperty creatorProp(String name, JavaType type, int index) {
        return CreatorProperty.construct(PropertyName.construct(name), type, null,
                null, null, null, index, null, PropertyMetadata.STD_REQUIRED);
    }

    @Override
    public Object createFromObjectWith(DeserializationContext ctxt, Object[] args) {
        // 14-Mar-2021, tatu: Before 2.13 constructor directly took "raw" source ref;
        //   with 2.13 changed to use `InputSourceReference`... left almost as is,
        //   for compatibility.
        final ContentReference srcRef = ContentReference.rawReference(args[0]);
        return new JsonLocation(srcRef, _long(args[1]), _long(args[2]),
                _int(args[3]), _int(args[4]));
    }

    private final static long _long(Object o) {
        return (o == null) ? 0L : ((Number) o).longValue();
    }

    private final static int _int(Object o) {
        return (o == null) ? 0 : ((Number) o).intValue();
    }
}
