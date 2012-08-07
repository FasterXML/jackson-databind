package com.fasterxml.jackson.databind.jsonschema.factories;


import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsonschema.JsonFormatVisitorAware;
import com.fasterxml.jackson.databind.jsonschema.SchemaFactoryProvider;
import com.fasterxml.jackson.databind.jsonschema.types.JsonSchema;
import com.fasterxml.jackson.databind.jsonschema.types.ObjectSchema;
import com.fasterxml.jackson.databind.jsonschema.types.SchemaType;
import com.fasterxml.jackson.databind.jsonschema.visitors.JsonObjectFormatVisitor;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;

public class ObjectSchemaFactory extends SchemaFactory implements JsonObjectFormatVisitor, SchemaFactoryDelegate {

	protected SchemaFactory parent;
	protected ObjectSchema objectSchema;
	
	public ObjectSchemaFactory(SchemaFactory parent) {
		this.parent = parent;
		setProvider(parent.getProvider());
		objectSchema = new ObjectSchema();
	}
	
	/**
	 * @param provider
	 */
	public ObjectSchemaFactory(SerializerProvider provider) {
		parent = null;
		setProvider(provider);
		objectSchema = new ObjectSchema();
	}

	public JsonSchema getSchema() {
		return objectSchema;
	}

	private JsonSerializer<Object> getSer(BeanPropertyWriter writer) {
		JsonSerializer<Object> ser = writer.getSerializer();
		if (ser == null) {
			Class<?>	serType = writer.getPropertyType();
			try {
				return getProvider().findValueSerializer(serType, writer);
			} catch (JsonMappingException e) {
				// TODO: log error
			}
		}
		return ser;
	}	
	
	protected JsonSchema propertySchema(BeanPropertyWriter writer) {
		SchemaFactoryProvider visitor = new SchemaFactoryProvider();
		visitor.setProvider(provider);
		JsonSerializer<Object> ser = getSer(writer);
		if (ser != null && ser instanceof JsonFormatVisitorAware) {
			((JsonFormatVisitorAware)ser).acceptJsonFormatVisitor(visitor, writer.getType());
		} else {
			visitor.expectAnyFormat(writer.getType());
		}
		return visitor.finalSchema();
	}
	
	protected JsonSchema propertySchema(JsonFormatVisitorAware handler, JavaType propertyTypeHint) {
		SchemaFactoryProvider visitor = new SchemaFactoryProvider();
		visitor.setProvider(provider);
		handler.acceptJsonFormatVisitor(visitor, propertyTypeHint);
		return visitor.finalSchema();
	}
	
	public void property(BeanPropertyWriter writer) {
		objectSchema.putProperty(writer.getName(), propertySchema(writer));
	}

	public void optionalProperty(BeanPropertyWriter writer) {
		objectSchema.putOptionalProperty(writer.getName(), propertySchema(writer));
	}
	
	public void property(String name, JsonFormatVisitorAware handler, JavaType propertyTypeHint) {
		objectSchema.putProperty(name, propertySchema(handler, propertyTypeHint));
	}
	
	public void optionalProperty(String name, JsonFormatVisitorAware handler, JavaType propertyTypeHint) {
		objectSchema.putOptionalProperty(name, propertySchema(handler, propertyTypeHint));
	}
	
	public void property(String name) {
		objectSchema.putProperty(name, JsonSchema.minimalForFormat(SchemaType.ANY));
	}
	
	public void optionalProperty(String name) {
		objectSchema.putOptionalProperty(name, JsonSchema.minimalForFormat(SchemaType.ANY));
	}

}
