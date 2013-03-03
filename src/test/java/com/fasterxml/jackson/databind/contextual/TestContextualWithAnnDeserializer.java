package com.fasterxml.jackson.databind.contextual;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.fasterxml.jackson.annotation.JacksonAnnotation;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;

public class TestContextualWithAnnDeserializer extends BaseMapTest
{
    @Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @JacksonAnnotation
    public @interface Name {
        public String value();
    }
    
    static class StringValue {
        protected String value;
        
        public StringValue(String v) { value = v; }
    }

    static class AnnotatedContextualClassBean
    {
        @Name("xyz")
        @JsonDeserialize(using=AnnotatedContextualDeserializer.class)
        public StringValue value;
    }
    
    static class AnnotatedContextualDeserializer
        extends JsonDeserializer<StringValue>
        implements ContextualDeserializer
    {
        protected final String _fieldName;
        
        public AnnotatedContextualDeserializer() { this(""); }
        public AnnotatedContextualDeserializer(String fieldName) {
            _fieldName = fieldName;
        }

        @Override
        public StringValue deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException
        {
            return new StringValue(""+_fieldName+"="+jp.getText());
        }

        @Override
        public JsonDeserializer<?> createContextual(DeserializationContext ctxt,
                BeanProperty property)
            throws JsonMappingException
        {
            Name ann = property.getAnnotation(Name.class);
            if (ann == null) {
                ann = property.getContextAnnotation(Name.class);
            }
            String propertyName = (ann == null) ?  "UNKNOWN" : ann.value();
            return new MyContextualDeserializer(propertyName);
        }
    }

    static class MyContextualDeserializer
        extends JsonDeserializer<StringValue>
        implements ContextualDeserializer
    {
        protected final String _fieldName;
        
        public MyContextualDeserializer() { this(""); }
        public MyContextualDeserializer(String fieldName) {
            _fieldName = fieldName;
        }
    
        @Override
        public StringValue deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException
        {
            return new StringValue(""+_fieldName+"="+jp.getText());
        }

        @Override
        public JsonDeserializer<?> createContextual(DeserializationContext ctxt,
                BeanProperty property)
            throws JsonMappingException
        {
            String name = (property == null) ? "NULL" : property.getName();
            return new MyContextualDeserializer(name);
        }
    }

    
    // ensure that direct associations also work
    public void testAnnotatedContextual() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        AnnotatedContextualClassBean bean = mapper.readValue(
                "{\"value\":\"a\"}",
              AnnotatedContextualClassBean.class);
        assertNotNull(bean);
        assertEquals("xyz=a", bean.value.value);
    }

}
