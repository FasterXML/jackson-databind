/**
 *
 */
package com.fasterxml.jackson.databind.jsonFormatVisitors;

import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * @author jphelan
 */
public interface JsonFormatVisitorWithSerializerProvider {
    public SerializerProvider getProvider();
    public abstract void setProvider(SerializerProvider provider);
}
