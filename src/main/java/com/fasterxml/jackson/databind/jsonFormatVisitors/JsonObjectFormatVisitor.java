package com.fasterxml.jackson.databind.jsonFormatVisitors;


import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JavaType;

public interface JsonObjectFormatVisitor extends JsonFormatVisitorWithSerializerProvider {

	public void property(BeanProperty writer);

	public void optionalProperty(BeanProperty writer);

	public void property(String name, JsonFormatVisitable handler, JavaType propertyTypeHint);

	public void optionalProperty(String name, JsonFormatVisitable handler,
			JavaType propertyTypeHint);

	public void property(String name);
	
	public void optionalProperty(String name);

}
