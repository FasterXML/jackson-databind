package com.fasterxml.jackson.databind.ser.impl;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
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
    {
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
        public void serializeAsField(Object bean, JsonGenerator jgen, SerializerProvider prov)
            throws Exception
        {
            Class<?> activeView = prov.getActiveView();
            if (activeView == null || _view.isAssignableFrom(activeView)) {
                _delegate.serializeAsField(bean, jgen, prov);
            } else {
                _delegate.serializeAsOmittedField(bean, jgen, prov);
            }
        }

        @Override
        public void serializeAsElement(Object bean, JsonGenerator jgen, SerializerProvider prov)
            throws Exception
        {
            Class<?> activeView = prov.getActiveView();
            if (activeView == null || _view.isAssignableFrom(activeView)) {
                _delegate.serializeAsElement(bean, jgen, prov);
            } else {
                _delegate.serializeAsPlaceholder(bean, jgen, prov);
            }
        }
    }

    private final static class MultiView
        extends BeanPropertyWriter
    {
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
        public void serializeAsField(Object bean, JsonGenerator jgen, SerializerProvider prov)
            throws Exception
        {
            final Class<?> activeView = prov.getActiveView();
            if (activeView != null) {
                int i = 0, len = _views.length;
                for (; i < len; ++i) {
                    if (_views[i].isAssignableFrom(activeView)) break;
                }
                // not included, bail out:
                if (i == len) {
                    _delegate.serializeAsOmittedField(bean, jgen, prov);
                    return;
                }
            }
            _delegate.serializeAsField(bean, jgen, prov);
        }

        @Override
        public void serializeAsElement(Object bean, JsonGenerator jgen, SerializerProvider prov)
            throws Exception
        {
            final Class<?> activeView = prov.getActiveView();
            if (activeView != null) {
                int i = 0, len = _views.length;
                for (; i < len; ++i) {
                    if (_views[i].isAssignableFrom(activeView)) break;
                }
                // not included, bail out:
                if (i == len) {
                    _delegate.serializeAsPlaceholder(bean, jgen, prov);
                    return;
                }
            }
            _delegate.serializeAsElement(bean, jgen, prov);
        }
    }
}
