package tools.jackson.databind.ser.std;

import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonFormat;

import tools.jackson.core.JsonGenerator;

import tools.jackson.databind.*;
import tools.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.PropertyWriter;
import tools.jackson.databind.util.NameTransformer;

/**
 * Base class that simplifies implementations of {@link ValueSerializer}s
 * that mostly delegate functionality to another serializer implementation
 * (possibly forming a chaining of serializers delegating functionality
 * in some cases).
 *
 * @since 3.1
 */
public abstract class DelegatingSerializer
    extends StdSerializer<Object>
{
    protected final ValueSerializer<Object> _delegatee;

    /*
    /**********************************************************************
    /* Construction
    /**********************************************************************
     */
    @SuppressWarnings("unchecked")
    public DelegatingSerializer(ValueSerializer<?> delegatee) {
        super((Class<Object>) Objects.requireNonNull(delegatee, "delegatee must not be null").handledType());
        _delegatee = (ValueSerializer<Object>) delegatee;
    }

    /*
    /**********************************************************************
    /* Abstract methods to implement
    /**********************************************************************
     */

    protected abstract ValueSerializer<Object> newDelegatingInstance(ValueSerializer<?> newDelegatee);

    protected final ValueSerializer<Object> _newIfChanged(ValueSerializer<?> newDelegatee) {
        if (newDelegatee == _delegatee) {
            return this;
        }
        return newDelegatingInstance(newDelegatee);
    }
    
    /*
    /**********************************************************************
    /* Overridden "mutant factory" methods
    /**********************************************************************
     */

    @Override
    public ValueSerializer<Object> replaceDelegatee(ValueSerializer<?> delegatee) {
        return _newIfChanged(delegatee);
    }

    @Override
    public ValueSerializer<Object> unwrappingSerializer(NameTransformer unwrapper) {
        return _newIfChanged(_delegatee.unwrappingSerializer(unwrapper));
    }
    
    @Override
    public ValueSerializer<?> withFilterId(Object filterId) {
        return _newIfChanged(_delegatee.withFilterId(filterId));
    }

    @Override
    public ValueSerializer<?> withFormatOverrides(SerializationConfig config,
            JsonFormat.Value formatOverrides) {
        return _newIfChanged(_delegatee.withFormatOverrides(config, formatOverrides));
    }
    
    @Override
    public ValueSerializer<?> withIgnoredProperties(Set<String> ignoredProperties) {
        return _newIfChanged(_delegatee.withIgnoredProperties(ignoredProperties));
    }

    /*
    /**********************************************************************
    /* Overridden methods for contextualization, resolving
    /**********************************************************************
     */

    @Override
    public void resolve(SerializationContext ctxt) {
        _delegatee.resolve(ctxt);
    }

    @Override
    public ValueSerializer<?> createContextual(SerializationContext ctxt, BeanProperty property)
    {
        return _newIfChanged(ctxt.handleSecondaryContextualization(_delegatee, property));
    }

    /*
    /**********************************************************************
    /* Overridden serialization methods
    /**********************************************************************
     */

    @Override
    public boolean isEmpty(SerializationContext ctxt, Object value) {
        return _delegatee.isEmpty(ctxt, value);
    }

    @Override
    public void serialize(Object value, JsonGenerator gen, SerializationContext ctxt) {
        _delegatee.serialize(value, gen, ctxt);
    }

    @Override
    public void serializeWithType(Object value, JsonGenerator gen, SerializationContext ctxt,
            TypeSerializer typeSer) {
        _delegatee.serializeWithType(value, gen, ctxt, typeSer);
    }

    /*
    /**********************************************************************
    /* Overridden accessors
    /**********************************************************************
     */
    
    @Override
    public Class<?> handledType() { return _delegatee.handledType(); }

    @Override
    public boolean usesObjectId() { return _delegatee.usesObjectId(); }

    @Override
    public boolean isUnwrappingSerializer() { return _delegatee.isUnwrappingSerializer(); }

    @Override
    public ValueSerializer<?> getDelegatee() { return _delegatee; }

    @Override
    public Iterator<PropertyWriter> properties() { return _delegatee.properties(); }

    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType type) {
        _delegatee.acceptJsonFormatVisitor(visitor, type);
    }
}
