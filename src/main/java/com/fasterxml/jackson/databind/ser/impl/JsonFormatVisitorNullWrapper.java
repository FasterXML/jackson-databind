package com.fasterxml.jackson.databind.ser.impl;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsonFormatVisitors.*;

/**
 * Default "empty" implementation, useful as the base to start on; especially as
 * it is guaranteed to implement all the method of the interface, even if new
 * methods are getting added.
 */
class JsonFormatVisitorNullWrapper implements JsonFormatVisitorWrapper {
	protected SerializerProvider _provider;

	public JsonFormatVisitorNullWrapper() {
	}

	public JsonFormatVisitorNullWrapper(SerializerProvider p) {
		_provider = p;
	}

	@Override
	public SerializerProvider getProvider() {
		return _provider;
	}

	@Override
	public void setProvider(SerializerProvider p) {
		_provider = p;
	}

	@Override
	public JsonObjectFormatVisitor expectObjectFormat(JavaType type)
			throws JsonMappingException {
		return null;
	}

	@Override
	public JsonArrayFormatVisitor expectArrayFormat(JavaType type)
			throws JsonMappingException {
		return null;
	}

	@Override
	public JsonStringFormatVisitor expectStringFormat(JavaType type)
			throws JsonMappingException {
		return null;
	}

	@Override
	public JsonNumberFormatVisitor expectNumberFormat(JavaType type)
			throws JsonMappingException {
		return null;
	}

	@Override
	public JsonIntegerFormatVisitor expectIntegerFormat(JavaType type)
			throws JsonMappingException {
		return null;
	}

	@Override
	public JsonBooleanFormatVisitor expectBooleanFormat(JavaType type)
			throws JsonMappingException {
		return null;
	}

	@Override
	public JsonNullFormatVisitor expectNullFormat(JavaType type)
			throws JsonMappingException {
		return null;
	}

	@Override
	public JsonAnyFormatVisitor expectAnyFormat(JavaType type)
			throws JsonMappingException {
		return null;
	}

	@Override
	public JsonMapFormatVisitor expectMapFormat(JavaType type)
			throws JsonMappingException {
		return null;
	}

};
