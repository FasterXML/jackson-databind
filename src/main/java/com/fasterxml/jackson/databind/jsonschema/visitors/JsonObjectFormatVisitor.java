package com.fasterxml.jackson.databind.jsonschema.visitors;

import java.lang.reflect.Type;

import com.fasterxml.jackson.databind.jsonschema.SchemaAware;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;

public interface JsonObjectFormatVisitor extends JsonFormatVisitor {

	public void property(BeanPropertyWriter writer);

	public void optionalProperty(BeanPropertyWriter writer);

	public void property(String name, SchemaAware handler, Type propertyTypeHint);

	public void optionalProperty(String name, SchemaAware handler,
			Type propertyTypeHint);

	public void property(String name);
	
	public void optionalProperty(String name);

}
