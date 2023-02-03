package tools.jackson.databind.deser.impl;

import java.lang.annotation.Annotation;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.*;
import tools.jackson.databind.deser.NullValueProvider;
import tools.jackson.databind.deser.SettableBeanProperty;
import tools.jackson.databind.deser.UnresolvedForwardReference;
import tools.jackson.databind.deser.ReadableObjectId.Referring;
import tools.jackson.databind.introspect.AnnotatedMember;
import tools.jackson.databind.introspect.ObjectIdInfo;

public class ObjectIdReferenceProperty extends SettableBeanProperty
{
    private static final long serialVersionUID = 1L;

    private final SettableBeanProperty _forward;

    public ObjectIdReferenceProperty(SettableBeanProperty forward, ObjectIdInfo objectIdInfo)
    {
        super(forward);
        _forward = forward;
        _objectIdInfo = objectIdInfo;
    }

    public ObjectIdReferenceProperty(ObjectIdReferenceProperty src, ValueDeserializer<?> deser,
            NullValueProvider nva)
    {
        super(src, deser, nva);
        _forward = src._forward;
        _objectIdInfo = src._objectIdInfo;
    }

    public ObjectIdReferenceProperty(ObjectIdReferenceProperty src, PropertyName newName)
    {
        super(src, newName);
        _forward = src._forward;
        _objectIdInfo = src._objectIdInfo;
    }

    @Override
    public SettableBeanProperty withName(PropertyName newName) {
        return new ObjectIdReferenceProperty(this, newName);
    }

    @Override
    public SettableBeanProperty withValueDeserializer(ValueDeserializer<?> deser) {
        if (_valueDeserializer == deser) {
            return this;
        }
        // 07-May-2019, tatu: As per [databind#2303], must keep VD/NVP in-sync if they were
        NullValueProvider nvp = (_valueDeserializer == _nullProvider) ? deser : _nullProvider;
        return new ObjectIdReferenceProperty(this, deser, nvp);
    }

    @Override
    public SettableBeanProperty withNullProvider(NullValueProvider nva) {
        return new ObjectIdReferenceProperty(this, _valueDeserializer, nva);
    }

    @Override
    public void fixAccess(DeserializationConfig config) {
        if (_forward != null) {
            _forward.fixAccess(config);
        }
    }

    @Override
    public <A extends Annotation> A getAnnotation(Class<A> acls) {
        return _forward.getAnnotation(acls);
    }

    @Override
    public AnnotatedMember getMember() {
        return _forward.getMember();
    }

    @Override
    public int getCreatorIndex() {
        return _forward.getCreatorIndex();
    }

    @Override
    public void deserializeAndSet(JsonParser p, DeserializationContext ctxt, Object instance) throws JacksonException {
        deserializeSetAndReturn(p, ctxt, instance);
    }

    @Override
    public Object deserializeSetAndReturn(JsonParser p, DeserializationContext ctxt,
            Object instance) throws JacksonException
    {
        try {
            return setAndReturn(instance, deserialize(p, ctxt));
        } catch (UnresolvedForwardReference reference) {
            boolean usingIdentityInfo = (_objectIdInfo != null)
                    || (_valueDeserializer.getObjectIdReader(ctxt) != null);
            if (!usingIdentityInfo) {
                throw DatabindException.from(p, "Unresolved forward reference but no identity info", reference);
            }
            reference.getRoid().appendReferring(new PropertyReferring(this, reference, _type.getRawClass(), instance));
            return null;
        }
    }

    @Override
    public void set(Object instance, Object value) throws JacksonException {
        _forward.set(instance, value);
    }

    @Override
    public Object setAndReturn(Object instance, Object value) throws JacksonException {
        return _forward.setAndReturn(instance, value);
    }

    public final static class PropertyReferring extends Referring {
        private final ObjectIdReferenceProperty _parent;
        public final Object _pojo;

        public PropertyReferring(ObjectIdReferenceProperty parent,
                UnresolvedForwardReference ref, Class<?> type, Object ob)
        {
            super(ref, type);
            _parent = parent;
            _pojo = ob;
        }

        @Override
        public void handleResolvedForwardReference(Object id, Object value) throws JacksonException
        {
            if (!hasId(id)) {
                throw new IllegalArgumentException("Trying to resolve a forward reference with id [" + id
                        + "] that wasn't previously seen as unresolved.");
            }
            _parent.set(_pojo, value);
        }
    }
}
