package com.fasterxml.jackson.databind.jsonschema.factories;


import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.jsonschema.SchemaAware;
import com.fasterxml.jackson.databind.jsonschema.types.ObjectSchema;
import com.fasterxml.jackson.databind.jsonschema.types.Schema;
import com.fasterxml.jackson.databind.jsonschema.types.SchemaType;
import com.fasterxml.jackson.databind.jsonschema.visitors.JsonObjectFormatVisitor;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;

public class ObjectSchemaFactory extends SchemaFactory implements JsonObjectFormatVisitor, SchemaFactoryDelegate {

	protected SchemaFactory parent;
	protected ObjectSchema objectSchema;
	
	public ObjectSchemaFactory(SchemaFactory parent) {
		super(parent.provider);
		this.parent = parent;
		objectSchema = new ObjectSchema();
	}
	
	public Schema getSchema() {
		// TODO Auto-generated method stub
		return objectSchema;
	}

	private JsonSerializer<Object> getSer(BeanPropertyWriter writer, Class<?> serType) {
		JsonSerializer<Object> ser = writer.getSerializer();
		if (ser == null) { // nope
			if (serType == null) {
				serType = writer.getPropertyType();
			}
			try {
				return getProvider().findValueSerializer(serType, writer);
			} catch (JsonMappingException e) {
				// TODO: log error
			}
		}
		return ser;
	}	
	
	private Class<?> writerType(BeanPropertyWriter writer) {
		
		//TODO:Will these ever return different types?
		
		//JavaType propType = writer.getSerializationType();
		//Type hint = (propType == null) ? writer.getGenericPropertyType() : propType.getRawClass();
		return writer.getPropertyType();
	}
	
	protected Schema propertySchema(BeanPropertyWriter writer) {
		SchemaFactory visitor = new SchemaFactory(provider);
		Class<?> serType = writerType(writer);
		JsonSerializer<Object> ser = getSer(writer, serType);
		if (ser != null && ser instanceof SchemaAware) {
			((SchemaAware)ser).acceptJsonFormatVisitor(visitor, writer.getType());
		} else {
			visitor.anyFormat();
		}
		return visitor.finalSchema();
	}
	
	protected Schema propertySchema(SchemaAware handler, JavaType propertyTypeHint) {
		SchemaFactory visitor = new SchemaFactory(provider);
		handler.acceptJsonFormatVisitor(visitor, propertyTypeHint);
		return visitor.finalSchema();
	}
	
	public void property(BeanPropertyWriter writer) {
		objectSchema.putProperty(writer.getName(), propertySchema(writer));
	}

	public void optionalProperty(BeanPropertyWriter writer) {
		objectSchema.putOptionalProperty(writer.getName(), propertySchema(writer));
	}
	
	public void property(String name, SchemaAware handler, JavaType propertyTypeHint) {
		objectSchema.putProperty(name, propertySchema(handler, propertyTypeHint));
	}
	
	public void optionalProperty(String name, SchemaAware handler, JavaType propertyTypeHint) {
		objectSchema.putOptionalProperty(name, propertySchema(handler, propertyTypeHint));
	}
	
	public void property(String name) {
		objectSchema.putProperty(name, Schema.minimalForFormat(SchemaType.ANY));
	}
	
	public void optionalProperty(String name) {
		objectSchema.putOptionalProperty(name, Schema.minimalForFormat(SchemaType.ANY));
	}

}
