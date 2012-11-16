package com.fasterxml.jackson.databind.jsonFormatVisitors;

import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;

public interface JsonObjectFormatVisitor extends JsonFormatVisitorWithSerializerProvider
{
    public void property(BeanProperty writer) throws JsonMappingException;
    public void property(String name, JsonFormatVisitable handler, JavaType propertyTypeHint) throws JsonMappingException;

    @Deprecated
    public void property(String name) throws JsonMappingException;

    public void optionalProperty(BeanProperty writer) throws JsonMappingException;
    public void optionalProperty(String name, JsonFormatVisitable handler,
            JavaType propertyTypeHint)
        throws JsonMappingException;

    @Deprecated
    public void optionalProperty(String name) throws JsonMappingException;
}
