package com.fasterxml.jackson.databind.jsonschema.factories;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonAnyFormatVisitor;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonArrayFormatVisitor;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonBooleanFormatVisitor;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonIntegerFormatVisitor;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonNullFormatVisitor;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonNumberFormatVisitor;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonObjectFormatVisitor;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonStringFormatVisitor;
import com.fasterxml.jackson.databind.jsonschema.factories.*;
import com.fasterxml.jackson.databind.jsonschema.types.JsonSchema;

/**
 * @author jphelan
 *
 */
public class SchemaFactoryProvider implements JsonFormatVisitorWrapper{

	protected SerializerProvider provider;
	private SchemaFactory delegate;
	
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
	
	
	public JsonAnyFormatVisitor expectAnyFormat(JavaType convertedType) {
		delegate = new AnySchemaFactory(provider);
		return (JsonAnyFormatVisitor) delegate;
	}

	public JsonArrayFormatVisitor expectArrayFormat(JavaType convertedType) {
		delegate = new ArraySchemaFactory(provider);
		return (JsonArrayFormatVisitor) delegate;
	}

	public JsonBooleanFormatVisitor expectBooleanFormat(JavaType convertedType) {
		delegate = new BooleanSchemaFactory(provider);
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
	
	public JsonIntegerFormatVisitor expectIntegerFormat(JavaType convertedType) {
		delegate = new IntegerSchemaFactory(provider);
		return (JsonIntegerFormatVisitor) delegate;
	}

	public JsonNullFormatVisitor expectNullFormat(JavaType convertedType) {
		delegate = new NullSchemaFactory(provider);
		return (JsonNullFormatVisitor) delegate;
	}

	public JsonNumberFormatVisitor expectNumberFormat(JavaType convertedType) {
		delegate = new NumberSchemaFactory(provider);
		return (JsonNumberFormatVisitor) delegate;
	}

	public JsonObjectFormatVisitor expectObjectFormat(JavaType convertedType) {
		delegate = new ObjectSchemaFactory(provider);
		return (JsonObjectFormatVisitor) delegate;
	}

	public JsonStringFormatVisitor expectStringFormat(JavaType convertedType) {
		delegate = new StringSchemaFactory(provider);
		return (JsonStringFormatVisitor) delegate;
	}

}
