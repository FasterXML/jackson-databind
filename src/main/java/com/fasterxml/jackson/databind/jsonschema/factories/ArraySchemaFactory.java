package com.fasterxml.jackson.databind.jsonschema.factories;

import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsonschema.JsonFormatVisitorAware;
import com.fasterxml.jackson.databind.jsonschema.SchemaFactoryProvider;
import com.fasterxml.jackson.databind.jsonschema.types.ArraySchema;
import com.fasterxml.jackson.databind.jsonschema.types.JsonSchema;
import com.fasterxml.jackson.databind.jsonschema.types.SchemaType;
import com.fasterxml.jackson.databind.jsonschema.visitors.JsonArrayFormatVisitor;

public class ArraySchemaFactory extends SchemaFactory implements JsonArrayFormatVisitor, SchemaFactoryDelegate {

	protected SchemaFactory parent; 
	protected ArraySchema arraySchema;
	protected BeanProperty _property;
	
	public ArraySchemaFactory(SchemaFactory parent, BeanProperty property) {
		this.parent = parent;
		setProvider(parent.getProvider());
		arraySchema = new ArraySchema();
	}

	public ArraySchemaFactory(SchemaFactory schemaFactory) {
		this(schemaFactory, null);
	}

	/**
	 * @param provider
	 */
	public ArraySchemaFactory(SerializerProvider provider) {
		parent = null;
		setProvider(provider);
		arraySchema = new ArraySchema();
	}

	public void itemsFormat(JavaType contentType) {
		// An array of object matches any values, thus we leave the schema empty.
        if (contentType.getRawClass() != Object.class) {
        	
            JsonSerializer<Object> ser;
			try {
				ser = getProvider().findValueSerializer(contentType, _property);
				if (ser instanceof JsonFormatVisitorAware) {
	            	SchemaFactoryProvider visitor = new SchemaFactoryProvider();
	            	visitor.setProvider(provider);
	                ((JsonFormatVisitorAware) ser).acceptJsonFormatVisitor(visitor, contentType);
	                arraySchema.setItemsSchema(visitor.finalSchema());
	            }
			} catch (JsonMappingException e) {
				//TODO: log error
			}   
        }
	}
	
	public void itemsFormat(JsonFormatVisitorAware toVisit) {}
	
	public void itemsFormat(SchemaType format) {
		arraySchema.setItemsSchema(JsonSchema.minimalForFormat(format));
	}

	public JsonSchema getSchema() {
		return arraySchema;
	}
	

}
