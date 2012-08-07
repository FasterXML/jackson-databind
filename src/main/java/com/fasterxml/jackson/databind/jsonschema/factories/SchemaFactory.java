package com.fasterxml.jackson.databind.jsonschema.factories;

import com.fasterxml.jackson.databind.SerializerProvider;

public abstract class SchemaFactory {

	
	protected SerializerProvider provider;

	public SchemaFactory() {
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

	
}
