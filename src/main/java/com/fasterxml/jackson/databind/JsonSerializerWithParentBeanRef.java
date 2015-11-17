package com.fasterxml.jackson.databind;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;

public abstract class JsonSerializerWithParentBeanRef<T> extends JsonSerializer<T> {

	@Override
	final public void serialize(T value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
		throw new RuntimeException("Not Implemented");
	}

	abstract public void serialize(T value, JsonGenerator jgen, SerializerProvider provider, final Object parentBeanReference) throws IOException, JsonProcessingException;
}