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

public class ModuleContextBase
    implements Module.SetupContext
{
    // // // Immutable objects we need to access information

    protected final MapperBuilder<?,?> _builder;

    // // // Factories we need to change/modify

    protected DeserializerFactory _deserializerFactory;

    protected SerializerFactory _serializerFactory;

    // // // Other modifiable state

    protected final ConfigOverrides _configOverrides;

    protected BaseSettings _baseSettings;

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    public ModuleContextBase(MapperBuilder<?,?> b,
            ConfigOverrides configOverrides, BaseSettings base)
    {
        _builder = b;

        _configOverrides = configOverrides;
        _baseSettings = base;

        _deserializerFactory = null;
        _serializerFactory = null;
    }

    /**
     * Method called after all changes have been applied through this context, to
     * propagate any buffered or pending changes back (some may have been applied
     * earlier)
     */
    public void applyChanges() {
        if (_deserializerFactory != null) {
            _builder.deserializerFactory(_deserializerFactory);
        }
        if (_serializerFactory != null) {
            _builder.serializerFactory(_serializerFactory);
        }
        // could keep track of changes/no-changes, but for now:
        _builder.baseSettings(_baseSettings);
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
        return streamFactory().getFormatName();
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
        return streamFactory();
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
        return streamFactory().isEnabled(f);
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
        _deserializerFactory = deserializerFactory().withAdditionalDeserializers(d);
        return this;
    }

    @Override
    public Module.SetupContext addKeyDeserializers(KeyDeserializers kd) {
        _deserializerFactory = deserializerFactory().withAdditionalKeyDeserializers(kd);
        return this;
    }

    @Override
    public Module.SetupContext addDeserializerModifier(BeanDeserializerModifier modifier) {
        _deserializerFactory = deserializerFactory().withDeserializerModifier(modifier);
        return this;
    }

    @Override
    public Module.SetupContext addValueInstantiators(ValueInstantiators instantiators) {
        _deserializerFactory = deserializerFactory().withValueInstantiators(instantiators);
        return this;
    }

    /*
    /**********************************************************************
    /* Mutators for adding serializers, related
    /**********************************************************************
     */

    @Override
    public Module.SetupContext addSerializers(Serializers s) {
        _serializerFactory = serializerFactory().withAdditionalSerializers(s);
        return this;
    }

    @Override
    public Module.SetupContext addKeySerializers(Serializers s) {
        _serializerFactory = serializerFactory().withAdditionalKeySerializers(s);
        return this;
    }

    @Override
    public Module.SetupContext addSerializerModifier(BeanSerializerModifier modifier) {
        _serializerFactory = serializerFactory().withSerializerModifier(modifier);
        return this;
    }

    /*
    /**********************************************************************
    /* Mutators for type handling
    /**********************************************************************
     */

    @Override
    public Module.SetupContext addAbstractTypeResolver(AbstractTypeResolver resolver) {
        _deserializerFactory = deserializerFactory().withAbstractTypeResolver(resolver);
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

    /*
    /**********************************************************************
    /* Internal/sub-class helper methods
    /**********************************************************************
     */

    protected TokenStreamFactory streamFactory() {
        return _builder.streamFactory();
    }

    protected DeserializerFactory deserializerFactory() {
        if (_deserializerFactory == null) {
            _deserializerFactory = _builder.deserializerFactory();
        }
        return _deserializerFactory;
    }

    protected SerializerFactory serializerFactory() {
        if (_serializerFactory == null) {
            _serializerFactory = _builder.serializerFactory();
        }
        return _serializerFactory;
    }
}
