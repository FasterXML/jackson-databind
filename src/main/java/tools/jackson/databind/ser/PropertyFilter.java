package tools.jackson.databind.ser;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.util.Snapshottable;
import tools.jackson.databind.SerializerProvider;
import tools.jackson.databind.jsonFormatVisitors.JsonObjectFormatVisitor;

/**
 * Interface that defines API for filter objects use (as configured
 * using {@link com.fasterxml.jackson.annotation.JsonFilter})
 * for filtering bean properties to serialize.
 *<p>
 * Note that since this is an interface, it is
 * strongly recommended that custom implementations extend
 * {@link tools.jackson.databind.ser.std.SimpleBeanPropertyFilter},
 * to avoid backwards compatibility issues in case interface needs to change.
 */
public interface PropertyFilter
    extends Snapshottable<PropertyFilter>
{
    /**
     * Method called by {@link BeanSerializer} to let the filter decide what to do with
     * given bean property value:
     * the usual choices are to either filter out (i.e.
     * do nothing) or write using given {@link PropertyWriter}, although filters
     * can choose other to do something different altogether.
     *<p>
     * Typical implementation is something like:
     *<pre>
     * if (include(writer)) {
     *      writer.serializeAsProperty(pojo, gen, prov);
     * }
     *</pre>
     * 
     * @param pojo Object that contains property value to serialize
     * @param g Generator use for serializing value
     * @param ctxt Provider that can be used for accessing dynamic aspects of serialization
     *    processing
     * @param writer Object called to do actual serialization of the field, if not filtered out
     */
    public void serializeAsProperty(Object pojo, JsonGenerator g, SerializerProvider ctxt,
            PropertyWriter writer)
        throws Exception;

    /**
     * Method called by container to let the filter decide what to do with given element
     * value: 
     * the usual choices are to either filter out (i.e.
     * do nothing) or write using given {@link PropertyWriter}, although filters
     * can choose other to do something different altogether.
     *<p>
     * Typical implementation is something like:
     *<pre>
     * if (include(writer)) {
     *      writer.serializeAsElement(pojo, gen, prov);
     * }
     *</pre>
     * 
     * @param elementValue Element value being serializerd
     * @param g Generator use for serializing value
     * @param ctxt Provider that can be used for accessing dynamic aspects of serialization
     *    processing
     * @param writer Object called to do actual serialization of the field, if not filtered out
     */
    public void serializeAsElement(Object elementValue, JsonGenerator g, SerializerProvider ctxt,
            PropertyWriter writer)
        throws Exception;

    /**
     * Method called by {@link BeanSerializer} to let the filter determine whether, and in what
     * form the given property exist within the parent, or root, schema. Filters can omit
     * adding the property to the node, or choose the form of the schema value for the property
     *<p>
     * Typical implementation is something like:
     *<pre>
     * if (include(writer)) {
     *      writer.depositSchemaProperty(objectVisitor, provider);
     * }
     *</pre>
     * 
     * @param writer Bean property serializer to use to create schema value
     * @param v JsonObjectFormatVisitor which should be aware of 
     * the property's existence
     * @param ctxt Serialization context
     */
    public void depositSchemaProperty(PropertyWriter writer, JsonObjectFormatVisitor v,
            SerializerProvider ctxt);
}
