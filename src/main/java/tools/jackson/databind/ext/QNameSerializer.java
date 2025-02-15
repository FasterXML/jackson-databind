package tools.jackson.databind.ext;

import javax.xml.namespace.QName;

import com.fasterxml.jackson.annotation.JsonFormat;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonToken;
import tools.jackson.core.type.WritableTypeId;
import tools.jackson.databind.*;
import tools.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.std.StdSerializer;
import tools.jackson.databind.ser.std.ToStringSerializer;

public class QNameSerializer
    extends StdSerializer<QName>
{
    public final static ValueSerializer<?> instance = new QNameSerializer();

    public QNameSerializer() {
        super(QName.class);
    }
    
    @Override
    public ValueSerializer<?> createContextual(SerializationContext serializers, BeanProperty property)
    {
        JsonFormat.Value format = findFormatOverrides(serializers, property, handledType());
        if (format != null) {
            JsonFormat.Shape shape = format.getShape();
            if (shape == JsonFormat.Shape.OBJECT) {
                return this;
            }
        }
        return ToStringSerializer.instance;
    }
    
    @Override
    public void serialize(QName value, JsonGenerator g, SerializationContext ctxt)
    {
        g.writeStartObject(value);
        serializeProperties(value, g, ctxt);
        g.writeEndObject();
    }
    
    @Override
    public final void serializeWithType(QName value, JsonGenerator g, SerializationContext ctxt,
            TypeSerializer typeSer)
    {
        WritableTypeId typeIdDef = typeSer.writeTypePrefix(g,
                ctxt, typeSer.typeId(value, JsonToken.START_OBJECT));
        serializeProperties(value, g, ctxt);
        typeSer.writeTypeSuffix(g, ctxt, typeIdDef);
    }
    
    private void serializeProperties(QName value, JsonGenerator g, SerializationContext ctxt)
    {
        g.writeStringProperty("localPart", value.getLocalPart());
        if (!value.getNamespaceURI().isEmpty()) {
            g.writeStringProperty("namespaceURI", value.getNamespaceURI());
        }
        if (!value.getPrefix().isEmpty()) {
            g.writeStringProperty("prefix", value.getPrefix());
        }
    }
    
    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
    {
        /*JsonObjectFormatVisitor v =*/ visitor.expectObjectFormat(typeHint);
        // TODO: would need to visit properties too, see `BeanSerializerBase`
    }
}
