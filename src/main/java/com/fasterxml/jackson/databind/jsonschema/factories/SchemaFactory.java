package com.fasterxml.jackson.databind.jsonschema.factories;

import com.fasterxml.jackson.databind.JavaType;
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

	private SchemaFactoryDelegate delegate;
	protected SerializerProvider provider;

	public SchemaFactory(SerializerProvider mapper) {
		this.provider = mapper;
	}

	public JsonAnyFormatVisitor anyFormat() {
		delegate = new AnySchemaFactory(this);
		return (JsonAnyFormatVisitor) delegate;
	}

	public JsonArrayFormatVisitor arrayFormat(JavaType elementType) {
		delegate = new ArraySchemaFactory(this);
		return (JsonArrayFormatVisitor) delegate;
	}

	public JsonBooleanFormatVisitor booleanFormat() {
		delegate = new BooleanSchemaFactory(this);
		return (JsonBooleanFormatVisitor) delegate;
	}

	public Schema finalSchema() {
		assert delegate != null : "SchemaFactory must envoke a delegate method before it can return a Schema.";
		if (delegate == null) {
			return null;
		} else {
			return delegate.getSchema();
		}

	}

	public SerializerProvider getProvider() {
		return provider;
	}

	public JsonIntegerFormatVisitor integerFormat() {
		delegate = new IntegerSchemaFactory(this);
		return (JsonIntegerFormatVisitor) delegate;
	}

	public JsonNullFormatVisitor nullFormat() {
		delegate = new NullSchemaFactory(this);
		return (JsonNullFormatVisitor) delegate;
	}

	public JsonNumberFormatVisitor numberFormat() {
		delegate = new NumberSchemaFactory(this);
		return (JsonNumberFormatVisitor) delegate;
	}

	public JsonObjectFormatVisitor objectFormat(JavaType type) {
		// BasicClassIntrospector.instance.
		delegate = new ObjectSchemaFactory(this);
		return (JsonObjectFormatVisitor) delegate;
	}

	public JsonStringFormatVisitor stringFormat() {
		delegate = new StringSchemaFactory(this);
		return (JsonStringFormatVisitor) delegate;
	}

}
