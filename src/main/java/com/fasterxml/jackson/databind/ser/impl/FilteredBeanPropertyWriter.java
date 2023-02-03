package com.fasterxml.jackson.databind.ser.impl;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonObjectFormatVisitor;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.util.NameTransformer;

/**
 * Decorated {@link BeanPropertyWriter} that will filter out properties
 * that are not to be included in currently active JsonView.
 */
public abstract class FilteredBeanPropertyWriter
{
    public static BeanPropertyWriter constructViewBased(BeanPropertyWriter base, Class<?>[] viewsToIncludeIn)
    {
        if (viewsToIncludeIn.length == 1) {
            return new SingleView(base, viewsToIncludeIn[0]);
        }
        return new MultiView(base, viewsToIncludeIn);
    }

    /*
    /**********************************************************
    /* Concrete sub-classes
    /**********************************************************
     */

    private final static class SingleView
        extends BeanPropertyWriter
        implements java.io.Serializable
    {
        private static final long serialVersionUID = 1L;

        protected final BeanPropertyWriter _delegate;

        protected final Class<?> _view;

        protected SingleView(BeanPropertyWriter delegate, Class<?> view)
        {
            super(delegate);
            _delegate = delegate;
            _view = view;
        }

        @Override
        public SingleView rename(NameTransformer transformer) {
            return new SingleView(_delegate.rename(transformer), _view);
        }

        @Override
        public void assignSerializer(JsonSerializer<Object> ser) {
            _delegate.assignSerializer(ser);
        }

        @Override
        public void assignNullSerializer(JsonSerializer<Object> nullSer) {
            _delegate.assignNullSerializer(nullSer);
        }

        @Override
        public void serializeAsField(Object bean, JsonGenerator gen, SerializerProvider prov)
            throws Exception
        {
            Class<?> activeView = prov.getActiveView();
            if (activeView == null || _view.isAssignableFrom(activeView)) {
                _delegate.serializeAsField(bean, gen, prov);
            } else {
                _delegate.serializeAsOmittedField(bean, gen, prov);
            }
        }

        @Override
        public void serializeAsElement(Object bean, JsonGenerator gen, SerializerProvider prov)
            throws Exception
        {
            Class<?> activeView = prov.getActiveView();
            if (activeView == null || _view.isAssignableFrom(activeView)) {
                _delegate.serializeAsElement(bean, gen, prov);
            } else {
                _delegate.serializeAsPlaceholder(bean, gen, prov);
            }
        }

        @Override
        public void depositSchemaProperty(JsonObjectFormatVisitor v,
                SerializerProvider provider) throws JsonMappingException
        {
            Class<?> activeView = provider.getActiveView();
            if (activeView == null || _view.isAssignableFrom(activeView)) {
                super.depositSchemaProperty(v, provider);
            }
        }
    }

    private final static class MultiView
        extends BeanPropertyWriter
        implements java.io.Serializable
    {
        private static final long serialVersionUID = 1L;

        protected final BeanPropertyWriter _delegate;

        protected final Class<?>[] _views;

        protected MultiView(BeanPropertyWriter delegate, Class<?>[] views) {
            super(delegate);
            _delegate = delegate;
            _views = views;
        }

        @Override
        public MultiView rename(NameTransformer transformer) {
            return new MultiView(_delegate.rename(transformer), _views);
        }

        @Override
        public void assignSerializer(JsonSerializer<Object> ser) {
            _delegate.assignSerializer(ser);
        }

        @Override
        public void assignNullSerializer(JsonSerializer<Object> nullSer) {
            _delegate.assignNullSerializer(nullSer);
        }

        @Override
        public void serializeAsField(Object bean, JsonGenerator gen, SerializerProvider prov)
            throws Exception
        {
            if (_inView(prov.getActiveView())) {
                _delegate.serializeAsField(bean, gen, prov);
                return;
            }
            _delegate.serializeAsOmittedField(bean, gen, prov);
        }

        @Override
        public void serializeAsElement(Object bean, JsonGenerator gen, SerializerProvider prov)
            throws Exception
        {
            if (_inView(prov.getActiveView())) {
                _delegate.serializeAsElement(bean, gen, prov);
                return;
            }
            _delegate.serializeAsPlaceholder(bean, gen, prov);
        }

        @Override
        public void depositSchemaProperty(JsonObjectFormatVisitor v,
                SerializerProvider provider) throws JsonMappingException
        {
            if (_inView(provider.getActiveView())) {
                super.depositSchemaProperty(v, provider);
            }
        }

        private final boolean _inView(Class<?> activeView)
        {
            if (activeView == null) {
                return true;
            }
            final int len = _views.length;
            for (int i = 0; i < len; ++i) {
                if (_views[i].isAssignableFrom(activeView)) {
                    return true;
                }
            }
            return false;
        }
    }
}
