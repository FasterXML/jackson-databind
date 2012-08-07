package com.fasterxml.jackson.databind.jsonschema.factories;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsonschema.types.JsonSchema;
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

	public SchemaFactory() {
	}

	public JsonAnyFormatVisitor anyFormat(JavaType convertedType) {
		delegate = new AnySchemaFactory(this);
		return (JsonAnyFormatVisitor) delegate;
	}

	public JsonArrayFormatVisitor arrayFormat(JavaType convertedType) {
		delegate = new ArraySchemaFactory(this);
		return (JsonArrayFormatVisitor) delegate;
	}

	public JsonBooleanFormatVisitor booleanFormat(JavaType convertedType) {
		delegate = new BooleanSchemaFactory(this);
		return (JsonBooleanFormatVisitor) delegate;
	}

	public JsonSchema finalSchema() {
		assert delegate != null : "SchemaFactory must envoke a delegate method before it can return a JsonSchema.";
		if (delegate == null) {
			return null;
		} else {
			return delegate.getSchema();
		}

	}

	/**
	 * {@link SchemaFactory#provider}
	 * @param provider the provider to set
	 */
	public void setProvider(SerializerProvider provider) {
		this.provider = provider;
	}
	
	public SerializerProvider getProvider() {
		return provider;
	}

	public JsonIntegerFormatVisitor integerFormat(JavaType convertedType) {
		delegate = new IntegerSchemaFactory(this);
		return (JsonIntegerFormatVisitor) delegate;
	}

	public JsonNullFormatVisitor nullFormat(JavaType convertedType) {
		delegate = new NullSchemaFactory(this);
		return (JsonNullFormatVisitor) delegate;
	}

	public JsonNumberFormatVisitor numberFormat(JavaType convertedType) {
		delegate = new NumberSchemaFactory(this);
		return (JsonNumberFormatVisitor) delegate;
	}

	public JsonObjectFormatVisitor objectFormat(JavaType convertedType) {
		// BasicClassIntrospector.instance.
		delegate = new ObjectSchemaFactory(this);
		return (JsonObjectFormatVisitor) delegate;
	}

	public JsonStringFormatVisitor stringFormat(JavaType convertedType) {
		delegate = new StringSchemaFactory(this);
		return (JsonStringFormatVisitor) delegate;
	}

}
