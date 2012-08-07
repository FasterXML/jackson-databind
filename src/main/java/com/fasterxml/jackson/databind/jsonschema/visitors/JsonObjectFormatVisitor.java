package com.fasterxml.jackson.databind.jsonschema.visitors;


import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.jsonschema.JsonFormatVisitorAware;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;

public interface JsonObjectFormatVisitor extends JsonFormatVisitorWithSerializerProvider {

	public void property(BeanPropertyWriter writer);

	public void optionalProperty(BeanPropertyWriter writer);

	public void property(String name, JsonFormatVisitorAware handler, JavaType propertyTypeHint);

	public void optionalProperty(String name, JsonFormatVisitorAware handler,
			JavaType propertyTypeHint);

	public void property(String name);
	
	public void optionalProperty(String name);

}
