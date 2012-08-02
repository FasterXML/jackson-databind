package com.fasterxml.jackson.databind.jsonschema.visitors;


import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.jsonschema.SchemaAware;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;

public interface JsonObjectFormatVisitor extends JsonFormatVisitor {

	public void property(BeanPropertyWriter writer);

	public void optionalProperty(BeanPropertyWriter writer);

	public void property(String name, SchemaAware handler, JavaType propertyTypeHint);

	public void optionalProperty(String name, SchemaAware handler,
			JavaType propertyTypeHint);

	public void property(String name);
	
	public void optionalProperty(String name);

}
