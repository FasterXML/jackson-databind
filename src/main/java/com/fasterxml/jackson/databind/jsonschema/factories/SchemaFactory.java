package com.fasterxml.jackson.databind.jsonschema.factories;

import java.lang.reflect.Type;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsonschema.types.Schema;
import com.fasterxml.jackson.databind.jsonschema.visitors.JsonAnyFormatVisitor;
import com.fasterxml.jackson.databind.jsonschema.visitors.JsonArrayFormatVisitor;
import com.fasterxml.jackson.databind.jsonschema.visitors.JsonBooleanFormatVisitor;
import com.fasterxml.jackson.databind.jsonschema.visitors.JsonFormatVisitor;
import com.fasterxml.jackson.databind.jsonschema.visitors.JsonIntegerFormatVisitor;
import com.fasterxml.jackson.databind.jsonschema.visitors.JsonNullFormatVisitor;
import com.fasterxml.jackson.databind.jsonschema.visitors.JsonNumberFormatVisitor;
import com.fasterxml.jackson.databind.jsonschema.visitors.JsonObjectFormatVisitor;
import com.fasterxml.jackson.databind.jsonschema.visitors.JsonStringFormatVisitor;

public class SchemaFactory implements JsonFormatVisitor {

	protected ObjectMapper mapper;
	private SchemaFactoryDelegate delegate;
	
	public SerializerProvider getProvider() {
		return mapper.getSerializerProvider();
	}

	public SchemaFactory(ObjectMapper mapper) {
		this.mapper = mapper;
	}
	
	public JsonObjectFormatVisitor objectFormat(Type type) {
		//BasicClassIntrospector.instance.
		delegate = new ObjectSchemaFactory(this);
		return (JsonObjectFormatVisitor) delegate;
	}

	public JsonArrayFormatVisitor arrayFormat(Type elementType) {
		delegate = new ArraySchemaFactory(this);
		return (JsonArrayFormatVisitor) delegate;
	}
	
	public JsonStringFormatVisitor stringFormat() { 
		delegate = new StringSchemaFactory(this);
		return (JsonStringFormatVisitor) delegate;
	}
	public JsonNumberFormatVisitor numberFormat() { return new NumberSchemaFactory(this); }
	public JsonIntegerFormatVisitor integerFormat() { return new IntegerSchemaFactory(this); }
	public JsonBooleanFormatVisitor booleanFormat() { return new BooleanSchemaFactory(this); }
	public JsonNullFormatVisitor nullFormat() { return new NullSchemaFactory(this); }
	public JsonAnyFormatVisitor anyFormat() { return new AnySchemaFactory(this); }

	public Schema finalSchema() {
		assert delegate != null : "SchemaFactory must envoke a delegate method before it can return a Schema.";
		if (delegate == null) {
			return null;
		} else {
			return delegate.getSchema();
		}
		
	}
	
}
