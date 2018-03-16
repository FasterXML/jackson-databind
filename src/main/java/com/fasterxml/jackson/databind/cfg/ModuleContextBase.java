package com.fasterxml.jackson.databind.cfg;

import java.util.Collection;
import java.util.function.UnaryOperator;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.Module.SetupContext;
import com.fasterxml.jackson.databind.deser.*;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.SerializerFactory;
import com.fasterxml.jackson.databind.ser.Serializers;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.type.TypeModifier;

public class ModuleContextBase
    implements SetupContext
{
    // // // Immutable objects we need to access information

    protected final MapperBuilder<?,?> _builder;

    // // // Other modifiable state

    protected final ConfigOverrides _configOverrides;

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    public ModuleContextBase(MapperBuilder<?,?> b,
            ConfigOverrides configOverrides)
    {
        _builder = b;
        _configOverrides = configOverrides;
    }

    /**
     * Method called after all changes have been applied through this context, to
     * propagate buffered or pending changes (if any) back to builder. Note that
     * base implementation does nothing here; it is only provded in case sub-classes
     * might need it.
     */
    public void applyChanges(MapperBuilder<?,?> b) {
        // nothing to apply at base class
    }

    /*
    /**********************************************************************
    /* Accessors for metadata
    /**********************************************************************
     */
    
    @Override
    public Version getMapperVersion() {
        return com.fasterxml.jackson.databind.cfg.PackageVersion.VERSION;
    }

    @Override
    public String getFormatName() {
        return _streamFactory().getFormatName();
    }

    /*
    /**********************************************************************
    /* Accessors for subset of handlers
    /**********************************************************************
     */

    @Override
    public Object getOwner() {
        return _builder;
    }

    @Override
    public TypeFactory typeFactory() {
        return _builder.typeFactory();
    }

    @Override
    public TokenStreamFactory tokenStreamFactory() {
        return _streamFactory();
    }

    /*
    /**********************************************************************
    /* Accessors on/off features
    /**********************************************************************
     */
    
    @Override
    public boolean isEnabled(MapperFeature f) {
        return _builder.isEnabled(f);
    }

    @Override
    public boolean isEnabled(DeserializationFeature f) {
        return _builder.isEnabled(f);
    }

    @Override
    public boolean isEnabled(SerializationFeature f) {
        return _builder.isEnabled(f);
    }

    @Override
    public boolean isEnabled(TokenStreamFactory.Feature f) {
        return _streamFactory().isEnabled(f);
    }

    @Override
    public boolean isEnabled(JsonParser.Feature f) {
        return _builder.isEnabled(f);
    }

    @Override
    public boolean isEnabled(JsonGenerator.Feature f) {
        return _builder.isEnabled(f);
    }

    /*
    /**********************************************************************
    /* Mutators for adding deserializers, related
    /**********************************************************************
     */

    @Override
    public SetupContext addDeserializers(Deserializers d) {
        _set(_deserializerFactory().withAdditionalDeserializers(d));
        return this;
    }

    @Override
    public SetupContext addKeyDeserializers(KeyDeserializers kd) {
        _set(_deserializerFactory().withAdditionalKeyDeserializers(kd));
        return this;
    }

    @Override
    public SetupContext addDeserializerModifier(BeanDeserializerModifier modifier) {
        _set(_deserializerFactory().withDeserializerModifier(modifier));
        return this;
    }

    @Override
    public SetupContext addValueInstantiators(ValueInstantiators instantiators) {
        _set(_deserializerFactory().withValueInstantiators(instantiators));
        return this;
    }

    /*
    /**********************************************************************
    /* Mutators for adding serializers, related
    /**********************************************************************
     */

    @Override
    public SetupContext addSerializers(Serializers s) {
        _set(_serializerFactory().withAdditionalSerializers(s));
        return this;
    }

    @Override
    public SetupContext addKeySerializers(Serializers s) {
        _set(_serializerFactory().withAdditionalKeySerializers(s));
        return this;
    }

    @Override
    public SetupContext addSerializerModifier(BeanSerializerModifier modifier) {
        _set(_serializerFactory().withSerializerModifier(modifier));
        return this;
    }

    @Override
    public SetupContext overrideDefaultNullKeySerializer(JsonSerializer<?> ser) {
        _set(_serializerFactory().withNullKeySerializer(ser));
        return this;
    }
    
    @Override
    public SetupContext overrideDefaultNullValueSerializer(JsonSerializer<?> ser) {
        _set(_serializerFactory().withNullValueSerializer(ser));
        return this;
    }

    /*
    /**********************************************************************
    /* Mutators for type handling
    /**********************************************************************
     */

    @Override
    public SetupContext addAbstractTypeResolver(AbstractTypeResolver resolver) {
        _builder.addAbstractTypeResolver(resolver);
        return this;
    }

    @Override
    public SetupContext addTypeModifier(TypeModifier modifier) {
        _builder.addTypeModifier(modifier);
        return this;
    }

    @Override
    public SetupContext registerSubtypes(Class<?>... subtypes) {
        _builder.subtypeResolver().registerSubtypes(subtypes);
        return this;
    }

    @Override
    public SetupContext registerSubtypes(NamedType... subtypes) {
        _builder.subtypeResolver().registerSubtypes(subtypes);
        return this;
    }

    @Override
    public SetupContext registerSubtypes(Collection<Class<?>> subtypes) {
        _builder.subtypeResolver().registerSubtypes(subtypes);
        return this;
    }

    /*
    /**********************************************************************
    /* Mutators for annotation introspection
    /**********************************************************************
     */

    @Override
    public SetupContext insertAnnotationIntrospector(AnnotationIntrospector ai) {
        _builder.baseSettings(_builder.baseSettings()
                .withInsertedAnnotationIntrospector(ai));
        return this;
    }

    @Override
    public SetupContext appendAnnotationIntrospector(AnnotationIntrospector ai) {
        _builder.baseSettings(_builder.baseSettings()
                .withAppendedAnnotationIntrospector(ai));
        return this;
    }

    /*
    /**********************************************************************
    /* Mutators, other
    /**********************************************************************
     */

    @Override
    public MutableConfigOverride configOverride(Class<?> type) {
        return _configOverrides.findOrCreateOverride(type);
    }

    @Override
    public SetupContext addHandler(DeserializationProblemHandler handler)
    {
        _builder.addHandler(handler);
        return this;
    }

    @Override
    public SetupContext overrideInjectableValues(UnaryOperator<InjectableValues> v) {
        InjectableValues oldV = _builder.injectableValues();
        InjectableValues newV = v.apply(oldV);
        if (newV != oldV) {
            _builder.injectableValues(newV);
        }
        return this;
    }
    
    @Override
    public SetupContext setMixIn(Class<?> target, Class<?> mixinSource) {
        _builder.addMixIn(target, mixinSource);
        return this;
    }

    /*
    /**********************************************************************
    /* Internal/sub-class helper methods
    /**********************************************************************
     */

    protected TokenStreamFactory _streamFactory() {
        return _builder.streamFactory();
    }

    protected DeserializerFactory _deserializerFactory() {
        return _builder.deserializerFactory();
    }

    protected void _set(DeserializerFactory f) {
        _builder.deserializerFactory(f);
    }

    protected SerializerFactory _serializerFactory() {
        return _builder.serializerFactory();
    }

    protected void _set(SerializerFactory f) {
        _builder.serializerFactory(f);
    }
}
