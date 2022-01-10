package com.fasterxml.jackson.databind.deser.jackson;

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
                creatorProp("byteOffset", longType, 0, true),
                creatorProp("charOffset", longType, 1, true),
                creatorProp("lineNr", intType, 2, true),
                creatorProp("columnNr", intType, 3, true),
                // 04-Apr-2021, tatu: 2.13 took "sourceRef"; we'll still accept
                //    it, if one given, but will ignore
                creatorProp("sourceRef", config.constructType(Object.class), 4, false),
        };
    }

    private static CreatorProperty creatorProp(String name, JavaType type, int index,
            boolean req)
    {
        return CreatorProperty.construct(PropertyName.construct(name), type, null,
                null, null, null, index, null,
                req ? PropertyMetadata.STD_REQUIRED : PropertyMetadata.STD_OPTIONAL);
    }

    @Override
    public Object createFromObjectWith(DeserializationContext ctxt, Object[] args) {
        // 14-Mar-2021, tatu: Before 2.13 constructor directly took "raw" source ref;
        //   with 2.13 changed to use `InputSourceReference`... left almost as is,
        //   for compatibility.
        final ContentReference srcRef = ContentReference.unknown();
        return new JsonLocation(srcRef, _long(args[0]), _long(args[1]),
                _int(args[2]), _int(args[3]));
    }

    private final static long _long(Object o) {
        return (o == null) ? 0L : ((Number) o).longValue();
    }

    private final static int _int(Object o) {
        return (o == null) ? 0 : ((Number) o).intValue();
    }
}
