/**
 * 
 */
package com.fasterxml.jackson.databind.jsonschema.visitors;

import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * @author jphelan
 *
 */
public interface JsonFormatVisitorWithSerializerProvider {

	public SerializerProvider getProvider();
	public abstract void setProvider(SerializerProvider provider);
}
