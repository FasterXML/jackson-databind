package com.fasterxml.jackson.databind.cfg;

import java.util.Collection;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.*;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.SerializerFactory;
import com.fasterxml.jackson.databind.ser.Serializers;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.type.TypeModifier;

public abstract class ModuleContextBase<M extends ObjectMapper>
    implements Module.SetupContext
{
    // // // Immutable objects we need to access information

    protected final MapperBuilder<M,?> _builder;

    protected final TokenStreamFactory _streamFactory;

    // // // Factories we need to change/modify

    protected DeserializerFactory _deserializerFactory;

    protected SerializerFactory _serializerFactory;

    // // // Other modifiable state

    protected final ConfigOverrides _configOverrides;

    protected BaseSettings _baseSettings;

    protected ModuleContextBase(MapperBuilder<M,?> b,
            ConfigOverrides configOverrides)
    {
        _builder = b;

        _streamFactory = b.streamFactory();
        _deserializerFactory = b.deserializerFactory();
        _serializerFactory = b.serializerFactory();

        _configOverrides = configOverrides;
        _baseSettings = b.baseSettings();
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
        return _streamFactory.getFormatName();
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
        return _streamFactory;
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
        return _streamFactory.isEnabled(f);
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
    public Module.SetupContext addDeserializers(Deserializers d) {
        _deserializerFactory = _deserializerFactory.withAdditionalDeserializers(d);
        return this;
    }

    @Override
    public Module.SetupContext addKeyDeserializers(KeyDeserializers kd) {
        _deserializerFactory = _deserializerFactory.withAdditionalKeyDeserializers(kd);
        return this;
    }

    @Override
    public Module.SetupContext addDeserializerModifier(BeanDeserializerModifier modifier) {
        _deserializerFactory = _deserializerFactory.withDeserializerModifier(modifier);
        return this;
    }

    @Override
    public Module.SetupContext addValueInstantiators(ValueInstantiators instantiators) {
        _deserializerFactory = _deserializerFactory.withValueInstantiators(instantiators);
        return this;
    }

    /*
    /**********************************************************************
    /* Mutators for adding serializers, related
    /**********************************************************************
     */

    @Override
    public Module.SetupContext addSerializers(Serializers s) {
        _serializerFactory = _serializerFactory.withAdditionalSerializers(s);
        return this;
    }

    @Override
    public Module.SetupContext addKeySerializers(Serializers s) {
        _serializerFactory = _serializerFactory.withAdditionalKeySerializers(s);
        return this;
    }

    @Override
    public Module.SetupContext addSerializerModifier(BeanSerializerModifier modifier) {
        _serializerFactory = _serializerFactory.withSerializerModifier(modifier);
        return this;
    }

    /*
    /**********************************************************************
    /* Mutators for type handling
    /**********************************************************************
     */

    @Override
    public Module.SetupContext addAbstractTypeResolver(AbstractTypeResolver resolver) {
        _deserializerFactory = _deserializerFactory.withAbstractTypeResolver(resolver);
        return this;
    }

    @Override
    public Module.SetupContext addTypeModifier(TypeModifier modifier) {
        _builder.addTypeModifier(modifier);
        return this;
    }

    @Override
    public Module.SetupContext registerSubtypes(Class<?>... subtypes) {
        _builder.subtypeResolver().registerSubtypes(subtypes);
        return this;
    }

    @Override
    public Module.SetupContext registerSubtypes(NamedType... subtypes) {
        _builder.subtypeResolver().registerSubtypes(subtypes);
        return this;
    }

    @Override
    public Module.SetupContext registerSubtypes(Collection<Class<?>> subtypes) {
        _builder.subtypeResolver().registerSubtypes(subtypes);
        return this;
    }

    /*
    /**********************************************************************
    /* Mutators for annotation introspection
    /**********************************************************************
     */

    @Override
    public Module.SetupContext insertAnnotationIntrospector(AnnotationIntrospector ai) {
        _baseSettings = _baseSettings.withInsertedAnnotationIntrospector(ai);
        return this;
    }

    @Override
    public Module.SetupContext appendAnnotationIntrospector(AnnotationIntrospector ai) {
        _baseSettings = _baseSettings.withAppendedAnnotationIntrospector(ai);
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
    public Module.SetupContext addHandler(DeserializationProblemHandler handler)
    {
        _builder.addHandler(handler);
        return this;
    }

    @Override
    public Module.SetupContext setMixIn(Class<?> target, Class<?> mixinSource) {
        _builder.addMixIn(target, mixinSource);
        return this;
    }
}
