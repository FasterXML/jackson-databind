/**
 *
 */
package tools.jackson.databind.jsonFormatVisitors;

import tools.jackson.databind.SerializationContext;

/**
 * @author jphelan
 */
public interface JsonFormatVisitorWithSerializationContext {
    public SerializationContext getProvider();
    public abstract void setProvider(SerializationContext provider);
}
