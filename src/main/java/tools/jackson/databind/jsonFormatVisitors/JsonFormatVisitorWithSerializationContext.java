/**
 *
 */
package tools.jackson.databind.jsonFormatVisitors;

import tools.jackson.databind.SerializationContext;

/**
 * @author jphelan
 */
public interface JsonFormatVisitorWithSerializationContext {
    public SerializationContext getContext();
    public abstract void setContext(SerializationContext provider);
}
