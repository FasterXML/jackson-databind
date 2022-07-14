package tools.jackson.databind.ext.jdk8;

import java.util.Optional;

import tools.jackson.databind.*;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.std.ReferenceTypeSerializer;
import tools.jackson.databind.type.ReferenceType;
import tools.jackson.databind.util.NameTransformer;

public class Jdk8OptionalSerializer
    extends ReferenceTypeSerializer<Optional<?>>
{
    /*
    /**********************************************************
    /* Constructors, factory methods
    /**********************************************************
     */

    public Jdk8OptionalSerializer(ReferenceType fullType, boolean staticTyping,
            TypeSerializer vts, ValueSerializer<Object> ser)
    {
        super(fullType, staticTyping, vts, ser);
    }

    protected Jdk8OptionalSerializer(Jdk8OptionalSerializer base, BeanProperty property,
            TypeSerializer vts, ValueSerializer<?> valueSer, NameTransformer unwrapper,
            Object suppressableValue, boolean suppressNulls)
    {
        super(base, property, vts, valueSer, unwrapper,
                suppressableValue, suppressNulls);
    }

    @Override
    protected ReferenceTypeSerializer<Optional<?>> withResolved(BeanProperty prop,
            TypeSerializer vts, ValueSerializer<?> valueSer,
            NameTransformer unwrapper)
    {
        return new Jdk8OptionalSerializer(this, prop, vts, valueSer, unwrapper,
                _suppressableValue, _suppressNulls);
    }

    @Override
    public ReferenceTypeSerializer<Optional<?>> withContentInclusion(Object suppressableValue,
            boolean suppressNulls)
    {
        return new Jdk8OptionalSerializer(this, _property, _valueTypeSerializer,
                _valueSerializer, _unwrapper,
                suppressableValue, suppressNulls);
    }

    /*
    /**********************************************************
    /* Abstract method impls
    /**********************************************************
     */

    @Override
    protected boolean _isValuePresent(Optional<?> value) {
        return value.isPresent();
    }

    @Override
    protected Object _getReferenced(Optional<?> value) {
        return value.get();
    }

    @Override
    protected Object _getReferencedIfPresent(Optional<?> value) {
        return value.orElse(null);
    }
}
