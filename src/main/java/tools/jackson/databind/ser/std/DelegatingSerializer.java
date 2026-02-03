package tools.jackson.databind.ser.std;

import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

import tools.jackson.core.JacksonException;
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
        ValueSerializer<?> del = ctxt.handleSecondaryContextualization(_delegatee,
                property);
        if (del == _delegatee) {
            return this;
        }
        return newDelegatingInstance(del);
    }

    @Override
    public ValueSerializer<Object> unwrappingSerializer(NameTransformer unwrapper) {
        ValueSerializer<?> unwrapping = _delegatee.unwrappingSerializer(unwrapper);
        if (unwrapping == _delegatee) {
            return this;
        }
        return newDelegatingInstance(unwrapping);
    }

    @Override
    public ValueSerializer<Object> replaceDelegatee(ValueSerializer<?> delegatee)
    {
        if (delegatee == _delegatee) {
            return this;
        }
        return newDelegatingInstance(delegatee);
    }

    /*
    /**********************************************************************
    /* Overridden serialization methods
    /**********************************************************************
     */

    @Override
    public void serialize(Object value, JsonGenerator gen, SerializationContext provider) {
        _delegatee.serialize(value, gen, provider);
    }

    @Override
    public void serializeWithType(Object value, JsonGenerator gen, SerializationContext ctxt,
            TypeSerializer typeSer) {
        _delegatee.serializeWithType(value, gen, ctxt, typeSer);
    }

    /*
    /**********************************************************************
    /* Overridden other methods
    /**********************************************************************
     */

    @Override
    public ValueSerializer<?> withFilterId(Object filterId) {
        return _delegatee.withFilterId(filterId);
    }

    @Override
    public ValueSerializer<?> withIgnoredProperties(Set<String> ignoredProperties) {
        return _delegatee.withIgnoredProperties(ignoredProperties);
    }

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
    public boolean isEmpty(SerializationContext ctxt, Object value) { return _delegatee.isEmpty(ctxt, value); }

    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType type) {
        _delegatee.acceptJsonFormatVisitor(visitor, type);
    }
}
